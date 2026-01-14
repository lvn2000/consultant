package com.consultant.data.repository

import cats.effect.IO
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import com.consultant.core.domain.*
import com.consultant.core.ports.ConnectionTypeRepository
import java.time.Instant

class PostgresConnectionTypeRepository(xa: Transactor[IO]) extends ConnectionTypeRepository:

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
