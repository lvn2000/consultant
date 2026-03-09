/*
 * Copyright (c) 2026 Volodymyr Lubenchenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
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

case class CreateConnectionTypeDto(
  name: String,
  description: Option[String]
) derives Codec.AsObject

case class UpdateConnectionTypeDto(
  name: String,
  description: Option[String]
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

// ClientConnection DTOs
case class ClientConnectionDto(
  id: UUID,
  userId: UUID,
  connectionTypeId: UUID,
  connectionValue: String,
  isVerified: Boolean,
  createdAt: Instant,
  updatedAt: Instant
) derives Codec.AsObject
