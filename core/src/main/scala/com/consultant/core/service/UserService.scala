package com.consultant.core.service

import cats.effect.IO
import cats.syntax.all.*
import com.consultant.core.domain.*
import com.consultant.core.ports.*
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
   * Left(DomainError) on error, Right(()) on success.
   */
  def deleteUser(
    userId: UserId,
    refreshTokenRepo: Option[RefreshTokenRepository] = None,
    auditRepo: Option[SecurityAuditRepository] = None
  ): IO[Either[DomainError, Unit]] =
    userRepo.findById(userId).flatMap {
      case None    => IO.pure(Left(DomainError.UserNotFound(userId)))
      case Some(_) =>
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
          case Left(e)  => Left(DomainError.DatabaseError(e.getMessage))
        }
    }

  case class LoginResult(user: User, session: UserSession)

  private val sessionTtl = 24.hours

  def createUser(request: CreateUserRequest): IO[Either[DomainError, User]] =
    for
      existing <- userRepo.findByEmail(request.email)
      result <- existing match
        case Some(_) => IO.pure(Left(DomainError.EmailAlreadyExists(request.email)))
        case None =>
          if !isValidEmail(request.email) then IO.pure(Left(DomainError.InvalidEmail(request.email)))
          else
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
    userRepo.update(user).map(Right(_))

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

  private def isValidEmail(email: String): Boolean =
    email.matches("""^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$""")
}
