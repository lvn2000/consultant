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
  scheduledAt: Option[Instant],
  duration: Option[Int]
) derives Codec.AsObject

case class ConsultationDto(
  id: UUID,
  userId: UUID,
  specialistId: UUID,
  categoryId: UUID,
  description: String,
  status: String,
  scheduledAt: Option[Instant],
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
