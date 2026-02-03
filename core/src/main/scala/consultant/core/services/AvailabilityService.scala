package com.consultant.core.service

import cats.effect.IO
import com.consultant.core.domain.SpecialistAvailability
import com.consultant.core.domain.Consultation
import com.consultant.core.domain.ConsultationStatus
import java.time.{ DayOfWeek, Instant, LocalDate, LocalTime, ZoneId, ZonedDateTime }

/**
 * Service for calculating specialist availability considering existing consultations. Combines defined availability
 * slots with existing consultation bookings to determine actual free time slots.
 */
case class AvailabilityService(
  availabilitySlots: List[SpecialistAvailability],
  existingConsultations: List[Consultation]
) {

  private val zoneId = ZoneId.systemDefault()

  /**
   * Get available time slots for a specific date. Removes time slots that conflict with existing consultations.
   *
   * @param date
   *   The date to check availability for
   * @param slotDuration
   *   Duration of the consultation slot in minutes
   * @return
   *   List of available (start, end) time windows
   */
  def getAvailableSlotsForDate(date: LocalDate, slotDuration: Int = 60): List[(LocalTime, LocalTime)] = {
    val dayOfWeek = date.getDayOfWeek.getValue - 1 // Convert to 0-6 (Monday=0, Sunday=6)

    // Find availability slots for this day of week
    val dayAvailability = availabilitySlots.filter(_.dayOfWeek == dayOfWeek)

    if (dayAvailability.isEmpty) return List()

    // Get consultations for this specific date
    val consultationsOnDate = existingConsultations.filter { consultation =>
      val consultationDate = consultation.scheduledAt.atZone(zoneId).toLocalDate
      consultationDate == date
    }

    // Convert consultations to time ranges (only Scheduled consultations with duration set)
    val bookedRanges = consultationsOnDate
      .filter(c => c.scheduledAt != null && c.status == ConsultationStatus.Scheduled && c.duration.nonEmpty)
      .map { consultation =>
        val start = consultation.scheduledAt.atZone(zoneId).toLocalTime
        val end   = start.plusMinutes(consultation.duration.get.toLong)
        (start, end)
      }
      .sortBy(_._1.toSecondOfDay)()

    // Calculate free slots by subtracting booked ranges from availability
    val allSlots = dayAvailability.map { slot =>
      calculateFreeSlots(
        slot.startTime,
        slot.endTime,
        bookedRanges,
        slotDuration
      )
    }.flatten

    allSlots
  }

  /**
   * Calculate free time slots within a day's availability, considering booked consultation times.
   *
   * @param dayStart
   *   Start time of availability window
   * @param dayEnd
   *   End time of availability window
   * @param bookedRanges
   *   List of booked time windows
   * @param slotDuration
   *   Minimum duration of free slot in minutes
   * @return
   *   List of free (start, end) time windows
   */
  private def calculateFreeSlots(
    dayStart: LocalTime,
    dayEnd: LocalTime,
    bookedRanges: List[(LocalTime, LocalTime)],
    slotDuration: Int
  ): List[(LocalTime, LocalTime)] = {
    val start = dayStart
    val end   = dayEnd

    if (bookedRanges.isEmpty) {
      // No bookings, entire day is available if long enough
      if (minutesBetween(start, end) >= slotDuration) {
        return List((start, end))
      }
      return List()
    }

    val freeSlots   = scala.collection.mutable.ListBuffer[(LocalTime, LocalTime)]()
    var currentTime = start

    for ((bookedStart, bookedEnd) <- bookedRanges) {
      // Check if there's free time before this booking
      if (currentTime.isBefore(bookedStart)) {
        val gap = minutesBetween(currentTime, bookedStart)
        if (gap >= slotDuration) {
          freeSlots += ((currentTime, bookedStart))
        }
      }
      // Move past this booking
      if (bookedEnd.isAfter(currentTime)) {
        currentTime = bookedEnd
      }
    }

    // Check if there's free time after the last booking
    if (currentTime.isBefore(end)) {
      val gap = minutesBetween(currentTime, end)
      if (gap >= slotDuration) {
        freeSlots += ((currentTime, end))
      }
    }

    freeSlots.toList
  }

  /**
   * Calculate minutes between two LocalTime values.
   */
  private def minutesBetween(start: LocalTime, end: LocalTime): Int = {
    val startMinutes = start.getHour * 60 + start.getMinute
    val endMinutes   = end.getHour * 60 + end.getMinute
    endMinutes - startMinutes
  }

  /**
   * Check if a specific time slot is available.
   *
   * @param date
   *   The date to check
   * @param startTime
   *   Start time (LocalTime)
   * @param durationMinutes
   *   Duration in minutes
   * @return
   *   true if the slot is available
   */
  def isTimeSlotAvailable(date: LocalDate, startTime: LocalTime, durationMinutes: Int): Boolean = {
    val requestedStart = startTime
    val requestedEnd   = requestedStart.plusMinutes(durationMinutes.toLong)

    val availableSlots = getAvailableSlotsForDate(date, durationMinutes)

    availableSlots.exists { case (slotStart, slotEnd) =>
      !requestedStart.isBefore(slotStart) && !requestedEnd.isAfter(slotEnd)
    }
  }
}
