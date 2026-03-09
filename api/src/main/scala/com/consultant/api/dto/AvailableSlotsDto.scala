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
