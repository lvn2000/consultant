package com.consultant.api.dto

import java.util.UUID
import java.time.Instant
import io.circe.{ Decoder, Encoder }
import io.circe.Codec

// Consultation DTOs
case class CreateConsultationDto(
  userId: UUID,
  specialistId: UUID,
  categoryId: UUID,
  description: String,
  scheduledAt: Instant,
  duration: Option[Int] = None // Client doesn't set duration
) derives Codec.AsObject

case class ConsultationDto(
  id: UUID,
  userId: UUID,
  specialistId: UUID,
  categoryId: UUID,
  description: String,
  status: String,
  scheduledAt: Instant,
  duration: Option[Int],
  price: BigDecimal,
  rating: Option[Int],
  review: Option[String],
  createdAt: Instant,
  updatedAt: Instant
) derives Codec.AsObject

case class AddReviewDto(
  rating: Int,
  review: String
) derives Codec.AsObject

case class UpdateConsultationStatusDto(
  status: String
) derives Codec.AsObject

case class ApproveConsultationDto(
  status: String,
  duration: Int
) derives Codec.AsObject

// DTO for paginated consultation results
case class PaginatedConsultationsDto(
  consultations: List[ConsultationDto],
  totalCount: Long,
  offset: Int,
  limit: Int
) derives Codec.AsObject
