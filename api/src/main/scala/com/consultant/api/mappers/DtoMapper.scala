package com.consultant.api.mappers

/**
 * Type class for mapping between domain models and DTOs.
 *
 * @tparam Domain
 *   The domain model type
 * @tparam Dto
 *   The DTO type
 */
trait DtoMapper[Domain, Dto]:
  def toDto(domain: Domain): Dto

trait BidirectionalDtoMapper[Domain, Dto] extends DtoMapper[Domain, Dto]:
  def toDto(domain: Domain): Dto
  def toDomain(dto: Dto): Domain

trait RequestDtoMapper[Dto, Request]:
  def toRequest(dto: Dto): Request

object DtoMapper:
  def apply[Domain, Dto](using mapper: DtoMapper[Domain, Dto]): DtoMapper[Domain, Dto] = mapper

  def toDto[Domain, Dto](domain: Domain)(using mapper: DtoMapper[Domain, Dto]): Dto =
    mapper.toDto(domain)

end DtoMapper
