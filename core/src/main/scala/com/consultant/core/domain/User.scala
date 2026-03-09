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
