package com.consultant.api.routes

import cats.effect.IO
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import com.consultant.api.dto.*
import com.consultant.core.service.CategoryService
import com.consultant.core.domain.Category
import com.consultant.api.mappers.CategoryMappers.*
import com.consultant.api.mappers.ErrorMappers.*
import java.util.UUID
import sttp.tapir.server.http4s.Http4sServerInterpreter
import org.http4s.HttpRoutes

class CategoryRoutes(categoryService: CategoryService):

  /**
   * SECURITY MODEL: The X-User-Role header is set by TokenAuthMiddleware from the verified JWT token. The middleware
   * overwrites any client-provided values for this header, ensuring the routes always receive trusted values extracted
   * from the authenticated token.
   *
   * Admin authorization checks (role.equalsIgnoreCase("Admin")) are safe because:
   *   1. The middleware verifies the JWT signature cryptographically 2. The role is extracted from the verified token
   *      claims 3. Client-supplied X-User-Role headers are replaced with the token's actual role
   */

  // Create category
  val createCategoryEndpoint = ApiEndpoints
    .adminEndpoint("createCategory", "Create a new category")
    .post
    .in(header[Option[String]]("X-User-Role"))
    .in(jsonBody[CreateCategoryDto])
    .out(jsonBody[CategoryDto])

  val createCategory = createCategoryEndpoint.serverLogic { (userRoleOpt, dto) =>
    userRoleOpt match
      case Some(role) if role.equalsIgnoreCase("Admin") =>
        categoryService.createCategory(toCreateCategoryRequest(dto)).map {
          case Right(category) => Right(toCategoryDto(category))
          case Left(error)     => Left(toErrorResponse(error))
        }
      case _ =>
        IO.pure(Left(ErrorResponse("FORBIDDEN", "Admin role required")))
  }

  // Get category by ID
  val getCategoryEndpoint = ApiEndpoints
    .publicEndpoint("getCategory", "Get category by ID")
    .get
    .in(path[UUID]("categoryId"))
    .out(jsonBody[CategoryDto])

  val getCategory = getCategoryEndpoint.serverLogic { id =>
    categoryService.getCategory(id).map {
      case Right(category) => Right(toCategoryDto(category))
      case Left(error)     => Left(toErrorResponse(error))
    }
  }

  // List all categories
  val listCategoriesEndpoint = ApiEndpoints
    .publicEndpoint("listCategories", "List all categories")
    .get
    .out(jsonBody[List[CategoryDto]])

  val listCategories = listCategoriesEndpoint.serverLogic { _ =>
    categoryService
      .listCategories()
      .map(categories => Right(categories.map(toCategoryDto)))
  }

  // Update category
  val updateCategoryEndpoint = ApiEndpoints
    .adminEndpoint("updateCategory", "Update a category")
    .put
    .in(path[UUID]("categoryId"))
    .in(header[Option[String]]("X-User-Role"))
    .in(jsonBody[UpdateCategoryDto])
    .out(jsonBody[CategoryDto])

  val updateCategory = updateCategoryEndpoint.serverLogic { case (id, userRoleOpt, dto) =>
    userRoleOpt match
      case Some(role) if role.equalsIgnoreCase("Admin") =>
        categoryService.updateCategory(Category(id, dto.name, dto.description, dto.parentId)).map {
          case Right(category) => Right(toCategoryDto(category))
          case Left(error)     => Left(toErrorResponse(error))
        }
      case _ =>
        IO.pure(Left(ErrorResponse("FORBIDDEN", "Admin role required")))
  }

  // Delete category
  val deleteCategoryEndpoint = ApiEndpoints
    .adminEndpoint("deleteCategory", "Delete a category")
    .delete
    .in(path[UUID]("categoryId"))
    .in(header[Option[String]]("X-User-Role"))
    .out(stringBody)

  val deleteCategory = deleteCategoryEndpoint.serverLogic { case (id, userRoleOpt) =>
    userRoleOpt match
      case Some(role) if role.equalsIgnoreCase("Admin") =>
        categoryService.deleteCategory(id).map {
          case Right(_)    => Right("Category deleted")
          case Left(error) => Left(toErrorResponse(error))
        }
      case _ =>
        IO.pure(Left(ErrorResponse("FORBIDDEN", "Admin role required")))
  }

  val endpoints = List(createCategory, getCategory, listCategories, updateCategory, deleteCategory)

  val routes: HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(endpoints)
