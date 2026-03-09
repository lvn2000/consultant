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
package com.consultant.data.repository

import cats.effect.IO
import doobie.ConnectionIO
import com.consultant.core.domain.{
  CategoryId,
  CreateSpecialistRequest,
  Specialist,
  SpecialistCategoryRate,
  SpecialistId
}
import java.util.UUID
import java.time.Instant

/** Trait for SpecialistRepository that supports transactional operations. */
trait TransactionalSpecialistRepository:

  /** Create a specialist within a transaction. */
  def createTransactional(request: CreateSpecialistRequest): ConnectionIO[Specialist]

  /**
   * Find a specialist by ID within a transaction. Returns a tuple of raw data that can be assembled into a Specialist.
   */
  def findByIdTransactional(id: SpecialistId): ConnectionIO[Option[
    (
      UUID,                        // id
      String,                      // email
      String,                      // name
      String,                      // phone
      String,                      // bio
      Boolean,                     // isAvailable
      Option[UUID],                // countryId
      Instant,                     // createdAt
      Instant,                     // updatedAt
      Set[UUID],                   // languages
      List[SpecialistCategoryRate] // categoryRates
    )
  ]]

  /** Update a specialist within a transaction. */
  def updateTransactional(specialist: Specialist): ConnectionIO[Specialist]

  /** Update category rating within a transaction. */
  def updateCategoryRatingTransactional(
    specialistId: SpecialistId,
    categoryId: CategoryId,
    rating: BigDecimal,
    consultationCount: Int
  ): ConnectionIO[Int]

  /** Delete a specialist within a transaction. */
  def deleteTransactional(id: SpecialistId): ConnectionIO[Int]

end TransactionalSpecialistRepository
