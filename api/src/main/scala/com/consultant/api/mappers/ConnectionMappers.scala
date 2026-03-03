package com.consultant.api.mappers

import com.consultant.core.domain.*
import com.consultant.api.dto.*

/** Mappers for Connection domain <-> DTO conversions. */
object ConnectionMappers:

  /** Mapper from ConnectionType domain to ConnectionTypeDto */
  given DtoMapper[ConnectionType, ConnectionTypeDto] with
    def toDto(connType: ConnectionType): ConnectionTypeDto =
      ConnectionTypeDto(
        connType.id,
        connType.name,
        connType.description,
        connType.createdAt,
        connType.updatedAt
      )

  /** Mapper from CreateConnectionTypeDto to CreateConnectionTypeRequest */
  given RequestDtoMapper[CreateConnectionTypeDto, CreateConnectionTypeRequest] with
    def toRequest(dto: CreateConnectionTypeDto): CreateConnectionTypeRequest =
      CreateConnectionTypeRequest(dto.name, dto.description)

  /** Mapper from UpdateConnectionTypeDto to UpdateConnectionTypeRequest */
  given RequestDtoMapper[UpdateConnectionTypeDto, UpdateConnectionTypeRequest] with
    def toRequest(dto: UpdateConnectionTypeDto): UpdateConnectionTypeRequest =
      UpdateConnectionTypeRequest(dto.name, dto.description)

  /** Mapper from CreateConnectionDto to CreateConnectionRequest */
  given RequestDtoMapper[CreateConnectionDto, CreateConnectionRequest] with
    def toRequest(dto: CreateConnectionDto): CreateConnectionRequest =
      CreateConnectionRequest(dto.connectionTypeId, dto.connectionValue)

  // Convenience methods for explicit usage
  def toConnectionTypeDto(connType: ConnectionType): ConnectionTypeDto =
    summon[DtoMapper[ConnectionType, ConnectionTypeDto]].toDto(connType)

  def toCreateConnectionTypeRequest(dto: CreateConnectionTypeDto): CreateConnectionTypeRequest =
    summon[RequestDtoMapper[CreateConnectionTypeDto, CreateConnectionTypeRequest]].toRequest(dto)

  def toUpdateConnectionTypeRequest(dto: UpdateConnectionTypeDto): UpdateConnectionTypeRequest =
    summon[RequestDtoMapper[UpdateConnectionTypeDto, UpdateConnectionTypeRequest]].toRequest(dto)

  def toCreateConnectionRequest(dto: CreateConnectionDto): CreateConnectionRequest =
    summon[RequestDtoMapper[CreateConnectionDto, CreateConnectionRequest]].toRequest(dto)

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

  def toClientConnectionDto(connection: ClientConnection): ClientConnectionDto =
    ClientConnectionDto(
      connection.id,
      connection.userId,
      connection.connectionTypeId,
      connection.connectionValue,
      connection.isVerified,
      connection.createdAt,
      connection.updatedAt
    )

end ConnectionMappers
