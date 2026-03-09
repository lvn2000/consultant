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
package com.consultant.core.service

import cats.effect.IO
import cats.syntax.all.*
import com.consultant.core.domain.*
import com.consultant.core.ports.*
import com.consultant.core.validation.UserValidator
import com.consultant.core.validation.ValidationResult.*
import java.util.UUID
import java.time.Instant
import org.mindrot.jbcrypt.BCrypt
import scala.concurrent.duration.*

class UserService(
  userRepo: UserRepository,
  sessionRepo: SessionRepository,
  notificationPreferenceRepo: Option[NotificationPreferenceRepository] = None
) {

  /**
   * Delete a user and cascade delete related data (sessions, notification preferences, refresh tokens, etc). Returns
   * Left(DomainError) on error, Right(()) on success. Cannot delete the last admin user.
   */
  def deleteUser(
    userId: UserId,
    refreshTokenRepo: Option[RefreshTokenRepository] = None,
    auditRepo: Option[SecurityAuditRepository] = None
  ): IO[Either[DomainError, Unit]] =
    for
      // Check if user exists and is admin
      userOpt <- userRepo.findById(userId)
      result <- userOpt match
        case None       => IO.pure(Left(DomainError.UserNotFound(userId)))
        case Some(user) =>
          // Check if this is an admin user
          if user.role == UserRole.Admin then
            // Count total admins
            userRepo.countAdmins().flatMap { adminCount =>
              if adminCount <= 1 then
                // Cannot delete the last admin
                IO.pure(
                  Left(DomainError.Forbidden("Cannot delete the last admin user. At least one admin must exist."))
                )
              else
                // Safe to delete this admin
                performDeleteUser(userId, refreshTokenRepo, auditRepo)
            }
          else
            // Not an admin, safe to delete
            performDeleteUser(userId, refreshTokenRepo, auditRepo)
    yield result

  private def performDeleteUser(
    userId: UserId,
    refreshTokenRepo: Option[RefreshTokenRepository],
    auditRepo: Option[SecurityAuditRepository]
  ): IO[Either[DomainError, Unit]] = {
    // Cascade delete: notification preferences, refresh tokens, sessions, then user
    val deletePrefs = notificationPreferenceRepo match {
      case Some(repo) => repo.deleteByUser(userId)
      case None       => IO.unit
    }
    val deleteRefreshTokens = refreshTokenRepo match {
      case Some(repo) => repo.deleteByUserId(userId).void
      case None       => IO.unit
    }
    // Optionally: delete audit logs if needed (not implemented)
    // val deleteAudit = auditRepo match {
    //   case Some(repo) => repo.deleteByUser(userId)
    //   case None => IO.unit
    // }
    // TODO: delete user sessions if needed (not implemented)
    (for {
      _ <- deletePrefs
      _ <- deleteRefreshTokens
      _ <- userRepo.delete(userId)
    } yield ()).attempt.map {
      case Right(_) => Right(())
      case Left(e)  => Left(parseError(e))
    }
  }

  /** Parses database errors into structured domain errors */
  private def parseError(error: Throwable): DomainError =
    import com.consultant.core.error.PostgresErrorParser
    PostgresErrorParser.parseError(error)

  /** Get the count of admin users in the system */
  def getAdminCount(): IO[Int] = userRepo.countAdmins()

  case class LoginResult(user: User, session: UserSession)

  private val sessionTtl = 24.hours

  def createUser(request: CreateUserRequest): IO[Either[DomainError, User]] =
    UserValidator.validateCreate(request).toEither match
      case Left(error) => IO.pure(Left(error))
      case Right(_) =>
        for
          existing <- userRepo.findByEmail(request.email)
          result <- existing match
            case Some(_) => IO.pure(Left(DomainError.EmailAlreadyExists(request.email)))
            case None =>
              for
                user <- userRepo.create(request)
                // Create default notification preferences if repo is available
                _ <- notificationPreferenceRepo match
                  case Some(repo) => repo.createDefaults(user.id)
                  case None       => IO.unit
              yield Right(user)
        yield result

  def getUser(id: UserId): IO[Either[DomainError, User]] =
    userRepo.findById(id).map {
      case Some(user) => Right(user)
      case None       => Left(DomainError.UserNotFound(id))
    }

  def updateUser(user: User): IO[Either[DomainError, User]] =
    UserValidator.validateUpdate(user).toEither match
      case Left(error) => IO.pure(Left(error))
      case Right(_)    => userRepo.update(user).map(Right(_))

  def listUsers(offset: Int, limit: Int): IO[List[User]] =
    userRepo.list(offset, limit)

  def login(
    login: String,
    password: String,
    ipAddress: String,
    userAgent: String
  ): IO[Either[DomainError, LoginResult]] =
    userRepo.login(login, password).flatMap {
      case Some(user) =>
        val now       = Instant.now()
        val sessionId = UUID.randomUUID().toString
        val session = UserSession(
          sessionId = sessionId,
          userId = user.id,
          role = user.role,
          ipAddress = ipAddress,
          userAgent = userAgent,
          createdAt = now,
          lastActivity = now,
          expiresAt = now.plusSeconds(sessionTtl.toSeconds)
        )

        sessionRepo.create(session).map(created => Right(LoginResult(user, created)))
      case None =>
        IO.pure(Left(DomainError.InvalidCredentials))
    }

  def logout(sessionId: String): IO[Boolean] =
    sessionRepo.delete(sessionId)

  // Email validation is now handled by UserValidator
}
