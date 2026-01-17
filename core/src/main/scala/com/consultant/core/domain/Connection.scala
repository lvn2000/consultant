package com.consultant.core.domain

import java.time.Instant
import java.util.UUID
import types.*

// ConnectionType domain model (Viber, WhatsApp, Slack, etc.)
case class ConnectionType(
  id: ConnectionTypeId,
  name: String,
  description: Option[String],
  createdAt: Instant,
  updatedAt: Instant
)

case class CreateConnectionTypeRequest(
  name: String,
  description: Option[String]
)

case class UpdateConnectionTypeRequest(
  name: String,
  description: Option[String]
)

// Specialist connection to a service (e.g., specialist's WhatsApp number)
case class SpecialistConnection(
  id: UUID,
  specialistId: SpecialistId,
  connectionTypeId: ConnectionTypeId,
  connectionValue: String, // The actual contact value (phone number, username, etc.)
  isVerified: Boolean,
  createdAt: Instant,
  updatedAt: Instant
)

case class CreateConnectionRequest(
  connectionTypeId: ConnectionTypeId,
  connectionValue: String
)
