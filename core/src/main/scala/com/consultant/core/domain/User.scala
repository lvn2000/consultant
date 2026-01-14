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
case class User(
  id: UserId,
  email: String,
  name: String,
  phone: Option[String],
  role: UserRole,
  createdAt: Instant,
  updatedAt: Instant
)

case class CreateUserRequest(
  email: String,
  name: String,
  phone: Option[String],
  role: UserRole
)
