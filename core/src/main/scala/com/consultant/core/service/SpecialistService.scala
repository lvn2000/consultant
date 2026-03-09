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
  private def parseError(error: Throwable): Either[DomainError, Nothing] =
    import com.consultant.core.error.PostgresErrorParser
    Left(PostgresErrorParser.parseError(error))

end SpecialistService
