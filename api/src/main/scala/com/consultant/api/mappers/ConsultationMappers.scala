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
package com.consultant.api.mappers

import com.consultant.core.domain.{ Consultation, CreateConsultationRequest }
import com.consultant.api.dto.{ ConsultationDto, CreateConsultationDto }

/** Mappers for Consultation domain <-> DTO conversions. */
object ConsultationMappers:

  /** Mapper from Consultation domain to ConsultationDto */
  given DtoMapper[Consultation, ConsultationDto] with
    def toDto(consultation: Consultation): ConsultationDto =
      ConsultationDto(
        consultation.id,
        consultation.userId,
        consultation.specialistId,
        consultation.categoryId,
        consultation.description,
        consultation.status.toString,
        consultation.scheduledAt,
        consultation.duration,
        consultation.price,
        consultation.rating,
        consultation.review,
        consultation.createdAt,
        consultation.updatedAt
      )

  /** Mapper from CreateConsultationDto to CreateConsultationRequest */
  given RequestDtoMapper[CreateConsultationDto, CreateConsultationRequest] with
    def toRequest(dto: CreateConsultationDto): CreateConsultationRequest =
      CreateConsultationRequest(
        dto.userId,
        dto.specialistId,
        dto.categoryId,
        dto.description,
        dto.scheduledAt,
        dto.duration
      )

  // Convenience methods for explicit usage
  def toConsultationDto(consultation: Consultation): ConsultationDto =
    summon[DtoMapper[Consultation, ConsultationDto]].toDto(consultation)

  def toCreateConsultationRequest(dto: CreateConsultationDto): CreateConsultationRequest =
    summon[RequestDtoMapper[CreateConsultationDto, CreateConsultationRequest]].toRequest(dto)

end ConsultationMappers
