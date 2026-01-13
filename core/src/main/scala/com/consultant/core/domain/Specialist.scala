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
  categories: List[CategoryId],
  hourlyRate: BigDecimal,
  experienceYears: Int,
  rating: Option[BigDecimal],
  totalConsultations: Int,
  isAvailable: Boolean,
  createdAt: Instant,
  updatedAt: Instant
)

case class CreateSpecialistRequest(
  email: String,
  name: String,
  phone: String,
  bio: String,
  categories: List[CategoryId],
  hourlyRate: BigDecimal,
  experienceYears: Int
)

case class SpecialistSearchCriteria(
  categoryId: Option[CategoryId],
  minRating: Option[BigDecimal],
  maxHourlyRate: Option[BigDecimal],
  minExperience: Option[Int],
  isAvailable: Option[Boolean]
)
