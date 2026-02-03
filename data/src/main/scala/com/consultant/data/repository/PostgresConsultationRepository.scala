package com.consultant.data.repository

import cats.effect.IO
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.postgres.pgisimplicits.*
import com.consultant.core.domain.*
import com.consultant.core.ports.ConsultationRepository
import java.util.UUID
import java.time.Instant

// Doobie Meta instances for custom types
given Meta[ConsultationStatus] =
  Meta[String].timap(s => ConsultationStatus.valueOf(s))(_.toString)

class PostgresConsultationRepository(xa: Transactor[IO]) extends ConsultationRepository:

  override def create(request: CreateConsultationRequest, price: BigDecimal): IO[Consultation] =
    val id  = UUID.randomUUID()
    val now = Instant.now()
    val consultation = Consultation(
      id,
      request.userId,
      request.specialistId,
      request.categoryId,
      request.description,
      ConsultationStatus.Requested,
      request.scheduledAt,
      request.duration,
      price,
      None,
      None,
      now,
      now
    )

    sql"""
      INSERT INTO consultations (
        id, user_id, specialist_id, category_id, description, status,
        scheduled_at, duration, price, created_at, updated_at
      )
      VALUES (
        ${consultation.id}, ${consultation.userId}, ${consultation.specialistId},
        ${consultation.categoryId}, ${consultation.description}, ${consultation.status},
        ${consultation.scheduledAt}, ${consultation.duration}, ${consultation.price},
        ${consultation.createdAt}, ${consultation.updatedAt}
      )
    """.update.run
      .transact(xa)
      .map(_ => consultation)

  override def findById(id: ConsultationId): IO[Option[Consultation]] =
    sql"""
      SELECT id, user_id, specialist_id, category_id, description, status,
             scheduled_at, duration, price, rating, review, created_at, updated_at
      FROM consultations
      WHERE id = $id
    """.query[Consultation].option.transact(xa)

  override def findByUser(userId: UserId, offset: Int, limit: Int): IO[List[Consultation]] =
    sql"""
      SELECT id, user_id, specialist_id, category_id, description, status,
             scheduled_at, duration, price, rating, review, created_at, updated_at
      FROM consultations
      WHERE user_id = $userId
      ORDER BY created_at DESC
      LIMIT $limit OFFSET $offset
    """.query[Consultation].to[List].transact(xa)

  override def findBySpecialist(
    specialistId: SpecialistId,
    offset: Int,
    limit: Int
  ): IO[List[Consultation]] =
    sql"""
      SELECT id, user_id, specialist_id, category_id, description, status,
             scheduled_at, duration, price, rating, review, created_at, updated_at
      FROM consultations
      WHERE specialist_id = $specialistId
      ORDER BY created_at DESC
      LIMIT $limit OFFSET $offset
    """.query[Consultation].to[List].transact(xa)

  override def update(consultation: Consultation): IO[Consultation] =
    val updated = consultation.copy(updatedAt = Instant.now())
    sql"""
      UPDATE consultations
      SET status = ${updated.status},
          scheduled_at = ${updated.scheduledAt},
          duration = ${updated.duration},
          price = ${updated.price},
          rating = ${updated.rating},
          review = ${updated.review},
          updated_at = ${updated.updatedAt}
      WHERE id = ${updated.id}
    """.update.run
      .transact(xa)
      .map(_ => updated)

  override def updateStatus(id: ConsultationId, status: ConsultationStatus): IO[Unit] =
    sql"""
      UPDATE consultations
      SET status = $status, updated_at = ${Instant.now()}
      WHERE id = $id
    """.update.run.transact(xa).void

  override def addReview(id: ConsultationId, rating: Int, review: String): IO[Unit] =
    sql"""
      UPDATE consultations
      SET rating = $rating, review = $review, updated_at = ${Instant.now()}
      WHERE id = $id
    """.update.run.transact(xa).void
