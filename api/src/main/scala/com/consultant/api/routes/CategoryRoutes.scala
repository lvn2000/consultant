package com.consultant.api.routes

import cats.effect.IO
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import com.consultant.api.dto.*
import com.consultant.core.service.CategoryService
import com.consultant.api.DtoMappers.*
import java.util.UUID
import sttp.tapir.server.http4s.Http4sServerInterpreter
import org.http4s.HttpRoutes

class CategoryRoutes(categoryService: CategoryService):

  private val baseEndpoint = endpoint.in("api" / "categories")

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
  val listCategoriesEndpoint = baseEndpoint.get
    .out(jsonBody[List[CategoryDto]])

  val listCategories = listCategoriesEndpoint.serverLogic { _ =>
    categoryService
      .listCategories()
      .map(categories => Right(categories.map(toCategoryDto)))
  }

  val endpoints = List(createCategory, getCategory, listCategories)

  val routes: HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(endpoints)
