package com.consultant.api.routes

import cats.effect.IO
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import com.consultant.api.dto.*
import com.consultant.core.service.ConsultationService
import com.consultant.core.service.AvailabilityService
import com.consultant.core.ports.AvailabilityRepository
import com.consultant.api.DtoMappers.*
import java.util.UUID
import java.time.LocalDate
import sttp.tapir.server.http4s.Http4sServerInterpreter
import org.http4s.HttpRoutes

class AvailabilitySlotRoutes(
  consultationService: ConsultationService,
  availabilityRepository: AvailabilityRepository
):

  private val baseEndpoint = endpoint
  private val specialistAvailabilityEndpoint = endpoint
    .in(path[UUID]("specialistId"))
    .in("availability")

  /**
   * Get available time slots for a specialist on a specific date. Takes into account existing consultations.
   *
   * GET /specialists/{specialistId}/availability/slots?date=YYYY-MM-DD&durationMinutes=60
   */
  val getAvailableSlotsEndpoint = specialistAvailabilityEndpoint.get
    .in("slots")
    .in(query[String]("date")) // YYYY-MM-DD format
    .in(query[Int]("durationMinutes").default(60))
    .out(jsonBody[AvailableSlotsResponse])
    .errorOut(jsonBody[ErrorResponse])

  val getAvailableSlots = getAvailableSlotsEndpoint.serverLogic { case (specialistId, date, durationMinutes) =>
    (for {
      // Get specialist's availability slots
      availabilitySlots <- availabilityRepository.findBySpecialist(specialistId)

      // Get specialist's existing consultations
      consultations <- consultationService.getSpecialistConsultations(specialistId, 0, 1000)

      // Create service and calculate available slots
      localDate = java.time.LocalDate.parse(date)
      service   = AvailabilityService(availabilitySlots, consultations)
      slots     = service.getAvailableSlotsForDate(localDate, durationMinutes)

      // Convert to response DTOs
      slotDtos = slots.map { case (start, end) =>
        val durationMin = java.time.temporal.ChronoUnit.MINUTES.between(start, end).toInt
        AvailableSlotDto(
          startTime = start.toString,
          endTime = end.toString,
          durationMinutes = durationMin
        )
      }

      response = AvailableSlotsResponse(
        specialistId = specialistId.toString,
        date = date,
        slots = slotDtos,
        hasAvailableSlots = slotDtos.nonEmpty
      )
    } yield Right(response)).recover { case e =>
      Left(ErrorResponse("ERROR", e.getMessage))
    }
  }

  /**
   * Check if a specific time slot is available.
   *
   * POST /specialists/{specialistId}/availability/check { "date": "2026-02-10", "startTime": "14:00",
   * "durationMinutes": 60 }
   */
  val checkAvailabilityEndpoint = specialistAvailabilityEndpoint.post
    .in("check")
    .in(jsonBody[CheckAvailabilityRequest])
    .out(jsonBody[Map[String, Boolean]])
    .errorOut(jsonBody[ErrorResponse])

  val checkAvailability = checkAvailabilityEndpoint.serverLogic { case (specialistId, request) =>
    (for {
      // Get specialist's availability slots
      availabilitySlots <- availabilityRepository.findBySpecialist(specialistId)

      // Get specialist's existing consultations
      consultations <- consultationService.getSpecialistConsultations(specialistId, 0, 1000)

      // Create service and check availability
      localDate   = java.time.LocalDate.parse(request.date)
      startTime   = java.time.LocalTime.parse(request.startTime)
      service     = AvailabilityService(availabilitySlots, consultations)
      isAvailable = service.isTimeSlotAvailable(localDate, startTime, request.durationMinutes)

      response = Map("available" -> isAvailable)
    } yield Right(response)).recover { case e =>
      Left(ErrorResponse("ERROR", e.getMessage))
    }
  }

  val endpoints = List(getAvailableSlots, checkAvailability)

  val routes: HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(endpoints)
