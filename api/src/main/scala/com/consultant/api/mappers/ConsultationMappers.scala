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
