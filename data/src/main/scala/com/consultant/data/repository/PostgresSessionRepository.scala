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
