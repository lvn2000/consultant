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
