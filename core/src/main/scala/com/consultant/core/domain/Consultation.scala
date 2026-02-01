package com.consultant.core.domain

import java.time.Instant
import java.util.UUID
import types.*

// Consultation statuses
enum ConsultationStatus:
  case Requested, Confirmed, InProgress, Completed, Cancelled

// Consultation domain model
case class Consultation(
  id: ConsultationId,
  userId: UserId,
  specialistId: SpecialistId,
  categoryId: CategoryId,
  description: String,
  status: ConsultationStatus,
  scheduledAt: Option[Instant],
  duration: Option[Int], // in minutes
  price: BigDecimal,
  isFree: Boolean = false, // Новое поле для бесплатных консультаций
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
  scheduledAt: Option[Instant],
  duration: Option[Int],
  isFree: Boolean = false // Новое поле для бесплатных консультаций
)
