package com.consultant.api.routes

import cats.effect.IO
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import com.consultant.api.dto.*
import com.consultant.core.service.SpecialistService
import com.consultant.api.mappers.SpecialistMappers.*
import com.consultant.api.mappers.ErrorMappers.*
import java.util.UUID
import java.time.Instant
import sttp.tapir.server.http4s.Http4sServerInterpreter
import org.http4s.HttpRoutes

class SpecialistRoutes(specialistService: SpecialistService):

  /**
   * SECURITY MODEL: The X-User-Role header is set by TokenAuthMiddleware from the verified JWT token. The middleware
   * overwrites any client-provided values for this header, ensuring the routes always receive trusted values extracted
   * from the authenticated token.
   *
   * Admin authorization checks (role.equalsIgnoreCase("Admin")) are safe because:
   *   1. The middleware verifies the JWT signature cryptographically 2. The role is extracted from the verified token
   *      claims 3. Client-supplied X-User-Role headers are replaced with the token's actual role
   */

  // Create specialist
  val createSpecialistEndpoint = ApiEndpoints
    .publicEndpoint("createSpecialist", "Create a new specialist")
    .post
    .in(jsonBody[CreateSpecialistDto])
    .out(jsonBody[SpecialistDto])

  val createSpecialist = createSpecialistEndpoint.serverLogic { dto =>
    specialistService.createSpecialist(toCreateSpecialistRequest(dto)).map {
      case Right(specialist) => Right(toSpecialistDto(specialist))
      case Left(error)       => Left(toErrorResponse(error))
    }
  }

  // Get specialist by ID
  val getSpecialistEndpoint = ApiEndpoints
    .publicEndpoint("getSpecialist", "Get specialist by ID")
    .get
    .in(path[UUID]("specialistId"))
    .out(jsonBody[SpecialistDto])

  val getSpecialist = getSpecialistEndpoint.serverLogic { id =>
    specialistService.getSpecialist(id).map {
      case Right(specialist) => Right(toSpecialistDto(specialist))
      case Left(error)       => Left(toErrorResponse(error))
    }
  }

  // Search specialists
  val searchSpecialistsEndpoint = ApiEndpoints
    .publicEndpoint("searchSpecialists", "Search specialists")
    .get
    .in("search")
    .in(query[Option[UUID]]("categoryId"))
    .in(query[Option[BigDecimal]]("minRating"))
    .in(query[Option[BigDecimal]]("maxHourlyRate"))
    .in(query[Option[Int]]("minExperience"))
    .in(query[Option[Boolean]]("isAvailable"))
    .in(query[Option[Int]]("offset").default(Some(0)))
    .in(query[Option[Int]]("limit").default(Some(20)))
    .out(jsonBody[List[SpecialistDto]])

  val searchSpecialists = searchSpecialistsEndpoint.serverLogic {
    case (categoryId, minRating, maxRate, minExp, available, offset, limit) =>
      val criteria = SpecialistSearchDto(categoryId, minRating, maxRate, minExp, available)
      specialistService
        .searchSpecialists(
          toSpecialistSearchCriteria(criteria),
          offset.getOrElse(0),
          limit.getOrElse(20)
        )
        .map(specialists => Right(specialists.map(toSpecialistDto)))
  }

  // Update specialist
  val updateSpecialistEndpoint = ApiEndpoints
    .securedEndpoint("updateSpecialist", "Update specialist")
    .put
    .in(header[Option[String]]("X-Auth-User-Id"))
    .in(path[UUID]("specialistId"))
    .in(jsonBody[UpdateSpecialistDto])
    .out(jsonBody[SpecialistDto])

  val updateSpecialist = updateSpecialistEndpoint.serverLogic { case (authUserIdOpt, id, dto) =>
    (for
      authUserId <- IO.fromOption(authUserIdOpt)(new RuntimeException("Missing authentication header"))
      _ <-
        if authUserId != id.toString then
          IO.raiseError(new RuntimeException("Unauthorized: Cannot update another specialist's profile"))
        else IO.unit
      existingOpt <- specialistService.getSpecialist(id)
      result <- existingOpt match
        case Right(existing) =>
          val updated = existing.copy(
            email = dto.email,
            name = dto.name,
            phone = dto.phone,
            bio = dto.bio,
            categoryRates = dto.categoryRates.map(toSpecialistCategoryRate),
            isAvailable = dto.isAvailable,
            updatedAt = Instant.now()
          )

          specialistService.updateSpecialist(updated).map {
            case Right(saved) => Right(toSpecialistDto(saved))
            case Left(error)  => Left(toErrorResponse(error))
          }
        case Left(error) => IO.pure(Left(toErrorResponse(error)))
    yield result).handleErrorWith { error =>
      IO.pure(Left(ErrorResponse("UNAUTHORIZED", error.getMessage)))
    }
  }

  // Delete specialist
  val deleteSpecialistEndpoint = ApiEndpoints
    .adminEndpoint("deleteSpecialist", "Delete specialist")
    .delete
    .in(path[UUID]("specialistId"))
    .in(header[Option[String]]("X-User-Role"))
    .out(stringBody)

  val deleteSpecialist = deleteSpecialistEndpoint.serverLogic { case (id, userRoleOpt) =>
    userRoleOpt match
      case Some(role) if role.equalsIgnoreCase("Admin") =>
        specialistService.deleteSpecialist(id).map {
          case Right(_)    => Right("Specialist deleted")
          case Left(error) => Left(toErrorResponse(error))
        }
      case _ =>
        IO.pure(Left(ErrorResponse("FORBIDDEN", "Admin role required")))
  }

  val endpoints = List(createSpecialist, searchSpecialists, getSpecialist, updateSpecialist, deleteSpecialist)

  val routes: HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(endpoints)
