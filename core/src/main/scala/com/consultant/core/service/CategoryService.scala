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
import com.consultant.core.validation.CategoryValidator
import com.consultant.core.validation.ValidationResult.*

class CategoryService(categoryRepo: CategoryRepository):

  def createCategory(request: CreateCategoryRequest): IO[Either[DomainError, Category]] =
    CategoryValidator.validateCreate(request).toEither match
      case Left(error) => IO.pure(Left(error))
      case Right(_) =>
        for
          existing <- categoryRepo.findByName(request.name)
          result <- existing match
            case Some(_) => IO.pure(Left(DomainError.ValidationError(s"Category '${request.name}' already exists")))
            case None    => categoryRepo.create(request).map(Right(_)).handleError(parseError)
        yield result

  def getCategory(id: CategoryId): IO[Either[DomainError, Category]] =
    categoryRepo.findById(id).map {
      case Some(category) => Right(category)
      case None           => Left(DomainError.CategoryNotFound(id))
    }

  def listCategories(): IO[List[Category]] =
    categoryRepo.listAll()

  def updateCategory(category: Category): IO[Either[DomainError, Category]] =
    CategoryValidator.validateUpdate(category).toEither match
      case Left(error) => IO.pure(Left(error))
      case Right(_)    => categoryRepo.update(category).map(Right(_)).handleError(parseError)

  def deleteCategory(id: CategoryId): IO[Either[DomainError, Unit]] =
    categoryRepo.findById(id).flatMap {
      case Some(_) => categoryRepo.delete(id).map(Right(_)).handleError(parseError)
      case None    => IO.pure(Left(DomainError.CategoryNotFound(id)))
    }

  /** Parses database errors into structured domain errors */
  private def parseError(error: Throwable): Either[DomainError, Nothing] =
    import com.consultant.core.error.PostgresErrorParser
    Left(PostgresErrorParser.parseError(error))

end CategoryService
