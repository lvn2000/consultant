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
