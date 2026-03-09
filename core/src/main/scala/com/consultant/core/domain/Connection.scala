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

// Client connection to a service (e.g., client's WhatsApp number)
case class ClientConnection(
  id: UUID,
  userId: UserId,
  connectionTypeId: ConnectionTypeId,
  connectionValue: String, // The actual contact value (phone number, username, etc.)
  isVerified: Boolean,
  createdAt: Instant,
  updatedAt: Instant
)
