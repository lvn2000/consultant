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

import cats.data.NonEmptyList
import cats.effect.Async
import doobie.free.connection

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

/** PostgreSQL implementation of SpecialistRepository with transactional support. */
class PostgresSpecialistRepository(xa: Transactor[IO], connectionRepo: ConnectionRepository)
    extends SpecialistRepository
    with TransactionalSpecialistRepository:

  override def create(request: CreateSpecialistRequest): IO[Specialist] =
    createTransactional(request).transact(xa)

  override def createTransactional(request: CreateSpecialistRequest): ConnectionIO[Specialist] = {
    val id  = request.id.getOrElse(UUID.randomUUID())
    val now = Instant.now()
    val (hourlyRate, experienceYears) =
      if request.categoryRates.nonEmpty then
        val first = request.categoryRates.head
        (first.hourlyRate, first.experienceYears)
      else (BigDecimal(0), 0)
    val specialist = Specialist(
      id,
      request.email,
      request.name,
      request.phone,
      request.bio,
      request.categoryRates,
      request.isAvailable,
      List(), // empty connections for new specialist
      request.countryId,
      request.languages,
      now,
      now
    )
    for {
      _ <- sql"""
        INSERT INTO specialists (
          id, email, name, phone, bio, hourly_rate, experience_years, is_available, country_id, created_at, updated_at
        )
        VALUES (
          $id, ${request.email}, ${request.name}, ${request.phone}, ${request.bio}, $hourlyRate, $experienceYears, ${request.isAvailable}, ${request.countryId}, $now, $now
        )
      """.update.run
      _ <- insertSpecialistCategoryRates(id, request.categoryRates)
      _ <- insertSpecialistLanguages(id, request.languages)
    } yield specialist
  }

  override def findById(id: SpecialistId): IO[Option[Specialist]] =
    findByIdTransactional(id).transact(xa).flatMap {
      case None => IO.pure(None)
      case Some((id, email, name, phone, bio, available, countryId, created, updated, langs, categoryRates)) =>
        connectionRepo.findBySpecialist(id).map { connections =>
          Some(
            Specialist(
              id,
              email,
              name,
              phone,
              bio,
              categoryRates,
              available,
              connections,
              countryId,
              langs,
              created,
              updated
            )
          )
        }
    }

  override def findByIdTransactional(id: SpecialistId): ConnectionIO[Option[
    (
      UUID,
      String,
      String,
      String,
      String,
      Boolean,
      Option[UUID],
      Instant,
      Instant,
      Set[UUID],
      List[SpecialistCategoryRate]
    )
  ]] =
    for {
      specialistOpt <- sql"""
        SELECT id, email, name, phone, bio, is_available, country_id, created_at, updated_at
        FROM specialists
        WHERE id = $id
      """.query[(UUID, String, String, String, String, Boolean, Option[UUID], Instant, Instant)].option
      langs <- sql"SELECT language_id FROM specialist_languages WHERE specialist_id = $id".query[UUID].to[List]
      catRates <- specialistOpt match {
        case Some((id, _, _, _, _, _, _, _, _)) => getSpecialistCategoryRates(id).map(Some(_))
        case None                               => connection.pure(None)
      }
    } yield for {
      (id, email, name, phone, bio, available, countryId, created, updated) <- specialistOpt
      categoryRates                                                         <- catRates
    } yield (
      id,
      email,
      name,
      phone,
      bio,
      available,
      countryId,
      created,
      updated,
      langs.toSet,
      categoryRates
    )

  override def findByEmail(email: String): IO[Option[Specialist]] =
    sql"SELECT id FROM specialists WHERE email = $email"
      .query[UUID]
      .option
      .transact(xa)
      .flatMap {
        case Some(id) => findById(id)
        case None     => IO.pure(None)
      }

  override def search(
    criteria: SpecialistSearchCriteria,
    offset: Int,
    limit: Int
  ): IO[List[Specialist]] = {
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
      SELECT id, email, name, phone, bio, is_available, country_id, created_at, updated_at
      FROM specialists s
    """ ++ whereClause ++ fr"""
      ORDER BY created_at DESC
      LIMIT $limit OFFSET $offset
    """

    val specialistsQ = query
      .query[(UUID, String, String, String, String, Boolean, Option[UUID], Instant, Instant)]
      .to[List]
    def languagesQ(ids: List[UUID]): ConnectionIO[List[(UUID, UUID)]] =
      NonEmptyList.fromList(ids) match {
        case Some(nel) =>
          (fr"SELECT specialist_id, language_id FROM specialist_languages WHERE " ++ Fragments.in(
            fr"specialist_id",
            nel
          )).query[(UUID, UUID)].to[List]
        case None => connection.pure(List.empty[(UUID, UUID)])
      }

    val action
      : ConnectionIO[List[(UUID, String, String, String, String, Boolean, Option[UUID], Instant, Instant, Set[UUID])]] =
      for {
        specialists <- specialistsQ
        langPairs   <- languagesQ(specialists.map(_._1))
        langMap = langPairs.groupMap(_._1)(_._2).view.mapValues(_.toSet).toMap
      } yield specialists.map { case (id, email, name, phone, bio, available, countryId, created, updated) =>
        (
          id,
          email,
          name,
          phone,
          bio,
          available,
          countryId,
          created,
          updated,
          langMap.getOrElse(id, Set.empty[LanguageId])
        )
      }
    action.transact(xa).flatMap { rows =>
      rows.parTraverse { case (id, email, name, phone, bio, available, countryId, created, updated, langs) =>
        for {
          categoryRates <- getSpecialistCategoryRates(id).transact(xa)
          connections   <- connectionRepo.findBySpecialist(id)
        } yield Specialist(
          id,
          email,
          name,
          phone,
          bio,
          categoryRates,
          available,
          connections,
          countryId,
          langs,
          created,
          updated
        )
      }
    }
  }

  override def update(specialist: Specialist): IO[Specialist] =
    updateTransactional(specialist).transact(xa)

  override def updateTransactional(specialist: Specialist): ConnectionIO[Specialist] = {
    val updated = specialist.copy(updatedAt = Instant.now())
    for {
      _ <- sql"""
        UPDATE specialists
        SET email = ${updated.email},
            name = ${updated.name},
            phone = ${updated.phone},
            bio = ${updated.bio},
            is_available = ${updated.isAvailable},
            country_id = ${updated.countryId},
            updated_at = ${updated.updatedAt}
        WHERE id = ${updated.id}
      """.update.run
      _ <- deleteSpecialistCategoryRates(updated.id)
      _ <- insertSpecialistCategoryRates(updated.id, updated.categoryRates)
      _ <- deleteSpecialistLanguages(updated.id)
      _ <- insertSpecialistLanguages(updated.id, updated.languages)
    } yield updated
  }
  // --- Specialist Languages helpers ---
  private def insertSpecialistLanguages(specialistId: UUID, languages: Set[UUID]): ConnectionIO[Unit] =
    languages.toList.traverse_ { langId =>
      sql"""
        INSERT INTO specialist_languages (specialist_id, language_id)
        VALUES ($specialistId, $langId)
      """.update.run.void
    }

  private def deleteSpecialistLanguages(specialistId: UUID): ConnectionIO[Unit] =
    sql"DELETE FROM specialist_languages WHERE specialist_id = $specialistId".update.run.void

  override def delete(id: SpecialistId): IO[Unit] =
    // Note: connectionRepo operations cannot be in the same transaction as they use a different transactor
    for {
      _ <- deleteTransactional(id).transact(xa)
      _ <- connectionRepo.deleteBySpecialist(id)
    } yield ()

  override def deleteTransactional(id: SpecialistId): ConnectionIO[Int] =
    for {
      _    <- deleteSpecialistCategoryRates(id)
      _    <- deleteSpecialistLanguages(id)
      rows <- sql"DELETE FROM specialists WHERE id = $id".update.run
    } yield rows

  override def updateCategoryRating(
    specialistId: SpecialistId,
    categoryId: CategoryId,
    rating: BigDecimal,
    consultationCount: Int
  ): IO[Unit] =
    updateCategoryRatingTransactional(specialistId, categoryId, rating, consultationCount).transact(xa).void

  override def updateCategoryRatingTransactional(
    specialistId: SpecialistId,
    categoryId: CategoryId,
    rating: BigDecimal,
    consultationCount: Int
  ): ConnectionIO[Int] =
    sql"""
      UPDATE specialist_category_rates
      SET rating = $rating, total_consultations = $consultationCount
      WHERE specialist_id = $specialistId AND category_id = $categoryId
    """.update.run

  private def insertSpecialistCategoryRates(
    specialistId: UUID,
    categoryRates: List[SpecialistCategoryRate]
  ): ConnectionIO[Unit] =
    categoryRates.traverse_ { rate =>
      sql"""
        INSERT INTO specialist_category_rates (
          specialist_id, category_id, hourly_rate, experience_years, rating, total_consultations
        )
        VALUES (
          $specialistId, ${rate.categoryId}, ${rate.hourlyRate},
          ${rate.experienceYears}, ${rate.rating}, ${rate.totalConsultations}
        )
      """.update.run.void
    }

  private def deleteSpecialistCategoryRates(specialistId: UUID): ConnectionIO[Unit] =
    sql"DELETE FROM specialist_category_rates WHERE specialist_id = $specialistId".update.run.void

  private def getSpecialistCategoryRates(specialistId: UUID): ConnectionIO[List[SpecialistCategoryRate]] =
    sql"""
      SELECT category_id, hourly_rate, experience_years, rating, total_consultations
      FROM specialist_category_rates
      WHERE specialist_id = $specialistId
    """.query[SpecialistCategoryRate].to[List]
