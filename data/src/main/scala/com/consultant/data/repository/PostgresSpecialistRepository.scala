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

class PostgresSpecialistRepository(xa: Transactor[IO], connectionRepo: ConnectionRepository)
    extends SpecialistRepository:

  override def create(request: CreateSpecialistRequest): IO[Specialist] = {
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
      request.countryId,
      request.languages,
      now,
      now
    )
    val action: ConnectionIO[Unit] = for {
      _ <- sql"""
        INSERT INTO specialists (
          id, email, name, phone, bio, is_available, country_id, created_at, updated_at
        )
        VALUES (
          ${specialist.id}, ${specialist.email}, ${specialist.name}, ${specialist.phone},
          ${specialist.bio}, ${specialist.isAvailable}, ${specialist.countryId}, ${specialist.createdAt}, ${specialist.updatedAt}
        )
      """.update.run
      _ <- insertSpecialistCategoryRates(specialist.id, request.categoryRates)
      _ <- insertSpecialistLanguages(specialist.id, request.languages)
    } yield ()
    action.transact(xa).as(specialist)
  }

  override def findById(id: SpecialistId): IO[Option[Specialist]] = {
    val action: ConnectionIO[Option[
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
    ]] = for {
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
    action.transact(xa).flatMap {
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
      case None => IO.pure(None)
    }
  }

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

  override def update(specialist: Specialist): IO[Specialist] = {
    val updated = specialist.copy(updatedAt = Instant.now())
    val action: ConnectionIO[Unit] = for {
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
    } yield ()
    action.transact(xa).as(updated)
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
    val dbAction: ConnectionIO[Unit] = for {
      _ <- deleteSpecialistCategoryRates(id)
      _ <- deleteSpecialistLanguages(id)
      _ <- sql"DELETE FROM specialists WHERE id = $id".update.run.void
    } yield ()
    for {
      _ <- dbAction.transact(xa)
      _ <- connectionRepo.deleteBySpecialist(id)
    } yield ()

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
