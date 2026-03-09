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
import com.consultant.core.domain.security.RefreshToken
import com.consultant.core.ports.RefreshTokenRepository
import java.util.UUID
import java.time.Instant

/** PostgreSQL implementation of RefreshTokenRepository */
class PostgresRefreshTokenRepository(xa: Transactor[IO]) extends RefreshTokenRepository:

  override def create(token: RefreshToken): IO[RefreshToken] =
    sql"""
      INSERT INTO refresh_tokens (token, user_id, expires_at, created_at)
      VALUES (${token.token}, ${token.userId.toString}::uuid, ${token.expiresAt}, ${token.createdAt})
    """.update.run
      .transact(xa)
      .as(token)

  override def findByToken(token: String): IO[Option[RefreshToken]] =
    sql"""
      SELECT token, user_id, expires_at, created_at
      FROM refresh_tokens
      WHERE token = $token
    """
      .query[(String, UUID, Instant, Instant)]
      .option
      .map(_.map { case (tk, userId, expiresAt, createdAt) =>
        RefreshToken(tk, userId, expiresAt, createdAt)
      })
      .transact(xa)

  override def findByUserId(userId: UUID): IO[List[RefreshToken]] =
    sql"""
      SELECT token, user_id, expires_at, created_at
      FROM refresh_tokens
      WHERE user_id = ${userId.toString}::uuid
    """
      .query[(String, UUID, Instant, Instant)]
      .to[List]
      .map(_.map { case (tk, uid, expiresAt, createdAt) =>
        RefreshToken(tk, uid, expiresAt, createdAt)
      })
      .transact(xa)

  override def delete(token: String): IO[Boolean] =
    sql"""
      DELETE FROM refresh_tokens
      WHERE token = $token
    """.update.run
      .transact(xa)
      .map(_ > 0)

  override def deleteByUserId(userId: UUID): IO[Int] =
    sql"""
      DELETE FROM refresh_tokens
      WHERE user_id = ${userId.toString}::uuid
    """.update.run
      .transact(xa)
