package com.consultant.api.dto

import java.util.UUID
import java.time.Instant
import io.circe.{ Decoder, Encoder }
import io.circe.Codec

// Specialist DTOs
case class CreateSpecialistDto(
  email: String,
  name: String,
  phone: String,
  bio: String,
  categoryRates: List[SpecialistCategoryRateDto],
  isAvailable: Boolean
) derives Codec.AsObject

case class SpecialistDto(
  id: UUID,
  email: String,
  name: String,
  phone: String,
  bio: String,
  categoryRates: List[SpecialistCategoryRateDto],
  isAvailable: Boolean,
  connections: List[SpecialistConnectionDto],
  createdAt: Instant,
  updatedAt: Instant
) derives Codec.AsObject

case class SpecialistSearchDto(
  categoryId: Option[UUID],
  minRating: Option[BigDecimal],
  maxHourlyRate: Option[BigDecimal],
  minExperience: Option[Int],
  isAvailable: Option[Boolean]
) derives Codec.AsObject

case class SpecialistCategoryRateDto(
  categoryId: UUID,
  hourlyRate: BigDecimal,
  experienceYears: Int,
  rating: Option[BigDecimal],
  totalConsultations: Option[Int]
) derives Codec.AsObject

case class UpdateSpecialistDto(
  email: String,
  name: String,
  phone: String,
  bio: String,
  categoryRates: List[SpecialistCategoryRateDto],
  isAvailable: Boolean
) derives Codec.AsObject
