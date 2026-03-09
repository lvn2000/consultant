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
import com.consultant.core.ports.AvailabilityRepository
import java.util.UUID
import java.time.LocalTime

class PostgresAvailabilityRepository(xa: Transactor[IO]) extends AvailabilityRepository:

  override def create(specialistId: UUID, request: CreateAvailabilityRequest): IO[SpecialistAvailability] =
    val id  = UUID.randomUUID()
    val now = java.time.Instant.now()
    val availability = SpecialistAvailability(
      id,
      specialistId,
      request.dayOfWeek,
      request.startTime,
      request.endTime,
      now,
      now
    )

    sql"""
      INSERT INTO specialist_availability (id, specialist_id, day_of_week, start_time, end_time, created_at, updated_at)
      VALUES (${availability.id}, ${specialistId}, ${availability.dayOfWeek}, ${availability.startTime}, ${availability.endTime}, ${availability.createdAt}, ${availability.updatedAt})
    """.update.run
      .transact(xa)
      .map(_ => availability)

  override def findById(id: UUID): IO[Option[SpecialistAvailability]] =
    sql"""
      SELECT id, specialist_id, day_of_week, start_time, end_time, created_at, updated_at
      FROM specialist_availability
      WHERE id = $id
    """.query[SpecialistAvailability].option.transact(xa)

  override def findBySpecialist(specialistId: UUID): IO[List[SpecialistAvailability]] =
    sql"""
      SELECT id, specialist_id, day_of_week, start_time, end_time, created_at, updated_at
      FROM specialist_availability
      WHERE specialist_id = $specialistId
      ORDER BY day_of_week, start_time
    """.query[SpecialistAvailability].to[List].transact(xa)

  override def findBySpecialistAndDay(specialistId: UUID, dayOfWeek: Int): IO[List[SpecialistAvailability]] =
    sql"""
      SELECT id, specialist_id, day_of_week, start_time, end_time, created_at, updated_at
      FROM specialist_availability
      WHERE specialist_id = $specialistId AND day_of_week = $dayOfWeek
      ORDER BY start_time
    """.query[SpecialistAvailability].to[List].transact(xa)

  override def update(availability: SpecialistAvailability): IO[SpecialistAvailability] =
    sql"""
      UPDATE specialist_availability
      SET day_of_week = ${availability.dayOfWeek},
          start_time = ${availability.startTime},
          end_time = ${availability.endTime},
          updated_at = ${availability.updatedAt}
      WHERE id = ${availability.id}
    """.update.run
      .transact(xa)
      .map(_ => availability)

  override def delete(id: UUID): IO[Unit] =
    sql"DELETE FROM specialist_availability WHERE id = $id".update.run
      .transact(xa)
      .void

  override def deleteBySpecialist(specialistId: UUID): IO[Unit] =
    sql"DELETE FROM specialist_availability WHERE specialist_id = $specialistId".update.run
      .transact(xa)
      .void
