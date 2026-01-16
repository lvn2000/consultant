package com.consultant.data.repository

import cats.effect.IO
import cats.syntax.all.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import com.consultant.core.domain.*
import com.consultant.core.ports.SpecialistRepository
import com.consultant.core.ports.ConnectionRepository
import java.util.UUID
import java.time.Instant

class PostgresSpecialistRepository(xa: Transactor[IO], connectionRepo: ConnectionRepository)
    extends SpecialistRepository:

  override def create(request: CreateSpecialistRequest): IO[Specialist] =
    val id  = UUID.randomUUID()
    val now = Instant.now()
    val specialist = Specialist(
      id,
      request.email,
      request.name,
      request.phone,
      request.bio,
      request.categoryRates,
      request.isAvailable,
      List(), // empty connections for new specialist
      now,
      now
    )

    sql"""
      INSERT INTO specialists (
        id, email, name, phone, bio, is_available, created_at, updated_at
      )
      VALUES (
        ${specialist.id}, ${specialist.email}, ${specialist.name}, ${specialist.phone},
        ${specialist.bio}, ${specialist.isAvailable}, ${specialist.createdAt}, ${specialist.updatedAt}
      )
    """.update.run.transact(xa) *>
      insertSpecialistCategoryRates(specialist.id, request.categoryRates).as(specialist)

  override def findById(id: SpecialistId): IO[Option[Specialist]] =
    for
      specialistOpt <- sql"""
        SELECT id, email, name, phone, bio, is_available, created_at, updated_at
        FROM specialists
        WHERE id = $id
      """
        .query[(UUID, String, String, String, String, Boolean, Instant, Instant)]
        .option
        .transact(xa)

      result <- specialistOpt match
        case Some((id, email, name, phone, bio, available, created, updated)) =>
          for
            categoryRates <- getSpecialistCategoryRates(id)
            connections   <- connectionRepo.findBySpecialist(id)
          yield Some(
            Specialist(
              id,
              email,
              name,
              phone,
              bio,
              categoryRates,
              available,
              connections,
              created,
              updated
            )
          )
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
      fr"EXISTS (SELECT 1 FROM specialist_category_rates WHERE specialist_id = s.id AND category_id = $cid)"
    )
    val ratingFilter = criteria.minRating.map(r =>
      fr"EXISTS (SELECT 1 FROM specialist_category_rates scr WHERE scr.specialist_id = s.id AND scr.rating >= $r)"
    )
    val rateFilter = criteria.maxHourlyRate.map(r =>
      fr"EXISTS (SELECT 1 FROM specialist_category_rates scr WHERE scr.specialist_id = s.id AND scr.hourly_rate <= $r)"
    )
    val experienceFilter = criteria.minExperience.map(e =>
      fr"EXISTS (SELECT 1 FROM specialist_category_rates scr WHERE scr.specialist_id = s.id AND scr.experience_years >= $e)"
    )
    val availableFilter = criteria.isAvailable.map(a => fr"s.is_available = $a")

    val filters     = List(categoryFilter, ratingFilter, rateFilter, experienceFilter, availableFilter).flatten
    val whereClause = if filters.isEmpty then Fragment.empty else fr"WHERE" ++ filters.reduce(_ ++ fr"AND" ++ _)

    val query = fr"""
      SELECT id, email, name, phone, bio, is_available, created_at, updated_at
      FROM specialists s
    """ ++ whereClause ++ fr"""
      ORDER BY created_at DESC
      LIMIT $limit OFFSET $offset
    """

    query
      .query[(UUID, String, String, String, String, Boolean, Instant, Instant)]
      .to[List]
      .transact(xa)
      .flatMap { specialists =>
        specialists.traverse { case (id, email, name, phone, bio, available, created, updated) =>
          for
            categoryRates <- getSpecialistCategoryRates(id)
            connections   <- connectionRepo.findBySpecialist(id)
          yield Specialist(
            id,
            email,
            name,
            phone,
            bio,
            categoryRates,
            available,
            connections,
            created,
            updated
          )
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
          is_available = ${updated.isAvailable},
          updated_at = ${updated.updatedAt}
      WHERE id = ${updated.id}
    """.update.run.transact(xa) *>
      deleteSpecialistCategoryRates(updated.id) *>
      insertSpecialistCategoryRates(updated.id, updated.categoryRates).as(updated)

  override def delete(id: SpecialistId): IO[Unit] =
    deleteSpecialistCategoryRates(id) *>
      connectionRepo.deleteBySpecialist(id) *>
      sql"DELETE FROM specialists WHERE id = $id".update.run.transact(xa).void

  override def updateCategoryRating(
    specialistId: SpecialistId,
    categoryId: CategoryId,
    rating: BigDecimal,
    consultationCount: Int
  ): IO[Unit] =
    sql"""
      UPDATE specialist_category_rates
      SET rating = $rating, total_consultations = $consultationCount
      WHERE specialist_id = $specialistId AND category_id = $categoryId
    """.update.run.transact(xa).void

  private def insertSpecialistCategoryRates(
    specialistId: UUID,
    categoryRates: List[SpecialistCategoryRate]
  ): IO[Unit] =
    categoryRates.traverse_ { rate =>
      sql"""
        INSERT INTO specialist_category_rates (
          specialist_id, category_id, hourly_rate, experience_years, rating, total_consultations
        )
        VALUES (
          $specialistId, ${rate.categoryId}, ${rate.hourlyRate},
          ${rate.experienceYears}, ${rate.rating}, ${rate.totalConsultations}
        )
      """.update.run.transact(xa)
    }

  private def deleteSpecialistCategoryRates(specialistId: UUID): IO[Unit] =
    sql"DELETE FROM specialist_category_rates WHERE specialist_id = $specialistId".update.run.transact(xa).void

  private def getSpecialistCategoryRates(specialistId: UUID): IO[List[SpecialistCategoryRate]] =
    sql"""
      SELECT category_id, hourly_rate, experience_years, rating, total_consultations
      FROM specialist_category_rates
      WHERE specialist_id = $specialistId
    """.query[SpecialistCategoryRate].to[List].transact(xa)
