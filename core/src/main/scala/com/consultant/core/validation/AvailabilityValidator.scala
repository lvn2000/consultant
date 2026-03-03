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
