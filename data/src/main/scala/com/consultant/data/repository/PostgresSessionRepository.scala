package com.consultant.data.repository

import cats.effect.IO
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import com.consultant.core.domain.security.{ UserRole, UserSession }
import com.consultant.core.ports.SessionRepository
import java.util.UUID
import java.time.Instant

class PostgresSessionRepository(xa: Transactor[IO]) extends SessionRepository:

  given userRoleMeta: Meta[UserRole] =
    Meta[String].timap(str => UserRole.valueOf(str))(_.toString)

  override def create(session: UserSession): IO[UserSession] =
    sql"""
      INSERT INTO user_sessions (
        session_id, user_id, role, ip_address, user_agent, created_at, last_activity, expires_at
      ) VALUES (
        ${session.sessionId}, ${session.userId}, ${session.role.toString},
        ${session.ipAddress}, ${session.userAgent}, ${session.createdAt},
        ${session.lastActivity}, ${session.expiresAt}
      )
    """.update.run.transact(xa).as(session)

  override def findById(sessionId: String): IO[Option[UserSession]] =
    sql"""
      SELECT session_id, user_id, role, ip_address, user_agent, created_at, last_activity, expires_at
      FROM user_sessions
      WHERE session_id = $sessionId
    """.query[UserSession].option.transact(xa)

  override def findByUserId(userId: UUID): IO[List[UserSession]] =
    sql"""
      SELECT session_id, user_id, role, ip_address, user_agent, created_at, last_activity, expires_at
      FROM user_sessions
      WHERE user_id = $userId
      ORDER BY created_at DESC
    """.query[UserSession].to[List].transact(xa)

  override def update(session: UserSession): IO[Option[UserSession]] =
    sql"""
      UPDATE user_sessions
      SET last_activity = ${session.lastActivity},
          expires_at = ${session.expiresAt}
      WHERE session_id = ${session.sessionId}
    """.update.run.transact(xa).map {
      case 0 => None
      case _ => Some(session)
    }

  override def delete(sessionId: String): IO[Boolean] =
    sql"""
      DELETE FROM user_sessions
      WHERE session_id = $sessionId
    """.update.run.transact(xa).map(_ > 0)

  override def deleteExpired(): IO[Int] =
    val now = Instant.now()
    sql"""
      DELETE FROM user_sessions
      WHERE expires_at < $now
    """.update.run.transact(xa)
