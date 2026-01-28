package com.consultant.core.domain

import java.time.Instant
import java.util.UUID
import types.*

// Specialist domain model

import com.consultant.core.domain.CountryId
import com.consultant.core.domain.LanguageId

case class Specialist(
  id: SpecialistId,
  email: String,
  name: String,
  phone: String,
  bio: String,
  categoryRates: List[SpecialistCategoryRate],
  isAvailable: Boolean,
  connections: List[SpecialistConnection],
  countryId: Option[CountryId],
  languages: Set[LanguageId],
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
  isAvailable: Boolean,
  countryId: Option[CountryId],
  languages: Set[LanguageId]
)

case class SpecialistSearchCriteria(
  categoryId: Option[CategoryId],
  minRating: Option[BigDecimal],
  maxHourlyRate: Option[BigDecimal],
  minExperience: Option[Int],
  isAvailable: Option[Boolean]
)
