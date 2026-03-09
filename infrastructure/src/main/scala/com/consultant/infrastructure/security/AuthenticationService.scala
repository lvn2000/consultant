/*
 * Copyright (c) 2026 Volodymyr Lubenchenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.consultant.infrastructure.security

import cats.effect.IO
import cats.syntax.all.*
import com.consultant.core.domain.{ CreateSpecialistRequest, CreateUserRequest, User }
import com.consultant.core.domain.security.*
import com.consultant.core.domain.types.*
import com.consultant.core.ports.{
  CredentialsRepository,
  RefreshTokenRepository,
  SecurityAuditRepository,
  SpecialistRepository,
  UserRepository
}
import com.consultant.infrastructure.config.AppConfig
import java.util.UUID
import java.time.Instant
import org.mindrot.jbcrypt.BCrypt
import scala.concurrent.duration.*

object AuthenticationService:
  case class RegistrationRequest(
    login: String,
    email: String,
    password: String,
    name: String,
    phone: Option[String],
    role: UserRole
  )

  /** Allowed roles for public (unauthenticated) registration */
  val publicAllowedRoles: Set[UserRole] = Set(UserRole.Client, UserRole.Specialist)

  /** Allowed roles when an admin creates a new account */
  val adminAllowedRoles: Set[UserRole] = Set(UserRole.Client, UserRole.Specialist, UserRole.Admin)

  case class LoginRequest(
    login: String,
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
  config: AppConfig,
  userRepository: UserRepository,
  specialistRepository: SpecialistRepository,
  credentialsRepository: CredentialsRepository,
  refreshTokenRepository: RefreshTokenRepository,
  auditRepository: SecurityAuditRepository,
  passwordService: PasswordHashingService,
  jwtService: JwtTokenService
):

  private val maxFailedAttempts = config.security.maxFailedLoginAttempts
  private val lockDuration      = config.security.accountLockDuration

  private def verifyPassword(password: String, hash: String, salt: String): IO[Boolean] =
    if hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$") then
      IO.blocking(BCrypt.checkpw(password, hash)).handleError(_ => false)
    else passwordService.verifyPassword(password, hash, salt)

  /** Register new user (public self-registration: Client or Specialist only) */
  def register(request: AuthenticationService.RegistrationRequest): IO[Either[String, User]] =
    registerWithRoleCheck(request, AuthenticationService.publicAllowedRoles)

  /** Register new user by admin (any role allowed) */
  def registerByAdmin(request: AuthenticationService.RegistrationRequest): IO[Either[String, User]] =
    registerWithRoleCheck(request, AuthenticationService.adminAllowedRoles)

  /** Internal registration with configurable role whitelist */
  private def registerWithRoleCheck(
    request: AuthenticationService.RegistrationRequest,
    allowedRoles: Set[UserRole]
  ): IO[Either[String, User]] =
    (for
      // Validate role
      _ <-
        if !allowedRoles.contains(request.role) then
          IO.raiseError(new RuntimeException(s"Role '${request.role}' is not allowed for this registration type"))
        else IO.unit

      // Check password complexity
      _ <- passwordService.validatePasswordStrength(request.password).flatMap {
        case Left(error) => IO.raiseError(new RuntimeException(error))
        case Right(_)    => IO.unit
      }

      // Check if email already exists
      existingByEmail <- userRepository.findByEmail(request.email)
      _ <- existingByEmail match
        case Some(_) => IO.raiseError(new RuntimeException("User with this email already exists"))
        case None    => IO.unit

      // Check if login already exists
      existingByLogin <- userRepository.findByLogin(request.login)
      _ <- existingByLogin match
        case Some(_) => IO.raiseError(new RuntimeException("User with this login already exists"))
        case None    => IO.unit

      // Create user
      createRequest: CreateUserRequest = CreateUserRequest(
        login = request.login,
        email = request.email,
        name = request.name,
        phone = request.phone,
        role = request.role,
        countryId = None,
        languages = Set.empty
      )
      createdUser <- userRepository.create(createRequest)

      // Auto-create specialist profile for specialist role so user/specialist IDs stay aligned.
      _ <-
        if request.role == UserRole.Specialist then
          specialistRepository.findByEmail(request.email).flatMap {
            case Some(_) => IO.unit
            case None =>
              specialistRepository
                .create(
                  CreateSpecialistRequest(
                    email = request.email,
                    name = request.name,
                    phone = request.phone.getOrElse(""),
                    bio = "",
                    categoryRates = List.empty,
                    isAvailable = true,
                    countryId = None,
                    languages = Set.empty,
                    id = Some(createdUser.id)
                  )
                )
                .void
          }
        else IO.unit

      // Hash password
      salt <- passwordService.generateSalt()
      hash <- passwordService.hashPassword(request.password, salt)

      // Save credentials
      credentials = Credentials(
        email = request.email,
        passwordHash = hash,
        salt = salt,
        userId = createdUser.id,
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
      credentials <- credentialsRepository.findByLogin(request.login).flatMap {
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
      validPassword <- verifyPassword(request.password, credentials.passwordHash, credentials.salt)

      _ <-
        if !validPassword then
          // Incorrect password - increment counter
          credentialsRepository.incrementFailedAttempts(credentials.email) *>
            (if credentials.failedLoginAttempts + 1 >= maxFailedAttempts then
               credentialsRepository.lockAccount(credentials.email, Instant.now().plusSeconds(lockDuration.toSeconds))
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
      _ <- credentialsRepository.resetFailedAttempts(credentials.email)

      // Get user
      user <- userRepository.findById(credentials.userId).flatMap {
        case Some(u) => IO.pure(u)
        case None    => IO.raiseError(new RuntimeException("User not found"))
      }

      // Generate tokens
      accessToken  <- jwtService.generateAccessToken(credentials.userId, credentials.role, credentials.email)
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
