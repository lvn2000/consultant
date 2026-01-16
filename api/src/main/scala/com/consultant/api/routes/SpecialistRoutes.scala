package com.consultant.api.routes

import cats.effect.IO
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import com.consultant.api.dto.*
import com.consultant.core.service.SpecialistService
import com.consultant.api.DtoMappers.*
import java.util.UUID
import java.time.Instant
import sttp.tapir.server.http4s.Http4sServerInterpreter
import org.http4s.HttpRoutes

class SpecialistRoutes(specialistService: SpecialistService):

  private val baseEndpoint = endpoint

  // Create specialist
  val createSpecialistEndpoint = baseEndpoint.post
    .in(jsonBody[CreateSpecialistDto])
    .out(jsonBody[SpecialistDto])
    .errorOut(jsonBody[ErrorResponse])

  val createSpecialist = createSpecialistEndpoint.serverLogic { dto =>
    specialistService.createSpecialist(toCreateSpecialistRequest(dto)).map {
      case Right(specialist) => Right(toSpecialistDto(specialist))
      case Left(error)       => Left(toErrorResponse(error))
    }
  }

  // Get specialist by ID
  val getSpecialistEndpoint = baseEndpoint.get
    .in(path[UUID]("specialistId"))
    .out(jsonBody[SpecialistDto])
    .errorOut(jsonBody[ErrorResponse])

  val getSpecialist = getSpecialistEndpoint.serverLogic { id =>
    specialistService.getSpecialist(id).map {
      case Right(specialist) => Right(toSpecialistDto(specialist))
      case Left(error)       => Left(toErrorResponse(error))
    }
  }

  // Search specialists
  val searchSpecialistsEndpoint = baseEndpoint.get
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
  val updateSpecialistEndpoint = baseEndpoint.put
    .in(path[UUID]("specialistId"))
    .in(jsonBody[UpdateSpecialistDto])
    .out(jsonBody[SpecialistDto])
    .errorOut(jsonBody[ErrorResponse])

  val updateSpecialist = updateSpecialistEndpoint.serverLogic { case (id, dto) =>
    specialistService.getSpecialist(id).flatMap {
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
    }
  }

  // Delete specialist
  val deleteSpecialistEndpoint = baseEndpoint.delete
    .in(path[UUID]("specialistId"))
    .out(stringBody)
    .errorOut(jsonBody[ErrorResponse])

  val deleteSpecialist = deleteSpecialistEndpoint.serverLogic { id =>
    specialistService.deleteSpecialist(id).map {
      case Right(_)    => Right("Specialist deleted")
      case Left(error) => Left(toErrorResponse(error))
    }
  }

  val endpoints = List(createSpecialist, searchSpecialists, getSpecialist, updateSpecialist, deleteSpecialist)

  val routes: HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(endpoints)
