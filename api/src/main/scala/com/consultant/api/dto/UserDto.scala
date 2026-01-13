package com.consultant.api.dto

import java.util.UUID
import java.time.Instant
import io.circe.{ Decoder, Encoder }
import io.circe.Codec
import com.consultant.core.domain.security.UserRole
import com.consultant.api.codec.SecurityCodecs.given

// User DTOs
case class CreateUserDto(
  email: String,
  name: String,
  phone: Option[String],
  role: UserRole
) derives Codec.AsObject

case class UserDto(
  id: UUID,
  email: String,
  name: String,
  phone: Option[String],
  role: UserRole,
  createdAt: Instant,
  updatedAt: Instant
) derives Codec.AsObject
