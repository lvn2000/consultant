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
import com.consultant.core.domain.*
import com.consultant.core.ports.ConnectionTypeRepository
import java.time.Instant

class PostgresConnectionTypeRepository(xa: Transactor[IO]) extends ConnectionTypeRepository:

  override def create(request: CreateConnectionTypeRequest): IO[ConnectionType] =
    val now = Instant.now()
    val connectionType = ConnectionType(
      java.util.UUID.randomUUID(),
      request.name,
      request.description,
      now,
      now
    )

    sql"""
      INSERT INTO connection_types (id, name, description, created_at, updated_at)
      VALUES (${connectionType.id}, ${connectionType.name}, ${connectionType.description},
              ${connectionType.createdAt}, ${connectionType.updatedAt})
    """.update.run.transact(xa).as(connectionType)

  override def findById(id: ConnectionTypeId): IO[Option[ConnectionType]] =
    sql"""
      SELECT id, name, description, created_at, updated_at
      FROM connection_types
      WHERE id = $id
    """.query[ConnectionType].option.transact(xa)

  override def listAll(): IO[List[ConnectionType]] =
    sql"""
      SELECT id, name, description, created_at, updated_at
      FROM connection_types
      ORDER BY name
    """.query[ConnectionType].to[List].transact(xa)

  override def findByName(name: String): IO[Option[ConnectionType]] =
    sql"""
      SELECT id, name, description, created_at, updated_at
      FROM connection_types
      WHERE name = $name
    """.query[ConnectionType].option.transact(xa)

  override def update(connectionType: ConnectionType): IO[ConnectionType] =
    val updated = connectionType.copy(updatedAt = Instant.now())
    sql"""
      UPDATE connection_types
      SET name = ${updated.name},
          description = ${updated.description},
          updated_at = ${updated.updatedAt}
      WHERE id = ${updated.id}
    """.update.run.transact(xa).as(updated)

  override def delete(id: ConnectionTypeId): IO[Unit] =
    sql"""
      DELETE FROM connection_types
      WHERE id = $id
    """.update.run.transact(xa).void
