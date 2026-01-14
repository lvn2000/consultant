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
