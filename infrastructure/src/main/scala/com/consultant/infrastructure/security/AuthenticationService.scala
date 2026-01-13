package com.consultant.infrastructure.security

import cats.effect.IO
import cats.syntax.all.*
import com.consultant.core.domain.security.*
import com.consultant.core.domain.User
import com.consultant.core.domain.{ CreateUserRequest, User }
import com.consultant.core.domain.types.*
import com.consultant.core.ports.{
  CredentialsRepository,
  RefreshTokenRepository,
  SecurityAuditRepository,
  UserRepository
}
import java.util.UUID
import java.time.Instant
import scala.concurrent.duration.*

object AuthenticationService:
  case class RegistrationRequest(
    email: String,
    password: String,
    name: String,
    phone: Option[String],
    role: UserRole
  )

  case class LoginRequest(
    email: String,
    password: String,
    ipAddress: String,
    userAgent: String
  )

  case class LoginResponse(
    accessToken: String,
    refreshToken: String,
    expiresAt: Instant,
    user: User
  )

/** Сервис аутентификации и авторизации */
class AuthenticationService(
  userRepository: UserRepository,
  credentialsRepository: CredentialsRepository,
  refreshTokenRepository: RefreshTokenRepository,
  auditRepository: SecurityAuditRepository,
  passwordService: PasswordHashingService,
  jwtService: JwtTokenService
):

  private val maxFailedAttempts = 5
  private val lockDuration      = 15.minutes

  /** Регистрация нового пользователя */
  def register(request: AuthenticationService.RegistrationRequest): IO[Either[String, User]] =
    (for
      // Проверяем сложность пароля
      _ <- passwordService.validatePasswordStrength(request.password).flatMap {
        case Left(error) => IO.raiseError(new RuntimeException(error))
        case Right(_)    => IO.unit
      }

      // Проверяем, не существует ли уже пользователь
      existing <- userRepository.findByEmail(request.email)
      _ <- existing match
        case Some(_) => IO.raiseError(new RuntimeException("User already exists"))
        case None    => IO.unit

      // Создаем пользователя
      userId = UUID.randomUUID()
      createRequest: CreateUserRequest = CreateUserRequest(
        email = request.email,
        name = request.name,
        phone = request.phone,
        role = request.role
      )
      createdUser <- userRepository.create(createRequest)

      // Хешируем пароль
      salt <- passwordService.generateSalt()
      hash <- passwordService.hashPassword(request.password, salt)

      // Сохраняем credentials
      credentials = Credentials(
        email = request.email,
        passwordHash = hash,
        salt = salt,
        userId = userId,
        role = request.role
      )
      _ <- credentialsRepository.create(credentials)
    yield createdUser).attempt.map {
      case Right(user) => Right(user)
      case Left(error) => Left(error.getMessage)
    }

  /** Аутентификация пользователя */
  def login(request: AuthenticationService.LoginRequest): IO[Either[String, AuthenticationService.LoginResponse]] =
    (for
      // Получаем credentials
      credentials <- credentialsRepository.findByEmail(request.email).flatMap {
        case Some(creds) => IO.pure(creds)
        case None        => IO.raiseError(new RuntimeException("Invalid credentials"))
      }

      // Проверяем, не заблокирован ли аккаунт
      _ <-
        if credentials.isLocked then
          auditLog(
            credentials.userId,
            "LOGIN_BLOCKED",
            request.ipAddress,
            request.userAgent,
            success = false,
            Some("Account locked")
          )
            .flatMap(_ => IO.raiseError(new RuntimeException("Account is locked. Try again later.")))
        else IO.unit

      // Проверяем активность аккаунта
      _ <-
        if !credentials.isActive then IO.raiseError(new RuntimeException("Account is inactive"))
        else IO.unit

      // Верифицируем пароль
      validPassword <- passwordService.verifyPassword(request.password, credentials.passwordHash, credentials.salt)

      _ <-
        if !validPassword then
          // Неверный пароль - увеличиваем счетчик
          credentialsRepository.incrementFailedAttempts(request.email) *>
            (if credentials.failedLoginAttempts + 1 >= maxFailedAttempts then
               credentialsRepository.lockAccount(request.email, Instant.now().plusSeconds(lockDuration.toSeconds))
             else IO.unit) *>
            auditLog(
              credentials.userId,
              "LOGIN_FAILED",
              request.ipAddress,
              request.userAgent,
              success = false,
              Some("Invalid password")
            )
              .flatMap(_ => IO.raiseError(new RuntimeException("Invalid credentials")))
        else IO.unit

      // Успешная аутентификация
      _ <- credentialsRepository.resetFailedAttempts(request.email)

      // Получаем пользователя
      user <- userRepository.findById(credentials.userId).flatMap {
        case Some(u) => IO.pure(u)
        case None    => IO.raiseError(new RuntimeException("User not found"))
      }

      // Генерируем токены
      accessToken  <- jwtService.generateAccessToken(credentials.userId, credentials.role, request.email)
      refreshToken <- jwtService.generateRefreshToken(credentials.userId)

      // Сохраняем refresh token
      _ <- refreshTokenRepository.create(refreshToken)

      // Обновляем время последнего логина
      updatedCreds = credentials.copy(lastLogin = Some(Instant.now()))
      _ <- credentialsRepository.update(updatedCreds)

      // Логируем успешный вход
      _ <- auditLog(credentials.userId, "LOGIN_SUCCESS", request.ipAddress, request.userAgent, success = true, None)

      response = AuthenticationService.LoginResponse(
        accessToken = accessToken.token,
        refreshToken = refreshToken.token,
        expiresAt = accessToken.expiresAt,
        user = user
      )
    yield response).attempt.map {
      case Right(response) => Right(response)
      case Left(error)     => Left(error.getMessage)
    }

  /** Обновление access token через refresh token */
  def refreshAccessToken(refreshTokenStr: String): IO[Either[String, AuthenticationService.LoginResponse]] =
    (for
      // Находим refresh token
      refreshToken <- refreshTokenRepository.findByToken(refreshTokenStr).flatMap {
        case Some(token) => IO.pure(token)
        case None        => IO.raiseError(new RuntimeException("Invalid refresh token"))
      }

      // Проверяем expiration
      _ <-
        if refreshToken.isExpired then
          refreshTokenRepository.delete(refreshTokenStr) *>
            IO.raiseError(new RuntimeException("Refresh token expired"))
        else IO.unit

      // Получаем credentials и пользователя
      credentials <- credentialsRepository.findByUserId(refreshToken.userId).flatMap {
        case Some(c) => IO.pure(c)
        case None    => IO.raiseError(new RuntimeException("User not found"))
      }

      user <- userRepository.findById(refreshToken.userId).flatMap {
        case Some(u) => IO.pure(u)
        case None    => IO.raiseError(new RuntimeException("User not found"))
      }

      // Генерируем новый access token
      accessToken <- jwtService.generateAccessToken(credentials.userId, credentials.role, credentials.email)

      response = AuthenticationService.LoginResponse(
        accessToken = accessToken.token,
        refreshToken = refreshTokenStr, // Используем тот же refresh token
        expiresAt = accessToken.expiresAt,
        user = user
      )
    yield response).attempt.map {
      case Right(response) => Right(response)
      case Left(error)     => Left(error.getMessage)
    }

  /** Logout - инвалидация токенов */
  def logout(refreshTokenStr: String, userId: UUID, ipAddress: String, userAgent: String): IO[Boolean] =
    refreshTokenRepository.delete(refreshTokenStr) *>
      auditLog(userId, "LOGOUT", ipAddress, userAgent, success = true, None)
        .as(true)

  /** Logout со всех устройств */
  def logoutAll(userId: UUID, ipAddress: String, userAgent: String): IO[Int] =
    refreshTokenRepository.deleteByUserId(userId) <*
      auditLog(userId, "LOGOUT_ALL", ipAddress, userAgent, success = true, None)

  /** Валидация токена */
  def validateToken(token: String): IO[Either[String, AuthToken]] =
    jwtService.validateToken(token)

  /** Проверка прав доступа */
  def hasPermission(role: UserRole, permission: Permission): Boolean =
    (role, permission) match
      case (UserRole.Admin, _) => true // Админ имеет все права
      case (
            UserRole.Specialist,
            Permission.ReadSpecialist | Permission.WriteSpecialist | Permission.ManageConsultations
          ) =>
        true
      case (UserRole.Client, Permission.ReadUser | Permission.ManageConsultations) => true
      case _                                                                       => false

  /** Аудит лог */
  private def auditLog(
    userId: UUID,
    action: String,
    ipAddress: String,
    userAgent: String,
    success: Boolean,
    details: Option[String]
  ): IO[SecurityAuditLog] =
    val log = SecurityAuditLog(
      id = UUID.randomUUID(),
      userId = userId,
      action = action,
      ipAddress = ipAddress,
      userAgent = userAgent,
      success = success,
      details = details
    )
    auditRepository.log(log)
