package com.consultant.api.dto

import java.util.UUID
import java.time.Instant
import io.circe.{ Decoder, Encoder }
import io.circe.Codec

// ConnectionType DTOs
case class ConnectionTypeDto(
  id: UUID,
  name: String,
  description: Option[String],
  createdAt: Instant,
  updatedAt: Instant
) derives Codec.AsObject

// SpecialistConnection DTOs
case class CreateConnectionDto(
  connectionTypeId: UUID,
  connectionValue: String
) derives Codec.AsObject

case class SpecialistConnectionDto(
  id: UUID,
  specialistId: UUID,
  connectionTypeId: UUID,
  connectionValue: String,
  isVerified: Boolean,
  createdAt: Instant,
  updatedAt: Instant
) derives Codec.AsObject

case class UpdateConnectionDto(
  connectionValue: String
) derives Codec.AsObject
