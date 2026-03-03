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

  /** Checks if exception is a PostgreSQL PSQLException using reflection */
  private def isPostgresException(ex: Throwable): Boolean =
    ex.getClass.getName == "org.postgresql.util.PSQLException"

  /** Gets SQLState from PostgreSQL exception using reflection */
  private def getSqlState(ex: Throwable): Option[String] =
    try
      val method = ex.getClass.getMethod("getSQLState")
      Option(method.invoke(ex).asInstanceOf[String])
    catch case _: Exception => None
