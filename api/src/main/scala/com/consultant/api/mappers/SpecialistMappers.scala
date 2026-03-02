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
