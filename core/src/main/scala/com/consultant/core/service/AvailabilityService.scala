package com.consultant.core.service

import cats.effect.IO
import cats.syntax.all.*
import com.consultant.core.domain.*
import com.consultant.core.ports.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

class AvailabilityService(
  availabilityRepo: AvailabilityRepository,
  consultationRepo: ConsultationRepository
):

  def createAvailability(
    specialistId: SpecialistId,
    request: CreateAvailabilityRequest
  ): IO[Either[DomainError, SpecialistAvailability]] =
    validateTimeRange(request.startTime, request.endTime) match
      case Left(error) => IO.pure(Left(error))
      case Right(_) =>
        availabilityRepo
          .create(specialistId, request)
          .map(Right(_))
          .handleError { error =>
            Left(DomainError.DatabaseError(s"Failed to create availability: ${error.getMessage}"))
          }

  def getAvailability(id: UUID): IO[Either[DomainError, SpecialistAvailability]] =
    availabilityRepo.findById(id).map {
      case Some(availability) => Right(availability)
      case None               => Left(DomainError.ValidationError(s"Availability not found: $id"))
    }

  def getSpecialistAvailability(specialistId: SpecialistId): IO[List[SpecialistAvailability]] =
    availabilityRepo.findBySpecialist(specialistId)

  def getAvailabilityForDay(
    specialistId: SpecialistId,
    dayOfWeek: Int
  ): IO[Either[DomainError, List[SpecialistAvailability]]] =
    if dayOfWeek < 0 || dayOfWeek > 6 then
      IO.pure(Left(DomainError.ValidationError(s"Invalid day of week: $dayOfWeek. Must be 0-6")))
    else availabilityRepo.findBySpecialistAndDay(specialistId, dayOfWeek).map(Right(_))

  def updateAvailability(
    id: UUID,
    availability: SpecialistAvailability
  ): IO[Either[DomainError, SpecialistAvailability]] =
    validateTimeRange(availability.startTime, availability.endTime) match
      case Left(error) => IO.pure(Left(error))
      case Right(_) =>
        availabilityRepo
          .update(availability)
          .map(Right(_))
          .handleError { error =>
            Left(DomainError.DatabaseError(s"Failed to update availability: ${error.getMessage}"))
          }

  def deleteAvailability(id: UUID): IO[Either[DomainError, Unit]] =
    availabilityRepo
      .delete(id)
      .map(Right(_))
      .handleError { error =>
        Left(DomainError.DatabaseError(s"Failed to delete availability: ${error.getMessage}"))
      }

  def deleteSpecialistAvailability(specialistId: SpecialistId): IO[Either[DomainError, Unit]] =
    availabilityRepo
      .deleteBySpecialist(specialistId)
      .map(Right(_))
      .handleError { error =>
        Left(DomainError.DatabaseError(s"Failed to delete specialist availability: ${error.getMessage}"))
      }

  // Check if a specific time slot is available (considering existing consultations)
  def isTimeSlotAvailable(
    specialistId: SpecialistId,
    date: LocalDate,
    startTime: LocalTime,
    durationMinutes: Int
  ): IO[Either[DomainError, Boolean]] =
    for
      dayOfWeek            <- IO.pure((date.getDayOfWeek.getValue + 6) % 7) // Convert to UI format (0=Mon, 6=Sun)
      availabilitiesResult <- getAvailabilityForDay(specialistId, dayOfWeek)
      available <- availabilitiesResult match
        case Right(availabilities) =>
          if availabilities.isEmpty then
            IO.pure(Left(DomainError.ValidationError("Specialist has no availability for this day")))
          else
            val endTime = startTime.plusMinutes(durationMinutes.toLong)
            val isWithinAnySlot = availabilities.exists { av =>
              !startTime.isBefore(av.startTime) && !endTime.isAfter(av.endTime)
            }
            if !isWithinAnySlot then IO.pure(Right(false))
            else
              // Check if slot overlaps with any existing consultation
              consultationRepo.findBySpecialist(specialistId, 0, Int.MaxValue).map { consultations =>
                val hasConflict = consultations.exists { consultation =>
                  val consultDateTime = consultation.scheduledAt
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime
                  val consultDate      = consultDateTime.toLocalDate
                  val consultStartTime = consultDateTime.toLocalTime
                  val consultEndTime = consultStartTime.plusMinutes(
                    consultation.duration.getOrElse(60).toLong
                  )

                  date == consultDate &&
                  !(endTime.isBefore(consultStartTime) || startTime.isAfter(consultEndTime))
                }
                Right(!hasConflict)
              }
        case Left(error) => IO.pure(Left(error))
    yield available

  // Get available time slots for a specific date
  def getAvailableSlots(
    specialistId: SpecialistId,
    date: LocalDate,
    slotDurationMinutes: Int = 60
  ): IO[Either[DomainError, List[(LocalTime, LocalTime)]]] =
    for
      dayOfWeek            <- IO.pure((date.getDayOfWeek.getValue + 6) % 7) // Convert to UI format (0=Mon, 6=Sun)
      availabilitiesResult <- getAvailabilityForDay(specialistId, dayOfWeek)
      slots <- availabilitiesResult match
        case Right(availabilities) =>
          if availabilities.isEmpty then IO.pure(Right(List()))
          else
            consultationRepo.findBySpecialist(specialistId, 0, Int.MaxValue).map { consultations =>
              val availableSlots = availabilities.flatMap { av =>
                calculateFreeSlots(av.startTime, av.endTime, consultations, date, slotDurationMinutes)
              }
              Right(availableSlots)
            }
        case Left(error) => IO.pure(Left(error))
    yield slots

  private def calculateFreeSlots(
    dayStart: LocalTime,
    dayEnd: LocalTime,
    consultations: List[Consultation],
    date: LocalDate,
    slotDurationMinutes: Int
  ): List[(LocalTime, LocalTime)] =
    // Filter consultations for this date and convert to time ranges
    val bookedSlots = consultations.filter { c =>
      val consultDateTime = c.scheduledAt.atZone(ZoneId.systemDefault()).toLocalDateTime
      consultDateTime.toLocalDate == date
    }.map { c =>
      val consultDateTime = c.scheduledAt.atZone(ZoneId.systemDefault()).toLocalDateTime
      val startTime       = consultDateTime.toLocalTime
      val endTime         = startTime.plusMinutes(c.duration.getOrElse(60).toLong)
      (startTime, endTime)
    }
      .sortBy(_._1)

    // Find gaps between booked slots
    if bookedSlots.isEmpty then
      // No consultations, entire window is available
      if dayEnd.minusMinutes(slotDurationMinutes.toLong).isAfter(dayStart) then List((dayStart, dayEnd))
      else List()
    else
      val gaps = scala.collection.mutable.ListBuffer[(LocalTime, LocalTime)]()

      // Check gap before first consultation
      val firstBookedStart = bookedSlots.head._1
      if dayStart.plusMinutes(slotDurationMinutes.toLong).isBefore(firstBookedStart) then
        gaps += ((dayStart, firstBookedStart))

      // Check gaps between consultations
      for i <- 0 until bookedSlots.length - 1 do
        val currentEnd = bookedSlots(i)._2
        val nextStart  = bookedSlots(i + 1)._1
        if currentEnd.plusMinutes(slotDurationMinutes.toLong).isBefore(nextStart) then gaps += ((currentEnd, nextStart))

      // Check gap after last consultation
      val lastBookedEnd = bookedSlots.last._2
      if lastBookedEnd.plusMinutes(slotDurationMinutes.toLong).isBefore(dayEnd) then gaps += ((lastBookedEnd, dayEnd))

      gaps.toList

  private def validateTimeRange(startTime: LocalTime, endTime: LocalTime): Either[DomainError, Unit] =
    if startTime.isAfter(endTime) || startTime.equals(endTime) then
      Left(DomainError.ValidationError("Start time must be before end time"))
    else Right(())
