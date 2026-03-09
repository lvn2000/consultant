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
