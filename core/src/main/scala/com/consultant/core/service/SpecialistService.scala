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
    specialistRepo.update(specialist).map(Right(_))

  private def validateAndCreate(request: CreateSpecialistRequest): IO[Either[DomainError, Specialist]] =
    if request.hourlyRate <= 0 then IO.pure(Left(DomainError.InvalidPrice(request.hourlyRate)))
    else specialistRepo.create(request).map(Right(_))
