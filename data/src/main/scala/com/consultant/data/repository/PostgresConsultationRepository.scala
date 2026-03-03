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

// Case class to hold consultation data along with total count from window function
case class ConsultationWithCount(consultation: Consultation, count: Long)

// Doobie Meta instances for custom types
given Meta[ConsultationStatus] =
  Meta[String].timap(s => ConsultationStatus.valueOf(s))(_.toString)

// Custom Read instance for ConsultationWithCount
given Read[ConsultationWithCount] =
  // Define a Read for the full row that includes all Consultation fields plus the count
  Read[
    (
      ConsultationId,
      UserId,
      SpecialistId,
      CategoryId,
      String,
      ConsultationStatus,
      Instant,
      Option[Int],
      BigDecimal,
      Option[Int],
      Option[String],
      Instant,
      Instant,
      Long // This is our count from COUNT(*) OVER()
    )
  ].map {
    case (
          id,
          userId,
          specialistId,
          categoryId,
          description,
          status,
          scheduledAt,
          duration,
          price,
          rating,
          review,
          createdAt,
          updatedAt,
          count
        ) =>
      val consultation = Consultation(
        id = id,
        userId = userId,
        specialistId = specialistId,
        categoryId = categoryId,
        description = description,
        status = status,
        scheduledAt = scheduledAt,
        duration = duration,
        price = price,
        rating = rating,
        review = review,
        createdAt = createdAt,
        updatedAt = updatedAt
      )
      ConsultationWithCount(consultation, count)
  }

class PostgresConsultationRepository(xa: Transactor[IO])
    extends ConsultationRepository
    with TransactionalConsultationRepository:

  override def create(request: CreateConsultationRequest, price: BigDecimal): IO[Consultation] =
    createTransactional(request, price).transact(xa)

  override def createTransactional(request: CreateConsultationRequest, price: BigDecimal): ConnectionIO[Consultation] =
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
    """.update.run.map(_ => consultation)

  override def findById(id: ConsultationId): IO[Option[Consultation]] =
    findByIdTransactional(id).transact(xa)

  override def findByIdTransactional(id: ConsultationId): ConnectionIO[Option[Consultation]] =
    sql"""
      SELECT id, user_id, specialist_id, category_id, description, status,
             scheduled_at, duration, price, rating, review, created_at, updated_at
      FROM consultations
      WHERE id = $id
    """.query[Consultation].option

  override def findByUser(userId: UserId, offset: Int, limit: Int): IO[List[Consultation]] =
    sql"""
      SELECT id, user_id, specialist_id, category_id, description, status,
             scheduled_at, duration, price, rating, review, created_at, updated_at
      FROM consultations
      WHERE user_id = $userId
      ORDER BY created_at DESC
      LIMIT $limit OFFSET $offset
    """.query[Consultation].to[List].transact(xa)

  override def findByUserCount(userId: UserId): IO[Long] =
    sql"""
      SELECT COUNT(*)::BIGINT
      FROM consultations
      WHERE user_id = $userId
    """.query[Long].unique.transact(xa)

  override def findByUserWithCount(userId: UserId, offset: Int, limit: Int): IO[(List[Consultation], Long)] =
    sql"""
      SELECT id, user_id, specialist_id, category_id, description, status,
             scheduled_at, duration, price, rating, review, created_at, updated_at,
             COUNT(*) OVER() AS total_count
      FROM consultations
      WHERE user_id = $userId
      ORDER BY created_at DESC
      LIMIT $limit OFFSET $offset
    """.query[ConsultationWithCount].to[List].transact(xa).map { results =>
      if (results.nonEmpty) {
        // All entries will have the same count value due to the window function
        (results.map(_.consultation), results.head.count)
      } else {
        (Nil, 0L)
      }
    }

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

  override def findBySpecialistCount(specialistId: SpecialistId): IO[Long] =
    sql"""
      SELECT COUNT(*)::BIGINT
      FROM consultations
      WHERE specialist_id = $specialistId
    """.query[Long].unique.transact(xa)

  override def findBySpecialistWithCount(
    specialistId: SpecialistId,
    offset: Int,
    limit: Int
  ): IO[(List[Consultation], Long)] =
    sql"""
      SELECT id, user_id, specialist_id, category_id, description, status,
             scheduled_at, duration, price, rating, review, created_at, updated_at,
             COUNT(*) OVER() AS total_count
      FROM consultations
      WHERE specialist_id = $specialistId
      ORDER BY created_at DESC
      LIMIT $limit OFFSET $offset
    """.query[ConsultationWithCount].to[List].transact(xa).map { results =>
      if (results.nonEmpty) {
        (results.map(_.consultation), results.head.count)
      } else {
        (Nil, 0L)
      }
    }

  override def update(consultation: Consultation): IO[Consultation] =
    updateTransactional(consultation).transact(xa)

  override def updateTransactional(consultation: Consultation): ConnectionIO[Consultation] =
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
    """.update.run.map(_ => updated)

  override def updateStatus(id: ConsultationId, status: ConsultationStatus): IO[Unit] =
    updateStatusTransactional(id, status.toString).transact(xa).void

  override def updateStatusTransactional(id: ConsultationId, status: String): ConnectionIO[Int] =
    sql"""
      UPDATE consultations
      SET status = $status, updated_at = ${Instant.now()}
      WHERE id = $id
    """.update.run

  override def addReview(id: ConsultationId, rating: Int, review: String): IO[Unit] =
    addReviewTransactional(id, rating, review).transact(xa).void

  override def addReviewTransactional(id: ConsultationId, rating: Int, review: String): ConnectionIO[Int] =
    sql"""
      UPDATE consultations
      SET rating = $rating, review = $review, updated_at = ${Instant.now()}
      WHERE id = $id
    """.update.run
