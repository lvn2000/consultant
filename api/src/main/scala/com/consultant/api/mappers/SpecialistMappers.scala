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

import com.consultant.core.domain.*
import com.consultant.api.dto.*

/** Mappers for Specialist domain <-> DTO conversions. */
object SpecialistMappers:

  /** Mapper for SpecialistCategoryRate <-> SpecialistCategoryRateDto */
  given BidirectionalDtoMapper[SpecialistCategoryRate, SpecialistCategoryRateDto] with
    def toDto(rate: SpecialistCategoryRate): SpecialistCategoryRateDto =
      SpecialistCategoryRateDto(
        rate.categoryId,
        rate.hourlyRate,
        rate.experienceYears,
        rate.rating,
        Some(rate.totalConsultations)
      )

    def toDomain(dto: SpecialistCategoryRateDto): SpecialistCategoryRate =
      SpecialistCategoryRate(
        dto.categoryId,
        dto.hourlyRate,
        dto.experienceYears,
        dto.rating,
        dto.totalConsultations.getOrElse(0)
      )

  /** Mapper from Specialist domain to SpecialistDto */
  given DtoMapper[Specialist, SpecialistDto] with
    def toDto(specialist: Specialist): SpecialistDto =
      SpecialistDto(
        specialist.id,
        specialist.email,
        specialist.name,
        specialist.phone,
        specialist.bio,
        specialist.categoryRates.map(SpecialistMappers.toSpecialistCategoryRateDto),
        specialist.isAvailable,
        specialist.connections.map(SpecialistMappers.toSpecialistConnectionDto),
        specialist.createdAt,
        specialist.updatedAt
      )

  /** Mapper from CreateSpecialistDto to CreateSpecialistRequest */
  given RequestDtoMapper[CreateSpecialistDto, CreateSpecialistRequest] with
    def toRequest(dto: CreateSpecialistDto): CreateSpecialistRequest =
      CreateSpecialistRequest(
        email = dto.email,
        name = dto.name,
        phone = dto.phone,
        bio = dto.bio,
        categoryRates = dto.categoryRates.map(SpecialistMappers.toSpecialistCategoryRate),
        isAvailable = dto.isAvailable,
        countryId = None,
        languages = Set.empty
      )

  /** Mapper from SpecialistSearchDto to SpecialistSearchCriteria */
  given RequestDtoMapper[SpecialistSearchDto, SpecialistSearchCriteria] with
    def toRequest(dto: SpecialistSearchDto): SpecialistSearchCriteria =
      SpecialistSearchCriteria(
        dto.categoryId,
        dto.minRating,
        dto.maxHourlyRate,
        dto.minExperience,
        dto.isAvailable
      )

  // Convenience methods for explicit usage
  def toSpecialistDto(specialist: Specialist): SpecialistDto =
    summon[DtoMapper[Specialist, SpecialistDto]].toDto(specialist)

  def toSpecialistCategoryRateDto(rate: SpecialistCategoryRate): SpecialistCategoryRateDto =
    summon[BidirectionalDtoMapper[SpecialistCategoryRate, SpecialistCategoryRateDto]].toDto(rate)

  def toSpecialistCategoryRate(dto: SpecialistCategoryRateDto): SpecialistCategoryRate =
    summon[BidirectionalDtoMapper[SpecialistCategoryRate, SpecialistCategoryRateDto]].toDomain(dto)

  def toCreateSpecialistRequest(dto: CreateSpecialistDto): CreateSpecialistRequest =
    summon[RequestDtoMapper[CreateSpecialistDto, CreateSpecialistRequest]].toRequest(dto)

  def toSpecialistSearchCriteria(dto: SpecialistSearchDto): SpecialistSearchCriteria =
    summon[RequestDtoMapper[SpecialistSearchDto, SpecialistSearchCriteria]].toRequest(dto)

  // Connection mappers (used by Specialist)
  def toSpecialistConnectionDto(connection: SpecialistConnection): SpecialistConnectionDto =
    SpecialistConnectionDto(
      connection.id,
      connection.specialistId,
      connection.connectionTypeId,
      connection.connectionValue,
      connection.isVerified,
      connection.createdAt,
      connection.updatedAt
    )

end SpecialistMappers
