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

// Consultation DTOs
case class CreateConsultationDto(
  userId: UUID,
  specialistId: UUID,
  categoryId: UUID,
  description: String,
  scheduledAt: Instant,
  duration: Option[Int] = None // Client doesn't set duration
) derives Codec.AsObject

case class ConsultationDto(
  id: UUID,
  userId: UUID,
  specialistId: UUID,
  categoryId: UUID,
  description: String,
  status: String,
  scheduledAt: Instant,
  duration: Option[Int],
  price: BigDecimal,
  rating: Option[Int],
  review: Option[String],
  createdAt: Instant,
  updatedAt: Instant
) derives Codec.AsObject

case class AddReviewDto(
  rating: Int,
  review: String
) derives Codec.AsObject

case class UpdateConsultationStatusDto(
  status: String
) derives Codec.AsObject

case class ApproveConsultationDto(
  status: String,
  duration: Int
) derives Codec.AsObject

// DTO for paginated consultation results
case class PaginatedConsultationsDto(
  consultations: List[ConsultationDto],
  totalCount: Long,
  offset: Int,
  limit: Int
) derives Codec.AsObject
