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

      // Calculate available slots by filtering out booked times
      localDate = java.time.LocalDate.parse(date)
      dayOfWeek = (localDate.getDayOfWeek.getValue + 6) % 7 // Convert to UI format (0=Mon, 6=Sun)
      slots = availabilitySlots
        .filter(avail => avail.dayOfWeek == dayOfWeek)
        .flatMap { avail =>
          val slotEnd = avail.startTime.plusMinutes(durationMinutes.toLong)
          Some((avail.startTime, slotEnd))
        }

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
      System.err.println(s"Error retrieving available slots: ${e.getMessage}")
      e.printStackTrace(System.err)
      Left(ErrorResponse("ERROR", "Failed to retrieve available slots. Please try again later."))
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

      // Check if time slot is available
      localDate = java.time.LocalDate.parse(request.date)
      dayOfWeek = (localDate.getDayOfWeek.getValue + 6) % 7 // Convert to UI format (0=Mon, 6=Sun)
      startTime = java.time.LocalTime.parse(request.startTime)
      slotEnd   = startTime.plusMinutes(request.durationMinutes.toLong)
      hasAvailabilitySlot = availabilitySlots.exists { avail =>
        avail.dayOfWeek == dayOfWeek &&
        !startTime.isBefore(avail.startTime) &&
        !slotEnd.isAfter(avail.endTime)
      }
      isAvailable = hasAvailabilitySlot

      response = Map("available" -> isAvailable)
    } yield Right(response)).recover { case e =>
      System.err.println(s"Error checking availability: ${e.getMessage}")
      e.printStackTrace(System.err)
      Left(ErrorResponse("ERROR", "Failed to check availability. Please try again later."))
    }
  }

  /**
   * Get specialist's availability preferences (all time slots for managing their schedule).
   *
   * GET /specialists/{specialistId}/availability
   */
  val getSpecialistAvailabilityEndpoint = specialistAvailabilityEndpoint.get
    .out(jsonBody[List[SpecialistAvailabilityDto]])
    .errorOut(jsonBody[ErrorResponse])

  val getSpecialistAvailability = getSpecialistAvailabilityEndpoint.serverLogic { specialistId =>
    availabilityRepository
      .findBySpecialist(specialistId)
      .map { availabilities =>
        Right(availabilities.map { avail =>
          SpecialistAvailabilityDto(
            id = avail.id,
            specialistId = avail.specialistId,
            dayOfWeek = avail.dayOfWeek,
            startTime = avail.startTime.toString,
            endTime = avail.endTime.toString,
            createdAt = avail.createdAt,
            updatedAt = avail.updatedAt
          )
        })
      }
      .recover { case e =>
        System.err.println(s"Error retrieving specialist availability: ${e.getMessage}")
        e.printStackTrace(System.err)
        Left(ErrorResponse("ERROR", "Failed to retrieve availability. Please try again later."))
      }
  }

  /**
   * Add a new availability slot for a specialist.
   *
   * POST /specialists/{specialistId}/availability { "dayOfWeek": 1, "startTime": "09:00", "endTime": "17:00" }
   */
  val createAvailabilityEndpoint = specialistAvailabilityEndpoint.post
    .in(header[Option[String]]("X-Auth-User-Id"))
    .in(header[Option[String]]("X-User-Role"))
    .in(jsonBody[CreateAvailabilityDto])
    .out(jsonBody[SpecialistAvailabilityDto])
    .errorOut(jsonBody[ErrorResponse])

  val createAvailability = createAvailabilityEndpoint.serverLogic {
    case (specialistId, authUserIdOpt, userRoleOpt, dto) =>
      (authUserIdOpt, userRoleOpt) match
        case (None, _) | (_, None) =>
          IO.pure(Left(ErrorResponse("UNAUTHORIZED", "Missing authentication headers")))
        case (Some(authUserId), Some(userRole)) =>
          // Authorization check: verify authenticated user owns this specialist ID or is admin
          val isOwner = specialistId.toString == authUserId
          val isAdmin = userRole.equalsIgnoreCase("Admin")

          if !isOwner && !isAdmin then
            IO.pure(
              Left(ErrorResponse("FORBIDDEN", "You can only create availability slots for your own specialist profile"))
            )
          else
            (for {
              availability <- availabilityRepository.create(
                specialistId,
                com.consultant.core.domain.CreateAvailabilityRequest(
                  dayOfWeek = dto.dayOfWeek,
                  startTime = java.time.LocalTime.parse(dto.startTime),
                  endTime = java.time.LocalTime.parse(dto.endTime)
                )
              )
              response = SpecialistAvailabilityDto(
                id = availability.id,
                specialistId = availability.specialistId,
                dayOfWeek = availability.dayOfWeek,
                startTime = availability.startTime.toString,
                endTime = availability.endTime.toString,
                createdAt = availability.createdAt,
                updatedAt = availability.updatedAt
              )
            } yield Right(response)).recover { case e =>
              System.err.println(s"Error creating availability slot: ${e.getMessage}")
              e.printStackTrace(System.err)
              Left(ErrorResponse("ERROR", "Failed to create availability slot. Please try again later."))
            }
  }

  /**
   * Update an availability slot.
   *
   * PUT /specialists/{specialistId}/availability/{slotId} { "dayOfWeek": 1, "startTime": "09:00", "endTime": "17:00" }
   */
  val updateAvailabilityEndpoint = specialistAvailabilityEndpoint.put
    .in(header[Option[String]]("X-Auth-User-Id"))
    .in(header[Option[String]]("X-User-Role"))
    .in(path[UUID]("slotId"))
    .in(jsonBody[UpdateAvailabilityDto])
    .out(jsonBody[SpecialistAvailabilityDto])
    .errorOut(jsonBody[ErrorResponse])

  val updateAvailability = updateAvailabilityEndpoint.serverLogic {
    case (specialistId, authUserIdOpt, userRoleOpt, slotId, dto) =>
      (authUserIdOpt, userRoleOpt) match
        case (None, _) | (_, None) =>
          IO.pure(Left(ErrorResponse("UNAUTHORIZED", "Missing authentication headers")))
        case (Some(authUserId), Some(userRole)) =>
          // Authorization check: verify authenticated user owns this specialist ID or is admin
          val isOwner = specialistId.toString == authUserId
          val isAdmin = userRole.equalsIgnoreCase("Admin")

          if !isOwner && !isAdmin then
            IO.pure(
              Left(ErrorResponse("FORBIDDEN", "You can only update availability slots for your own specialist profile"))
            )
          else
            (for {
              existing <- availabilityRepository.findById(slotId).map {
                case Some(avail) => Right(avail)
                case None        => Left(ErrorResponse("NOT_FOUND", "Availability slot not found"))
              }
              result <- existing match {
                case Right(avail) =>
                  // Additional check: verify the slot belongs to the requested specialist
                  if avail.specialistId != specialistId then
                    IO.pure(Left(ErrorResponse("FORBIDDEN", "You can only update your own availability slots")))
                  else
                    val updated = avail.copy(
                      dayOfWeek = dto.dayOfWeek,
                      startTime = java.time.LocalTime.parse(dto.startTime),
                      endTime = java.time.LocalTime.parse(dto.endTime),
                      updatedAt = java.time.Instant.now()
                    )
                    availabilityRepository.update(updated).map { updated =>
                      Right(
                        SpecialistAvailabilityDto(
                          id = updated.id,
                          specialistId = updated.specialistId,
                          dayOfWeek = updated.dayOfWeek,
                          startTime = updated.startTime.toString,
                          endTime = updated.endTime.toString,
                          createdAt = updated.createdAt,
                          updatedAt = updated.updatedAt
                        )
                      )
                    }
                case Left(error) => IO.pure(Left(error))
              }
            } yield result).recover { case e =>
              System.err.println(s"Error updating availability slot: ${e.getMessage}")
              e.printStackTrace(System.err)
              Left(ErrorResponse("ERROR", "Failed to update availability slot. Please try again later."))
            }
  }

  /**
   * Delete an availability slot.
   *
   * DELETE /specialists/{specialistId}/availability/{slotId}
   */
  val deleteAvailabilityEndpoint = specialistAvailabilityEndpoint.delete
    .in(header[Option[String]]("X-Auth-User-Id"))
    .in(header[Option[String]]("X-User-Role"))
    .in(path[UUID]("slotId"))
    .out(jsonBody[Map[String, String]])
    .errorOut(jsonBody[ErrorResponse])

  val deleteAvailability = deleteAvailabilityEndpoint.serverLogic {
    case (specialistId, authUserIdOpt, userRoleOpt, slotId) =>
      (authUserIdOpt, userRoleOpt) match
        case (None, _) | (_, None) =>
          IO.pure(Left(ErrorResponse("UNAUTHORIZED", "Missing authentication headers")))
        case (Some(authUserId), Some(userRole)) =>
          // Authorization check: verify authenticated user owns this specialist ID or is admin
          val isOwner = specialistId.toString == authUserId
          val isAdmin = userRole.equalsIgnoreCase("Admin")

          if !isOwner && !isAdmin then
            IO.pure(
              Left(ErrorResponse("FORBIDDEN", "You can only delete availability slots for your own specialist profile"))
            )
          else
            (for {
              existing <- availabilityRepository.findById(slotId).map {
                case Some(avail) if avail.specialistId == specialistId => Right(avail)
                case Some(_) => Left(ErrorResponse("FORBIDDEN", "You are not the owner of this slot"))
                case None    => Left(ErrorResponse("NOT_FOUND", "Availability slot not found"))
              }
              result <- existing match {
                case Right(_) =>
                  availabilityRepository.delete(slotId).map { _ =>
                    Right(Map("message" -> "Availability slot deleted successfully"))
                  }
                case Left(error) => IO.pure(Left(error))
              }
            } yield result).recover { case e =>
              System.err.println(s"Error deleting availability slot: ${e.getMessage}")
              e.printStackTrace(System.err)
              Left(ErrorResponse("ERROR", "Failed to delete availability slot. Please try again later."))
            }
  }

  val endpoints = List(
    getAvailableSlots,
    checkAvailability,
    getSpecialistAvailability,
    createAvailability,
    updateAvailability,
    deleteAvailability
  )

  val routes: HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(endpoints)
