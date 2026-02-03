package com.consultant.api.dto

import io.circe.Codec

/**
 * Response DTO for available time slots.
 */
case class AvailableSlotDto(
  startTime: String,   // HH:mm format
  endTime: String,     // HH:mm format
  durationMinutes: Int // Duration of the available window
) derives Codec.AsObject

/**
 * Request DTO to check availability.
 */
case class CheckAvailabilityRequest(
  specialistId: String,
  date: String,        // YYYY-MM-DD format
  startTime: String,   // HH:mm format (will be parsed to LocalTime)
  durationMinutes: Int // How long the consultation will be
) derives Codec.AsObject

/**
 * Response with available slots for a date.
 */
case class AvailableSlotsResponse(
  specialistId: String,
  date: String,
  slots: List[AvailableSlotDto],
  hasAvailableSlots: Boolean
) derives Codec.AsObject
