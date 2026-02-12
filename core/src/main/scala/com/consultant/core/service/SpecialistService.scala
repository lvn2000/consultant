package com.consultant.core.service

import cats.effect.IO
import cats.syntax.all.*
import com.consultant.core.domain.*
import com.consultant.core.ports.*

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
      .handleError { error =>
        val errorMsg = error.getMessage
        if errorMsg != null && errorMsg.contains("duplicate key value violates unique constraint") && errorMsg.contains(
            "specialist_category_rates_pkey"
          )
        then Left(DomainError.DuplicateCategoryRate("unknown"))
        else if errorMsg != null && errorMsg.contains("duplicate key value violates unique constraint") then
          Left(
            DomainError.DatabaseError(
              s"A duplicate entry already exists. Please check that you haven't added this category before."
            )
          )
        else Left(DomainError.DatabaseError(s"Database error: ${error.getMessage}"))
      }

  def deleteSpecialist(id: SpecialistId): IO[Either[DomainError, Unit]] =
    specialistRepo.findById(id).flatMap {
      case Some(_) => specialistRepo.delete(id).as(Right(()))
      case None    => IO.pure(Left(DomainError.SpecialistNotFound(id)))
    }

  private def validateAndCreate(request: CreateSpecialistRequest): IO[Either[DomainError, Specialist]] =
    if request.categoryRates.exists(_.hourlyRate <= 0) then
      val invalidRate = request.categoryRates.find(_.hourlyRate <= 0).map(_.hourlyRate).getOrElse(BigDecimal(0))
      IO.pure(Left(DomainError.InvalidPrice(invalidRate)))
    else if request.categoryRates.exists(_.experienceYears < 0) then
      IO.pure(Left(DomainError.ValidationError("Experience years must be non-negative")))
    else
      specialistRepo
        .create(request)
        .map(Right(_))
        .handleError { error =>
          val errorMsg = error.getMessage
          if errorMsg != null && errorMsg.contains("duplicate key value violates unique constraint") && errorMsg
              .contains("specialist_category_rates_pkey")
          then Left(DomainError.DuplicateCategoryRate("unknown"))
          else if errorMsg != null && errorMsg.contains("duplicate key value violates unique constraint") then
            Left(DomainError.DatabaseError(s"A duplicate entry already exists. Please check your input."))
          else Left(DomainError.DatabaseError(s"Database error: ${error.getMessage}"))
        }
