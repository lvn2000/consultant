package com.consultant.api.mappers

import com.consultant.core.domain.{ CreateUserRequest, User }
import com.consultant.api.dto.{ CreateUserDto, UserDto }

/** Mappers for User domain <-> DTO conversions. */
object UserMappers:

  /** Mapper from User domain to UserDto */
  given DtoMapper[User, UserDto] with
    def toDto(user: User): UserDto =
      UserDto(
        user.id,
        user.login,
        user.email,
        user.name,
        user.phone,
        user.role,
        user.createdAt,
        user.updatedAt
      )

  /** Mapper from CreateUserDto to CreateUserRequest */
  given RequestDtoMapper[CreateUserDto, CreateUserRequest] with
    def toRequest(dto: CreateUserDto): CreateUserRequest =
      CreateUserRequest(
        login = dto.login,
        email = dto.email,
        name = dto.name,
        phone = dto.phone,
        role = dto.role,
        countryId = None,
        languages = Set.empty
      )

  // Convenience methods for explicit usage
  def toUserDto(user: User): UserDto =
    summon[DtoMapper[User, UserDto]].toDto(user)

  def toCreateUserRequest(dto: CreateUserDto): CreateUserRequest =
    summon[RequestDtoMapper[CreateUserDto, CreateUserRequest]].toRequest(dto)

end UserMappers
