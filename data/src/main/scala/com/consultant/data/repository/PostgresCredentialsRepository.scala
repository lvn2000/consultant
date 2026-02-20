package com.consultant.data.repository

import cats.effect.IO
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import com.consultant.core.domain.security.*
import com.consultant.core.ports.CredentialsRepository
import java.util.UUID
import java.time.Instant

/** PostgreSQL implementation of CredentialsRepository */
class PostgresCredentialsRepository(xa: Transactor[IO]) extends CredentialsRepository:

  given userRoleMeta: Meta[UserRole] =
    Meta[String].timap(str => UserRole.valueOf(str))(_.toString)

  private def readCredentials(
    email: String,
    passwordHash: String,
    salt: String,
    userId: UUID,
    role: UserRole,
    isActive: Boolean,
    lastLogin: Option[Instant],
    failedLoginAttempts: Int,
    lockedUntil: Option[Instant]
  ): Credentials =
    Credentials(email, passwordHash, salt, userId, role, isActive, lastLogin, failedLoginAttempts, lockedUntil)

  // Custom Read instance to avoid tuple-size limitations
  private given Read[Credentials] =
    Read[(String, String, String, UUID, UserRole, Boolean, Option[Instant], Int, Option[Instant])].map {
      case (email, passwordHash, salt, userId, role, isActive, lastLogin, failedAttempts, lockedUntil) =>
        Credentials(email, passwordHash, salt, userId, role, isActive, lastLogin, failedAttempts, lockedUntil)
    }

  override def findByEmail(email: String): IO[Option[Credentials]] =
    sql"""
      SELECT email, password_hash, salt, user_id, role, is_active,
             last_login, failed_login_attempts, locked_until
      FROM credentials
      WHERE email = $email
    """
      .query[Credentials]
      .option
      .transact(xa)

  override def findByLogin(login: String): IO[Option[Credentials]] =
    sql"""
      SELECT c.email, c.password_hash, c.salt, c.user_id, c.role, c.is_active,
             c.last_login, c.failed_login_attempts, c.locked_until
      FROM credentials c
      JOIN users u ON c.user_id = u.id
      WHERE LOWER(u.login) = LOWER($login) OR LOWER(c.email) = LOWER($login)
    """
      .query[Credentials]
      .option
      .transact(xa)

  override def findByUserId(userId: UUID): IO[Option[Credentials]] =
    sql"""
      SELECT email, password_hash, salt, user_id, role, is_active,
             last_login, failed_login_attempts, locked_until
      FROM credentials
      WHERE user_id = $userId
    """
      .query[Credentials]
      .option
      .transact(xa)

  override def create(credentials: Credentials): IO[Credentials] =
    sql"""
      INSERT INTO credentials (email, password_hash, salt, user_id, role, is_active,
                               last_login, failed_login_attempts, locked_until)
      VALUES (${credentials.email}, ${credentials.passwordHash}, ${credentials.salt},
              ${credentials.userId.toString}::uuid, ${credentials.role.toString}, ${credentials.isActive},
              ${credentials.lastLogin}, ${credentials.failedLoginAttempts}, ${credentials.lockedUntil})
    """.update.run
      .transact(xa)
      .as(credentials)

  override def update(credentials: Credentials): IO[Option[Credentials]] =
    sql"""
      UPDATE credentials
      SET password_hash = ${credentials.passwordHash},
          salt = ${credentials.salt},
          role = ${credentials.role.toString},
          is_active = ${credentials.isActive},
          last_login = ${credentials.lastLogin},
          failed_login_attempts = ${credentials.failedLoginAttempts},
          locked_until = ${credentials.lockedUntil}
      WHERE email = ${credentials.email}
    """.update.run
      .transact(xa)
      .map(rows => if rows > 0 then Some(credentials) else None)

  override def incrementFailedAttempts(email: String): IO[Unit] =
    sql"""
      UPDATE credentials
      SET failed_login_attempts = failed_login_attempts + 1
      WHERE email = $email
    """.update.run
      .transact(xa)
      .void

  override def resetFailedAttempts(email: String): IO[Unit] =
    sql"""
      UPDATE credentials
      SET failed_login_attempts = 0, locked_until = NULL
      WHERE email = $email
    """.update.run
      .transact(xa)
      .void

  override def lockAccount(email: String, until: Instant): IO[Unit] =
    sql"""
      UPDATE credentials
      SET locked_until = $until
      WHERE email = $email
    """.update.run
      .transact(xa)
      .void
