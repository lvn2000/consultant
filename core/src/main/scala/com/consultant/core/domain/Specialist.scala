package com.consultant.core.domain

import java.time.Instant
import java.util.UUID
import types.*

// Specialist domain model
case class Specialist(
  id: SpecialistId,
  email: String,
  name: String,
  phone: String,
  bio: String,
  categoryRates: List[SpecialistCategoryRate],
  isAvailable: Boolean,
  connections: List[SpecialistConnection],
  createdAt: Instant,
  updatedAt: Instant
)

case class SpecialistCategoryRate(
  categoryId: CategoryId,
  hourlyRate: BigDecimal,
  experienceYears: Int,
  rating: Option[BigDecimal],
  totalConsultations: Int
)

case class CreateSpecialistRequest(
  email: String,
  name: String,
  phone: String,
  bio: String,
  categoryRates: List[SpecialistCategoryRate],
  isAvailable: Boolean
)

case class SpecialistSearchCriteria(
  categoryId: Option[CategoryId],
  minRating: Option[BigDecimal],
  maxHourlyRate: Option[BigDecimal],
  minExperience: Option[Int],
  isAvailable: Option[Boolean]
)
