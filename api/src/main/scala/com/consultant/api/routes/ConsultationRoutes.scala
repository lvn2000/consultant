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

  private val baseEndpoint = endpoint

  // Create consultation
  val createConsultationEndpoint = baseEndpoint.post
    .in("consultations")
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
    .in("consultations" / path[UUID]("consultationId"))
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
    .in("consultations" / "user" / path[UUID]("userId"))
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
    .in("consultations" / "specialist" / path[UUID]("specialistId"))
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

  // Update consultation status
  val updateConsultationStatusEndpoint = baseEndpoint.put
    .in("consultations" / path[UUID]("consultationId") / "status")
    .in(jsonBody[UpdateConsultationStatusDto])
    .out(jsonBody[ConsultationDto])
    .errorOut(jsonBody[ErrorResponse])

  val updateConsultationStatus = updateConsultationStatusEndpoint.serverLogic { (id, dto) =>
    try
      val status = ConsultationStatus.valueOf(dto.status)
      for
        consultationOpt <- consultationService.getConsultation(id)
        result <- consultationOpt match
          case Right(consultation) =>
            consultationService.updateConsultationStatus(id, status).map {
              case Right(()) => Right(toConsultationDto(consultation.copy(status = status)))
              case Left(error) => Left(toErrorResponse(error))
            }
          case Left(error) => IO.pure(Left(toErrorResponse(error)))
      yield result
    catch
      case _: IllegalArgumentException => IO.pure(Left(ErrorResponse("VALIDATION_ERROR", "Invalid status")))
  }

  // Add review
  val addReviewEndpoint = baseEndpoint.post
    .in("consultations" / path[UUID]("consultationId") / "review")
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
    updateConsultationStatus,
    addReview
  )

  val routes: HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(endpoints)
