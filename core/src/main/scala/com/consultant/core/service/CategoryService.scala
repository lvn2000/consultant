package com.consultant.core.service

import cats.effect.IO
import com.consultant.core.domain.*
import com.consultant.core.ports.*

class CategoryService(categoryRepo: CategoryRepository):

  def createCategory(request: CreateCategoryRequest): IO[Either[DomainError, Category]] =
    for
      existing <- categoryRepo.findByName(request.name)
      result <- existing match
        case Some(_) => IO.pure(Left(DomainError.ValidationError(s"Category '${request.name}' already exists")))
        case None    => categoryRepo.create(request).map(Right(_))
    yield result

  def getCategory(id: CategoryId): IO[Either[DomainError, Category]] =
    categoryRepo.findById(id).map {
      case Some(category) => Right(category)
      case None           => Left(DomainError.CategoryNotFound(id))
    }

  def listCategories(): IO[List[Category]] =
    categoryRepo.listAll()

  def updateCategory(category: Category): IO[Either[DomainError, Category]] =
    categoryRepo.update(category).map(Right(_))
