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

/** Authentication and authorization service */
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

  /** Register new user */
  def register(request: AuthenticationService.RegistrationRequest): IO[Either[String, User]] =
    (for
      // Check password complexity
      _ <- passwordService.validatePasswordStrength(request.password).flatMap {
        case Left(error) => IO.raiseError(new RuntimeException(error))
        case Right(_)    => IO.unit
      }

      // Check if user already exists
      existing <- userRepository.findByEmail(request.email)
      _ <- existing match
        case Some(_) => IO.raiseError(new RuntimeException("User already exists"))
        case None    => IO.unit

      // Create user
      userId = UUID.randomUUID()
      createRequest: CreateUserRequest = CreateUserRequest(
        login = request.email,
        email = request.email,
        name = request.name,
        phone = request.phone,
        role = request.role,
        countryId = None,
        languages = Set.empty
      )
      createdUser <- userRepository.create(createRequest)

      // Hash password
      salt <- passwordService.generateSalt()
      hash <- passwordService.hashPassword(request.password, salt)

      // Save credentials
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

  /** User authentication */
  def login(request: AuthenticationService.LoginRequest): IO[Either[String, AuthenticationService.LoginResponse]] =
    (for
      // Get credentials
      credentials <- credentialsRepository.findByEmail(request.email).flatMap {
        case Some(creds) => IO.pure(creds)
        case None        => IO.raiseError(new RuntimeException("Invalid credentials"))
      }

      // Check if account is locked
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

      // Check account activity
      _ <-
        if !credentials.isActive then IO.raiseError(new RuntimeException("Account is inactive"))
        else IO.unit

      // Verify password
      validPassword <- passwordService.verifyPassword(request.password, credentials.passwordHash, credentials.salt)

      _ <-
        if !validPassword then
          // Incorrect password - increment counter
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

      // Successful authentication
      _ <- credentialsRepository.resetFailedAttempts(request.email)

      // Get user
      user <- userRepository.findById(credentials.userId).flatMap {
        case Some(u) => IO.pure(u)
        case None    => IO.raiseError(new RuntimeException("User not found"))
      }

      // Generate tokens
      accessToken  <- jwtService.generateAccessToken(credentials.userId, credentials.role, request.email)
      refreshToken <- jwtService.generateRefreshToken(credentials.userId)

      // Save refresh token
      _ <- refreshTokenRepository.create(refreshToken)

      // Update last login time
      updatedCreds = credentials.copy(lastLogin = Some(Instant.now()))
      _ <- credentialsRepository.update(updatedCreds)

      // Log successful login
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

  /** Refresh access token via refresh token */
  def refreshAccessToken(refreshTokenStr: String): IO[Either[String, AuthenticationService.LoginResponse]] =
    (for
      // Find refresh token
      refreshToken <- refreshTokenRepository.findByToken(refreshTokenStr).flatMap {
        case Some(token) => IO.pure(token)
        case None        => IO.raiseError(new RuntimeException("Invalid refresh token"))
      }

      // Check expiration
      _ <-
        if refreshToken.isExpired then
          refreshTokenRepository.delete(refreshTokenStr) *>
            IO.raiseError(new RuntimeException("Refresh token expired"))
        else IO.unit

      // Get credentials and user
      credentials <- credentialsRepository.findByUserId(refreshToken.userId).flatMap {
        case Some(c) => IO.pure(c)
        case None    => IO.raiseError(new RuntimeException("User not found"))
      }

      user <- userRepository.findById(refreshToken.userId).flatMap {
        case Some(u) => IO.pure(u)
        case None    => IO.raiseError(new RuntimeException("User not found"))
      }

      // Generate new access token
      accessToken <- jwtService.generateAccessToken(credentials.userId, credentials.role, credentials.email)

      response = AuthenticationService.LoginResponse(
        accessToken = accessToken.token,
        refreshToken = refreshTokenStr, // Reuse the same refresh token
        expiresAt = accessToken.expiresAt,
        user = user
      )
    yield response).attempt.map {
      case Right(response) => Right(response)
      case Left(error)     => Left(error.getMessage)
    }

  /** Logout - invalidate tokens */
  def logout(refreshTokenStr: String, userId: UUID, ipAddress: String, userAgent: String): IO[Boolean] =
    refreshTokenRepository.delete(refreshTokenStr) *>
      auditLog(userId, "LOGOUT", ipAddress, userAgent, success = true, None)
        .as(true)

  /** Logout from all devices */
  def logoutAll(userId: UUID, ipAddress: String, userAgent: String): IO[Int] =
    refreshTokenRepository.deleteByUserId(userId) <*
      auditLog(userId, "LOGOUT_ALL", ipAddress, userAgent, success = true, None)

  /** Token validation */
  def validateToken(token: String): IO[Either[String, AuthToken]] =
    jwtService.validateToken(token)

  /** Check access permissions */
  def hasPermission(role: UserRole, permission: Permission): Boolean =
    (role, permission) match
      case (UserRole.Admin, _) => true // Admin has all permissions
      case (
            UserRole.Specialist,
            Permission.ReadSpecialist | Permission.WriteSpecialist | Permission.ManageConsultations
          ) =>
        true
      case (UserRole.Client, Permission.ReadUser | Permission.ManageConsultations) => true
      case _                                                                       => false

  /** Audit log */
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
