package com.consultant.api.dto

import java.util.UUID
import java.time.Instant
import java.time.LocalTime
import io.circe.{ Decoder, Encoder }
import io.circe.Codec

// Specialist Availability DTOs
case class SpecialistAvailabilityDto(
  id: UUID,
  specialistId: UUID,
  dayOfWeek: Int,
  startTime: String, // HH:mm format
  endTime: String,   // HH:mm format
  createdAt: Instant,
  updatedAt: Instant
) derives Codec.AsObject

case class CreateAvailabilityDto(
  dayOfWeek: Int,
  startTime: String, // HH:mm format
  endTime: String    // HH:mm format
) derives Codec.AsObject

case class UpdateAvailabilityDto(
  dayOfWeek: Int,
  startTime: String, // HH:mm format
  endTime: String    // HH:mm format
) derives Codec.AsObject
