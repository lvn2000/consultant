package com.consultant.data.repository

import cats.effect.IO
import cats.syntax.all.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import com.consultant.core.domain.*
import com.consultant.core.ports.SpecialistRepository
import java.util.UUID
import java.time.Instant

class PostgresSpecialistRepository(xa: Transactor[IO]) extends SpecialistRepository:

  override def create(request: CreateSpecialistRequest): IO[Specialist] =
    val id  = UUID.randomUUID()
    val now = Instant.now()
    val specialist = Specialist(
      id,
      request.email,
      request.name,
      request.phone,
      request.bio,
      request.categories,
      request.hourlyRate,
      request.experienceYears,
      None,
      0,
      true,
      now,
      now
    )

    sql"""
      INSERT INTO specialists (
        id, email, name, phone, bio, hourly_rate, experience_years,
        is_available, created_at, updated_at
      )
      VALUES (
        ${specialist.id}, ${specialist.email}, ${specialist.name}, ${specialist.phone},
        ${specialist.bio}, ${specialist.hourlyRate}, ${specialist.experienceYears},
        ${specialist.isAvailable}, ${specialist.createdAt}, ${specialist.updatedAt}
      )
    """.update.run.transact(xa) *>
      insertSpecialistCategories(specialist.id, request.categories).as(specialist)

  override def findById(id: SpecialistId): IO[Option[Specialist]] =
    for
      specialistOpt <- sql"""
        SELECT id, email, name, phone, bio, hourly_rate, experience_years,
               rating, total_consultations, is_available, created_at, updated_at
        FROM specialists
        WHERE id = $id
      """
        .query[
          (UUID, String, String, String, String, BigDecimal, Int, Option[BigDecimal], Int, Boolean, Instant, Instant)
        ]
        .option
        .transact(xa)

      result <- specialistOpt match
        case Some((id, email, name, phone, bio, rate, exp, rating, total, available, created, updated)) =>
          getSpecialistCategories(id).map { categories =>
            Some(
              Specialist(id, email, name, phone, bio, categories, rate, exp, rating, total, available, created, updated)
            )
          }
        case None => IO.pure(None)
    yield result

  override def findByEmail(email: String): IO[Option[Specialist]] =
    for
      specialistOpt <- sql"""
        SELECT id FROM specialists WHERE email = $email
      """.query[UUID].option.transact(xa)

      result <- specialistOpt match
        case Some(id) => findById(id)
        case None     => IO.pure(None)
    yield result

  override def search(
    criteria: SpecialistSearchCriteria,
    offset: Int,
    limit: Int
  ): IO[List[Specialist]] =
    val categoryFilter = criteria.categoryId.map(cid =>
      fr"EXISTS (SELECT 1 FROM specialist_categories WHERE specialist_id = s.id AND category_id = $cid)"
    )
    val ratingFilter     = criteria.minRating.map(r => fr"s.rating >= $r")
    val rateFilter       = criteria.maxHourlyRate.map(r => fr"s.hourly_rate <= $r")
    val experienceFilter = criteria.minExperience.map(e => fr"s.experience_years >= $e")
    val availableFilter  = criteria.isAvailable.map(a => fr"s.is_available = $a")

    val filters     = List(categoryFilter, ratingFilter, rateFilter, experienceFilter, availableFilter).flatten
    val whereClause = if filters.isEmpty then Fragment.empty else fr"WHERE" ++ filters.reduce(_ ++ fr"AND" ++ _)

    val query = fr"""
      SELECT id, email, name, phone, bio, hourly_rate, experience_years,
             rating, total_consultations, is_available, created_at, updated_at
      FROM specialists s
    """ ++ whereClause ++ fr"""
      ORDER BY rating DESC NULLS LAST, total_consultations DESC
      LIMIT $limit OFFSET $offset
    """

    query
      .query[
        (UUID, String, String, String, String, BigDecimal, Int, Option[BigDecimal], Int, Boolean, Instant, Instant)
      ]
      .to[List]
      .transact(xa)
      .flatMap { specialists =>
        specialists.traverse {
          case (id, email, name, phone, bio, rate, exp, rating, total, available, created, updated) =>
            getSpecialistCategories(id).map { categories =>
              Specialist(id, email, name, phone, bio, categories, rate, exp, rating, total, available, created, updated)
            }
        }
      }

  override def update(specialist: Specialist): IO[Specialist] =
    val updated = specialist.copy(updatedAt = Instant.now())
    sql"""
      UPDATE specialists
      SET email = ${updated.email},
          name = ${updated.name},
          phone = ${updated.phone},
          bio = ${updated.bio},
          hourly_rate = ${updated.hourlyRate},
          experience_years = ${updated.experienceYears},
          is_available = ${updated.isAvailable},
          updated_at = ${updated.updatedAt}
      WHERE id = ${updated.id}
    """.update.run.transact(xa) *>
      deleteSpecialistCategories(updated.id) *>
      insertSpecialistCategories(updated.id, updated.categories).as(updated)

  override def delete(id: SpecialistId): IO[Unit] =
    deleteSpecialistCategories(id) *>
      sql"DELETE FROM specialists WHERE id = $id".update.run.transact(xa).void

  override def updateRating(id: SpecialistId, rating: BigDecimal, consultationCount: Int): IO[Unit] =
    sql"""
      UPDATE specialists
      SET rating = $rating, total_consultations = $consultationCount
      WHERE id = $id
    """.update.run.transact(xa).void

  private def insertSpecialistCategories(specialistId: UUID, categories: List[UUID]): IO[Unit] =
    categories.traverse_ { categoryId =>
      sql"""
        INSERT INTO specialist_categories (specialist_id, category_id)
        VALUES ($specialistId, $categoryId)
      """.update.run.transact(xa)
    }

  private def deleteSpecialistCategories(specialistId: UUID): IO[Unit] =
    sql"DELETE FROM specialist_categories WHERE specialist_id = $specialistId".update.run.transact(xa).void

  private def getSpecialistCategories(specialistId: UUID): IO[List[UUID]] =
    sql"""
      SELECT category_id FROM specialist_categories WHERE specialist_id = $specialistId
    """.query[UUID].to[List].transact(xa)
