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
  languages: Set[LanguageId],
  id: Option[SpecialistId] = None
)

case class SpecialistSearchCriteria(
  categoryId: Option[CategoryId],
  minRating: Option[BigDecimal],
  maxHourlyRate: Option[BigDecimal],
  minExperience: Option[Int],
  isAvailable: Option[Boolean]
)
