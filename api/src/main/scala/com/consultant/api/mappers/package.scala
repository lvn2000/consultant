package com.consultant.api

/**
 * DTO Mappers package.
 *
 * This package provides type-safe, composable mappers for converting between domain models and DTOs.
 *
 * Example usage: {{ import com.consultant.api.mappers.*
 *
 * val userDto = UserMappers.toUserDto(user) val request = UserMappers.toCreateUserRequest(dto) }}
 *
 * Or using type class syntax: {{ import com.consultant.api.mappers.DtoMapper.*
 *
 * val userDto = DtoMapper.toDto[User, UserDto](user) }}
 */
package object mappers
