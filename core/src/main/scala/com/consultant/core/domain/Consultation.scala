package com.consultant.core.domain

import java.time.Instant
import java.util.UUID
import types.*

// Consultation statuses
enum ConsultationStatus:
  case Requested, Scheduled, InProgress, Completed, Missed, Cancelled

// Consultation domain model
case class Consultation(
  id: ConsultationId,
  userId: UserId,
  specialistId: SpecialistId,
  categoryId: CategoryId,
  description: String,
  status: ConsultationStatus,
  scheduledAt: Instant,
  duration: Option[Int], // in minutes - set by specialist when approving
  price: BigDecimal,
  rating: Option[Int], // 1-5
  review: Option[String],
  createdAt: Instant,
  updatedAt: Instant
)

case class CreateConsultationRequest(
  userId: UserId,
  specialistId: SpecialistId,
  categoryId: CategoryId,
  description: String,
  scheduledAt: Instant,
  duration: Option[Int] = None // Client doesn't set duration, specialist will set it
)
