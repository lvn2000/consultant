package com.consultant.core.service

import cats.effect.IO
import cats.syntax.all.*
import com.consultant.core.domain.*
import com.consultant.core.ports.*
import com.consultant.core.validation.SpecialistValidator
import com.consultant.core.validation.ValidationResult.*

class SpecialistService(
  specialistRepo: SpecialistRepository,
  categoryRepo: CategoryRepository
):

  def createSpecialist(request: CreateSpecialistRequest): IO[Either[DomainError, Specialist]] =
    for
      existing <- specialistRepo.findByEmail(request.email)
      result <- existing match
        case Some(_) => IO.pure(Left(DomainError.EmailAlreadyExists(request.email)))
        case None =>
          validateAndCreate(request)
    yield result

  def getSpecialist(id: SpecialistId): IO[Either[DomainError, Specialist]] =
    specialistRepo.findById(id).map {
      case Some(specialist) => Right(specialist)
      case None             => Left(DomainError.SpecialistNotFound(id))
    }

  def searchSpecialists(
    criteria: SpecialistSearchCriteria,
    offset: Int,
    limit: Int
  ): IO[List[Specialist]] =
    specialistRepo.search(criteria, offset, limit)

  def updateSpecialist(specialist: Specialist): IO[Either[DomainError, Specialist]] =
    specialistRepo
      .update(specialist)
      .map(Right(_))
      .handleError(parseError)

  def deleteSpecialist(id: SpecialistId): IO[Either[DomainError, Unit]] =
    specialistRepo.findById(id).flatMap {
      case Some(_) => specialistRepo.delete(id).as(Right(()))
      case None    => IO.pure(Left(DomainError.SpecialistNotFound(id)))
    }

  private def validateAndCreate(request: CreateSpecialistRequest): IO[Either[DomainError, Specialist]] =
    SpecialistValidator.validateCreate(request).toEither match
      case Left(error) => IO.pure(Left(error))
      case Right(_) =>
        specialistRepo
          .create(request)
          .map(Right(_))
          .handleError(parseError)

  /**
   * Parses database errors into structured domain errors. This uses SQLState codes instead of string parsing for
   * reliability. Uses reflection to avoid direct PostgreSQL dependency in core module.
   */
  private def parseError(error: Throwable): Either[DomainError, Specialist] =
    import com.consultant.core.error.PostgresErrorParser
    Left(PostgresErrorParser.parseError(error))

  private def parseConstraintName(message: String): Option[String] =
    val pattern = """violates unique constraint "([^"]+)"""".r
    pattern.findFirstMatchIn(message).map(_.group(1))
