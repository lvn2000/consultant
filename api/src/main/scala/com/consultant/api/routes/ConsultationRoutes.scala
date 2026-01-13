package com.consultant.api.routes

import cats.effect.IO
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import com.consultant.api.dto.*
import com.consultant.core.service.ConsultationService
import com.consultant.core.domain.ConsultationStatus
import com.consultant.api.DtoMappers.*
import java.util.UUID
import sttp.tapir.server.http4s.Http4sServerInterpreter
import org.http4s.HttpRoutes

class ConsultationRoutes(consultationService: ConsultationService):

  private val baseEndpoint = endpoint.in("api" / "consultations")

  // Create consultation
  val createConsultationEndpoint = baseEndpoint.post
    .in(jsonBody[CreateConsultationDto])
    .out(jsonBody[ConsultationDto])
    .errorOut(jsonBody[ErrorResponse])

  val createConsultation = createConsultationEndpoint.serverLogic { dto =>
    consultationService.createConsultation(toCreateConsultationRequest(dto)).map {
      case Right(consultation) => Right(toConsultationDto(consultation))
      case Left(error)         => Left(toErrorResponse(error))
    }
  }

  // Get consultation by ID
  val getConsultationEndpoint = baseEndpoint.get
    .in(path[UUID]("consultationId"))
    .out(jsonBody[ConsultationDto])
    .errorOut(jsonBody[ErrorResponse])

  val getConsultation = getConsultationEndpoint.serverLogic { id =>
    consultationService.getConsultation(id).map {
      case Right(consultation) => Right(toConsultationDto(consultation))
      case Left(error)         => Left(toErrorResponse(error))
    }
  }

  // Get user consultations
  val getUserConsultationsEndpoint = baseEndpoint.get
    .in("user" / path[UUID]("userId"))
    .in(query[Option[Int]]("offset").default(Some(0)))
    .in(query[Option[Int]]("limit").default(Some(20)))
    .out(jsonBody[List[ConsultationDto]])

  val getUserConsultations = getUserConsultationsEndpoint.serverLogic { case (userId, offset, limit) =>
    consultationService
      .getUserConsultations(userId, offset.getOrElse(0), limit.getOrElse(20))
      .map(consultations => Right(consultations.map(toConsultationDto)))
  }

  // Get specialist consultations
  val getSpecialistConsultationsEndpoint = baseEndpoint.get
    .in("specialist" / path[UUID]("specialistId"))
    .in(query[Option[Int]]("offset").default(Some(0)))
    .in(query[Option[Int]]("limit").default(Some(20)))
    .out(jsonBody[List[ConsultationDto]])

  val getSpecialistConsultations = getSpecialistConsultationsEndpoint.serverLogic {
    case (specialistId, offset, limit) =>
      consultationService
        .getSpecialistConsultations(
          specialistId,
          offset.getOrElse(0),
          limit.getOrElse(20)
        )
        .map(consultations => Right(consultations.map(toConsultationDto)))
  }

  // Add review
  val addReviewEndpoint = baseEndpoint.post
    .in(path[UUID]("consultationId") / "review")
    .in(jsonBody[AddReviewDto])
    .out(jsonBody[String])
    .errorOut(jsonBody[ErrorResponse])

  val addReview = addReviewEndpoint.serverLogic { case (id, dto) =>
    consultationService.addReview(id, dto.rating, dto.review).map {
      case Right(_)    => Right("Review added successfully")
      case Left(error) => Left(toErrorResponse(error))
    }
  }

  val endpoints = List(
    createConsultation,
    getConsultation,
    getUserConsultations,
    getSpecialistConsultations,
    addReview
  )

  val routes: HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(endpoints)
