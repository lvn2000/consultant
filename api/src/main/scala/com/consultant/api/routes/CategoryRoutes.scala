package com.consultant.api.routes

import cats.effect.IO
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import com.consultant.api.dto.*
import com.consultant.core.service.CategoryService
import com.consultant.core.domain.Category
import com.consultant.api.DtoMappers.*
import java.util.UUID
import sttp.tapir.server.http4s.Http4sServerInterpreter
import org.http4s.HttpRoutes

class CategoryRoutes(categoryService: CategoryService):

  private val baseEndpoint = endpoint

  // Create category
  val createCategoryEndpoint = baseEndpoint.post
    .in(jsonBody[CreateCategoryDto])
    .out(jsonBody[CategoryDto])
    .errorOut(jsonBody[ErrorResponse])

  val createCategory = createCategoryEndpoint.serverLogic { dto =>
    categoryService.createCategory(toCreateCategoryRequest(dto)).map {
      case Right(category) => Right(toCategoryDto(category))
      case Left(error)     => Left(toErrorResponse(error))
    }
  }

  // Get category by ID
  val getCategoryEndpoint = baseEndpoint.get
    .in(path[UUID]("categoryId"))
    .out(jsonBody[CategoryDto])
    .errorOut(jsonBody[ErrorResponse])

  val getCategory = getCategoryEndpoint.serverLogic { id =>
    categoryService.getCategory(id).map {
      case Right(category) => Right(toCategoryDto(category))
      case Left(error)     => Left(toErrorResponse(error))
    }
  }

  // List all categories
  // List categories
  val listCategoriesEndpoint = baseEndpoint.get
    .out(jsonBody[List[CategoryDto]])

  val listCategories = listCategoriesEndpoint.serverLogic { _ =>
    categoryService
      .listCategories()
      .map(categories => Right(categories.map(toCategoryDto)))
  }

  // Update category
  val updateCategoryEndpoint = baseEndpoint.put
    .in(path[UUID]("categoryId"))
    .in(jsonBody[UpdateCategoryDto])
    .out(jsonBody[CategoryDto])
    .errorOut(jsonBody[ErrorResponse])

  val updateCategory = updateCategoryEndpoint.serverLogic { case (id, dto) =>
    categoryService.updateCategory(Category(id, dto.name, dto.description, dto.parentId)).map {
      case Right(category) => Right(toCategoryDto(category))
      case Left(error)     => Left(toErrorResponse(error))
    }
  }

  // Delete category
  val deleteCategoryEndpoint = baseEndpoint.delete
    .in(path[UUID]("categoryId"))
    .out(stringBody)
    .errorOut(jsonBody[ErrorResponse])

  val deleteCategory = deleteCategoryEndpoint.serverLogic { id =>
    categoryService.deleteCategory(id).map {
      case Right(_)    => Right("Category deleted")
      case Left(error) => Left(toErrorResponse(error))
    }
  }

  val endpoints = List(createCategory, getCategory, listCategories, updateCategory, deleteCategory)

  val routes: HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(endpoints)
