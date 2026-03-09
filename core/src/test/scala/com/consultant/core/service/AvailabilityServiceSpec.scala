/*
 * Copyright (c) 2026 Volodymyr Lubenchenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.consultant.core.service

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalamock.scalatest.MockFactory
import cats.effect.IO
import com.consultant.core.domain._
import com.consultant.core.ports._
import cats.effect.unsafe.implicits.global
import java.time.Instant
import java.time.LocalTime
import java.time.LocalDate

class AvailabilityServiceSpec extends AnyFlatSpec with Matchers with MockFactory {

  private def createAvailability(
    id: java.util.UUID,
    specialistId: java.util.UUID,
    dayOfWeek: Int,
    startTime: LocalTime,
    endTime: LocalTime
  ): SpecialistAvailability =
    SpecialistAvailability(
      id = id,
      specialistId = specialistId,
      dayOfWeek = dayOfWeek,
      startTime = startTime,
      endTime = endTime,
      createdAt = Instant.now(),
      updatedAt = Instant.now()
    )

  private def createConsultation(
    id: java.util.UUID,
    specialistId: java.util.UUID,
    scheduledAt: Instant,
    duration: Option[Int] = Some(60)
  ): Consultation =
    Consultation(
      id = id,
      userId = java.util.UUID.randomUUID(),
      specialistId = specialistId,
      categoryId = java.util.UUID.randomUUID(),
      description = "Test consultation",
      status = ConsultationStatus.Scheduled,
      scheduledAt = scheduledAt,
      duration = duration,
      price = BigDecimal("50.00"),
      rating = None,
      review = None,
      createdAt = Instant.now(),
      updatedAt = Instant.now()
    )

  "createAvailability" should "successfully create availability with valid time range" in {
    val availabilityRepo = mock[AvailabilityRepository]
    val consultationRepo = mock[ConsultationRepository]
    val service          = new AvailabilityService(availabilityRepo, consultationRepo)
    val specialistId     = java.util.UUID.randomUUID()
    val availabilityId   = java.util.UUID.randomUUID()
    val request = CreateAvailabilityRequest(
      dayOfWeek = 0,
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(17, 0)
    )
    val createdAvailability =
      createAvailability(availabilityId, specialistId, request.dayOfWeek, request.startTime, request.endTime)

    availabilityRepo.create.expects(specialistId, request).returning(IO.pure(createdAvailability))

    val result = service.createAvailability(specialistId, request).unsafeRunSync()

    result.shouldBe(Right(createdAvailability))
  }

  it should "return error if start time is after end time" in {
    val availabilityRepo = mock[AvailabilityRepository]
    val consultationRepo = mock[ConsultationRepository]
    val service          = new AvailabilityService(availabilityRepo, consultationRepo)
    val specialistId     = java.util.UUID.randomUUID()
    val request = CreateAvailabilityRequest(
      dayOfWeek = 0,
      startTime = LocalTime.of(17, 0),
      endTime = LocalTime.of(9, 0)
    )

    val result = service.createAvailability(specialistId, request).unsafeRunSync()

    result.isLeft shouldBe true
    result.left.toOption.get match
      case DomainError.ValidationError(msg) => msg.contains("Start time must be before end time") shouldBe true
      case _                                => fail("Expected ValidationError")
  }

  "getAvailability" should "return availability if found" in {
    val availabilityRepo = mock[AvailabilityRepository]
    val consultationRepo = mock[ConsultationRepository]
    val service          = new AvailabilityService(availabilityRepo, consultationRepo)
    val availabilityId   = java.util.UUID.randomUUID()
    val availability = createAvailability(
      availabilityId,
      java.util.UUID.randomUUID(),
      0,
      LocalTime.of(9, 0),
      LocalTime.of(17, 0)
    )

    availabilityRepo.findById.expects(availabilityId).returning(IO.pure(Some(availability)))

    val result = service.getAvailability(availabilityId).unsafeRunSync()

    result.shouldBe(Right(availability))
  }

  it should "return error if availability not found" in {
    val availabilityRepo = mock[AvailabilityRepository]
    val consultationRepo = mock[ConsultationRepository]
    val service          = new AvailabilityService(availabilityRepo, consultationRepo)
    val availabilityId   = java.util.UUID.randomUUID()

    availabilityRepo.findById.expects(availabilityId).returning(IO.pure(None))

    val result = service.getAvailability(availabilityId).unsafeRunSync()

    result.isLeft shouldBe true
  }

  "getSpecialistAvailability" should "return all availability slots for specialist" in {
    val availabilityRepo = mock[AvailabilityRepository]
    val consultationRepo = mock[ConsultationRepository]
    val service          = new AvailabilityService(availabilityRepo, consultationRepo)
    val specialistId     = java.util.UUID.randomUUID()
    val availabilities = List(
      createAvailability(java.util.UUID.randomUUID(), specialistId, 0, LocalTime.of(9, 0), LocalTime.of(17, 0)),
      createAvailability(java.util.UUID.randomUUID(), specialistId, 1, LocalTime.of(9, 0), LocalTime.of(17, 0))
    )

    availabilityRepo.findBySpecialist.expects(specialistId).returning(IO.pure(availabilities))

    val result = service.getSpecialistAvailability(specialistId).unsafeRunSync()

    result.shouldBe(availabilities)
  }

  "getAvailabilityForDay" should "return availability for specific day of week" in {
    val availabilityRepo = mock[AvailabilityRepository]
    val consultationRepo = mock[ConsultationRepository]
    val service          = new AvailabilityService(availabilityRepo, consultationRepo)
    val specialistId     = java.util.UUID.randomUUID()
    val availability = createAvailability(
      java.util.UUID.randomUUID(),
      specialistId,
      0,
      LocalTime.of(9, 0),
      LocalTime.of(17, 0)
    )

    availabilityRepo.findBySpecialistAndDay
      .expects(specialistId, 0)
      .returning(IO.pure(List(availability)))

    val result = service.getAvailabilityForDay(specialistId, 0).unsafeRunSync()

    result.shouldBe(Right(List(availability)))
  }

  it should "return error for invalid day of week" in {
    val availabilityRepo = mock[AvailabilityRepository]
    val consultationRepo = mock[ConsultationRepository]
    val service          = new AvailabilityService(availabilityRepo, consultationRepo)
    val specialistId     = java.util.UUID.randomUUID()

    val result = service.getAvailabilityForDay(specialistId, 7).unsafeRunSync()

    result.isLeft shouldBe true
    result.left.toOption.get match
      case DomainError.ValidationError(msg) => msg.contains("Invalid day of week") shouldBe true
      case _                                => fail("Expected ValidationError")
  }

  "isTimeSlotAvailable" should "return true when slot is available" in {
    val availabilityRepo = mock[AvailabilityRepository]
    val consultationRepo = mock[ConsultationRepository]
    val service          = new AvailabilityService(availabilityRepo, consultationRepo)
    val specialistId     = java.util.UUID.randomUUID()
    val date             = LocalDate.of(2026, 2, 9) // Monday
    val dayOfWeek        = (date.getDayOfWeek.getValue + 6) % 7 // Convert to UI format (0=Mon, 6=Sun)
    val availability = createAvailability(
      java.util.UUID.randomUUID(),
      specialistId,
      dayOfWeek,
      LocalTime.of(9, 0),
      LocalTime.of(17, 0)
    )

    availabilityRepo.findBySpecialistAndDay
      .expects(specialistId, dayOfWeek)
      .returning(IO.pure(List(availability)))
    consultationRepo.findBySpecialist.expects(specialistId, 0, Int.MaxValue).returning(IO.pure(List()))

    val result = service
      .isTimeSlotAvailable(specialistId, date, LocalTime.of(10, 0), 60)
      .unsafeRunSync()

    result.shouldBe(Right(true))
  }

  it should "return false when slot overlaps with existing consultation" in {
    val availabilityRepo = mock[AvailabilityRepository]
    val consultationRepo = mock[ConsultationRepository]
    val service          = new AvailabilityService(availabilityRepo, consultationRepo)
    val specialistId     = java.util.UUID.randomUUID()
    val date             = LocalDate.of(2026, 2, 9) // Monday
    val dayOfWeek        = (date.getDayOfWeek.getValue + 6) % 7 // Convert to UI format (0=Mon, 6=Sun)
    val availability = createAvailability(
      java.util.UUID.randomUUID(),
      specialistId,
      dayOfWeek,
      LocalTime.of(9, 0),
      LocalTime.of(17, 0)
    )

    // Create consultation at 10:00-11:00
    val consultation = createConsultation(
      java.util.UUID.randomUUID(),
      specialistId,
      date.atTime(10, 0).atZone(java.time.ZoneId.systemDefault()).toInstant,
      Some(60)
    )

    availabilityRepo.findBySpecialistAndDay
      .expects(specialistId, dayOfWeek)
      .returning(IO.pure(List(availability)))
    consultationRepo.findBySpecialist
      .expects(specialistId, 0, Int.MaxValue)
      .returning(IO.pure(List(consultation)))

    val result = service
      .isTimeSlotAvailable(specialistId, date, LocalTime.of(10, 30), 60)
      .unsafeRunSync()

    result.shouldBe(Right(false))
  }

  "getAvailableSlots" should "return all available slots for a date" in {
    val availabilityRepo = mock[AvailabilityRepository]
    val consultationRepo = mock[ConsultationRepository]
    val service          = new AvailabilityService(availabilityRepo, consultationRepo)
    val specialistId     = java.util.UUID.randomUUID()
    val date             = LocalDate.of(2026, 2, 9) // Monday
    val dayOfWeek        = (date.getDayOfWeek.getValue + 6) % 7 // Convert to UI format (0=Mon, 6=Sun)
    val availability = createAvailability(
      java.util.UUID.randomUUID(),
      specialistId,
      dayOfWeek,
      LocalTime.of(9, 0),
      LocalTime.of(17, 0)
    )

    availabilityRepo.findBySpecialistAndDay
      .expects(specialistId, dayOfWeek)
      .returning(IO.pure(List(availability)))
    consultationRepo.findBySpecialist.expects(specialistId, 0, Int.MaxValue).returning(IO.pure(List()))

    val result = service.getAvailableSlots(specialistId, date, 60).unsafeRunSync()

    result.isRight shouldBe true
    val slots = result.toOption.get
    slots.length should be > 0
    slots.head._1.shouldBe(LocalTime.of(9, 0))
    slots.head._2.shouldBe(LocalTime.of(17, 0))
  }

  it should "return empty list when no availability for the day" in {
    val availabilityRepo = mock[AvailabilityRepository]
    val consultationRepo = mock[ConsultationRepository]
    val service          = new AvailabilityService(availabilityRepo, consultationRepo)
    val specialistId     = java.util.UUID.randomUUID()
    val date             = LocalDate.of(2026, 2, 9) // Monday
    val dayOfWeek        = (date.getDayOfWeek.getValue + 6) % 7 // Convert to UI format (0=Mon, 6=Sun)

    availabilityRepo.findBySpecialistAndDay
      .expects(specialistId, dayOfWeek)
      .returning(IO.pure(List()))

    val result = service.getAvailableSlots(specialistId, date, 60).unsafeRunSync()

    result.shouldBe(Right(List()))
  }

  it should "exclude booked time from available slots" in {
    val availabilityRepo = mock[AvailabilityRepository]
    val consultationRepo = mock[ConsultationRepository]
    val service          = new AvailabilityService(availabilityRepo, consultationRepo)
    val specialistId     = java.util.UUID.randomUUID()
    val date             = LocalDate.of(2026, 2, 9) // Monday
    val dayOfWeek        = (date.getDayOfWeek.getValue + 6) % 7 // Convert to UI format (0=Mon, 6=Sun)
    val availability = createAvailability(
      java.util.UUID.randomUUID(),
      specialistId,
      dayOfWeek,
      LocalTime.of(9, 0),
      LocalTime.of(17, 0)
    )

    // Create consultation at 12:00-13:00
    val consultation = createConsultation(
      java.util.UUID.randomUUID(),
      specialistId,
      date.atTime(12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant,
      Some(60)
    )

    availabilityRepo.findBySpecialistAndDay
      .expects(specialistId, dayOfWeek)
      .returning(IO.pure(List(availability)))
    consultationRepo.findBySpecialist
      .expects(specialistId, 0, Int.MaxValue)
      .returning(IO.pure(List(consultation)))

    val result = service.getAvailableSlots(specialistId, date, 60).unsafeRunSync()

    result.isRight shouldBe true
    val slots = result.toOption.get
    slots.length shouldBe 2                   // Before and after the consultation
    slots(0)._2.shouldBe(LocalTime.of(12, 0)) // First slot ends when consultation starts
    slots(1)._1.shouldBe(LocalTime.of(13, 0)) // Second slot starts when consultation ends
  }

  "updateAvailability" should "successfully update availability" in {
    val availabilityRepo = mock[AvailabilityRepository]
    val consultationRepo = mock[ConsultationRepository]
    val service          = new AvailabilityService(availabilityRepo, consultationRepo)
    val availabilityId   = java.util.UUID.randomUUID()
    val availability = createAvailability(
      availabilityId,
      java.util.UUID.randomUUID(),
      0,
      LocalTime.of(9, 0),
      LocalTime.of(17, 0)
    )
    val updated = availability.copy(startTime = LocalTime.of(8, 0))

    availabilityRepo.update.expects(updated).returning(IO.pure(updated))

    val result = service.updateAvailability(availabilityId, updated).unsafeRunSync()

    result.shouldBe(Right(updated))
  }

  "deleteAvailability" should "successfully delete availability" in {
    val availabilityRepo = mock[AvailabilityRepository]
    val consultationRepo = mock[ConsultationRepository]
    val service          = new AvailabilityService(availabilityRepo, consultationRepo)
    val availabilityId   = java.util.UUID.randomUUID()

    availabilityRepo.delete.expects(availabilityId).returning(IO.unit)

    val result = service.deleteAvailability(availabilityId).unsafeRunSync()

    result.shouldBe(Right(()))
  }

  "deleteSpecialistAvailability" should "successfully delete all availability for specialist" in {
    val availabilityRepo = mock[AvailabilityRepository]
    val consultationRepo = mock[ConsultationRepository]
    val service          = new AvailabilityService(availabilityRepo, consultationRepo)
    val specialistId     = java.util.UUID.randomUUID()

    availabilityRepo.deleteBySpecialist.expects(specialistId).returning(IO.unit)

    val result = service.deleteSpecialistAvailability(specialistId).unsafeRunSync()

    result.shouldBe(Right(()))
  }
}
