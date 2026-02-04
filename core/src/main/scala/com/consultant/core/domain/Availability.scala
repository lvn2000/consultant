package com.consultant.core.domain

import java.time.Instant
import java.time.LocalTime
import java.util.UUID
import types.*

// Specialist availability (time slots when specialist is available)
case class SpecialistAvailability(
  id: UUID,
  specialistId: SpecialistId,
  dayOfWeek: Int, // 0=Monday, 6=Sunday
  startTime: LocalTime,
  endTime: LocalTime,
  createdAt: Instant,
  updatedAt: Instant
)

case class CreateAvailabilityRequest(
  dayOfWeek: Int, // 0=Monday, 6=Sunday
  startTime: LocalTime,
  endTime: LocalTime
)

case class UpdateAvailabilityRequest(
  dayOfWeek: Int,
  startTime: LocalTime,
  endTime: LocalTime
)
