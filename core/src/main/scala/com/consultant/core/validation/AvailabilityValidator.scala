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
package com.consultant.core.validation

import com.consultant.core.domain.{ CreateAvailabilityRequest, DomainError, SpecialistAvailability }
import Validator.*
import ValidationResult.*
import java.time.LocalTime

/** Validator for Availability-related requests. */
object AvailabilityValidator:

  /**
   * Validates a CreateAvailabilityRequest.
   *
   * @param request
   *   The request to validate
   * @return
   *   ValidationResult indicating success or failure
   */
  def validateCreate(request: CreateAvailabilityRequest): ValidationResult =
    all(
      // Validate day of week (0-6)
      inRange(request.dayOfWeek, 0, 6, "Day of week"),

      // Validate time range
      validateTimeRange(request.startTime, request.endTime)
    )

  /**
   * Validates an availability update.
   *
   * @param availability
   *   The availability to validate
   * @return
   *   ValidationResult indicating success or failure
   */
  def validateUpdate(availability: SpecialistAvailability): ValidationResult =
    all(
      // Validate day of week
      inRange(availability.dayOfWeek, 0, 6, "Day of week"),

      // Validate time range
      validateTimeRange(availability.startTime, availability.endTime)
    )

  /** Validates that start time is before end time. */
  def validateTimeRange(startTime: LocalTime, endTime: LocalTime): ValidationResult =
    check(
      startTime.isBefore(endTime),
      DomainError.ValidationError("Start time must be before end time")
    )

end AvailabilityValidator
