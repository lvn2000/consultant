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
import com.consultant.core.domain.{ Consultation, ConsultationId, ConsultationStatus, CreateConsultationRequest }
import java.util.UUID

/** Trait for ConsultationRepository that supports transactional operations. */
trait TransactionalConsultationRepository:

  /** Create a consultation within a transaction. */
  def createTransactional(request: CreateConsultationRequest, price: BigDecimal): ConnectionIO[Consultation]

  /** Find a consultation by ID within a transaction. */
  def findByIdTransactional(id: ConsultationId): ConnectionIO[Option[Consultation]]

  /** Update a consultation within a transaction. */
  def updateTransactional(consultation: Consultation): ConnectionIO[Consultation]

  /** Update consultation status within a transaction. */
  def updateStatusTransactional(id: ConsultationId, status: ConsultationStatus): ConnectionIO[Int]

  /** Add review within a transaction. */
  def addReviewTransactional(id: ConsultationId, rating: Int, review: String): ConnectionIO[Int]

end TransactionalConsultationRepository
