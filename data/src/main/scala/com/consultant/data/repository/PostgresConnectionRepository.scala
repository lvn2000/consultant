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
import cats.syntax.all.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import com.consultant.core.domain.*
import com.consultant.core.ports.ConnectionRepository
import java.util.UUID
import java.time.Instant

class PostgresConnectionRepository(xa: Transactor[IO]) extends ConnectionRepository:

  override def create(specialistId: SpecialistId, request: CreateConnectionRequest): IO[SpecialistConnection] =
    val id  = UUID.randomUUID()
    val now = Instant.now()
    val connection = SpecialistConnection(
      id,
      specialistId,
      request.connectionTypeId,
      request.connectionValue,
      false, // is_verified
      now,
      now
    )

    sql"""
      INSERT INTO specialist_connections (
        id, specialist_id, connection_type_id, connection_value, is_verified, created_at, updated_at
      )
      VALUES (
        ${connection.id}, ${connection.specialistId}, ${connection.connectionTypeId},
        ${connection.connectionValue}, ${connection.isVerified}, ${connection.createdAt}, ${connection.updatedAt}
      )
    """.update.run.transact(xa).as(connection)

  override def findById(id: UUID): IO[Option[SpecialistConnection]] =
    sql"""
      SELECT id, specialist_id, connection_type_id, connection_value, is_verified, created_at, updated_at
      FROM specialist_connections
      WHERE id = $id
    """.query[SpecialistConnection].option.transact(xa)

  override def findBySpecialist(specialistId: SpecialistId): IO[List[SpecialistConnection]] =
    sql"""
      SELECT id, specialist_id, connection_type_id, connection_value, is_verified, created_at, updated_at
      FROM specialist_connections
      WHERE specialist_id = $specialistId
      ORDER BY created_at DESC
    """.query[SpecialistConnection].to[List].transact(xa)

  override def findBySpecialistAndType(
    specialistId: SpecialistId,
    connectionTypeId: ConnectionTypeId
  ): IO[Option[SpecialistConnection]] =
    sql"""
      SELECT id, specialist_id, connection_type_id, connection_value, is_verified, created_at, updated_at
      FROM specialist_connections
      WHERE specialist_id = $specialistId AND connection_type_id = $connectionTypeId
    """.query[SpecialistConnection].option.transact(xa)

  override def update(connection: SpecialistConnection): IO[SpecialistConnection] =
    val updated = connection.copy(updatedAt = Instant.now())
    sql"""
      UPDATE specialist_connections
      SET connection_value = ${updated.connectionValue},
          is_verified = ${updated.isVerified},
          updated_at = ${updated.updatedAt}
      WHERE id = ${updated.id}
    """.update.run.transact(xa).as(updated)

  override def delete(id: UUID): IO[Unit] =
    sql"DELETE FROM specialist_connections WHERE id = $id".update.run.transact(xa).void

  override def deleteBySpecialist(specialistId: SpecialistId): IO[Unit] =
    sql"DELETE FROM specialist_connections WHERE specialist_id = $specialistId".update.run.transact(xa).void

  // Client connections implementation
  override def createClientConnection(userId: UserId, request: CreateConnectionRequest): IO[ClientConnection] =
    val id  = UUID.randomUUID()
    val now = Instant.now()
    val connection = ClientConnection(
      id,
      userId,
      request.connectionTypeId,
      request.connectionValue,
      false, // is_verified
      now,
      now
    )

    sql"""
      INSERT INTO client_connections (
        id, user_id, connection_type_id, connection_value, is_verified, created_at, updated_at
      )
      VALUES (
        ${connection.id}, ${connection.userId}, ${connection.connectionTypeId},
        ${connection.connectionValue}, ${connection.isVerified}, ${connection.createdAt}, ${connection.updatedAt}
      )
    """.update.run.transact(xa).as(connection)

  override def findClientConnectionById(id: UUID): IO[Option[ClientConnection]] =
    sql"""
      SELECT id, user_id, connection_type_id, connection_value, is_verified, created_at, updated_at
      FROM client_connections
      WHERE id = $id
    """.query[ClientConnection].option.transact(xa)

  override def findClientConnectionsByUser(userId: UserId): IO[List[ClientConnection]] =
    sql"""
      SELECT id, user_id, connection_type_id, connection_value, is_verified, created_at, updated_at
      FROM client_connections
      WHERE user_id = $userId
      ORDER BY created_at DESC
    """.query[ClientConnection].to[List].transact(xa)

  override def findClientConnectionByUserAndType(
    userId: UserId,
    connectionTypeId: ConnectionTypeId
  ): IO[Option[ClientConnection]] =
    sql"""
      SELECT id, user_id, connection_type_id, connection_value, is_verified, created_at, updated_at
      FROM client_connections
      WHERE user_id = $userId AND connection_type_id = $connectionTypeId
    """.query[ClientConnection].option.transact(xa)

  override def updateClientConnection(connection: ClientConnection): IO[ClientConnection] =
    val updated = connection.copy(updatedAt = Instant.now())
    sql"""
      UPDATE client_connections
      SET connection_value = ${updated.connectionValue},
          is_verified = ${updated.isVerified},
          updated_at = ${updated.updatedAt}
      WHERE id = ${updated.id}
    """.update.run.transact(xa).as(updated)

  override def deleteClientConnection(id: UUID): IO[Unit] =
    sql"DELETE FROM client_connections WHERE id = $id".update.run.transact(xa).void

  override def deleteClientConnectionsByUser(userId: UserId): IO[Unit] =
    sql"DELETE FROM client_connections WHERE user_id = $userId".update.run.transact(xa).void
