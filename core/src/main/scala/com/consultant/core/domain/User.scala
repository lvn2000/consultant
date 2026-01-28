package com.consultant.core.domain

import java.time.Instant
import java.util.UUID
import com.consultant.core.domain.security.UserRole

object types:
  // Common types
  type UserId           = UUID
  type SpecialistId     = UUID
  type CategoryId       = UUID
  type ConsultationId   = UUID
  type ConnectionTypeId = UUID

export types.*

// User (Client) domain model

import com.consultant.core.domain.CountryId
import com.consultant.core.domain.LanguageId

case class User(
  id: UserId,
  login: String,
  email: String,
  name: String,
  phone: Option[String],
  role: UserRole,
  countryId: Option[CountryId],
  languages: Set[LanguageId],
  createdAt: Instant,
  updatedAt: Instant
)

case class CreateUserRequest(
  login: String,
  email: String,
  name: String,
  phone: Option[String],
  role: UserRole,
  countryId: Option[CountryId],
  languages: Set[LanguageId]
)
