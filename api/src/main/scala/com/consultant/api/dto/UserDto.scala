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
import com.consultant.core.domain.security.UserRole
import com.consultant.api.codec.SecurityCodecs.given
import sttp.tapir.Schema

// User DTOs
case class CreateUserDto(
  login: String,
  email: String,
  name: String,
  phone: Option[String],
  role: UserRole
) derives Codec.AsObject

case class LoginDto(
  login: String,
  password: String
) derives Codec.AsObject

object LoginDto:
  given Codec[LoginDto] = Codec.AsObject.derived

case class LoginResponseDto(
  userId: String,
  login: String,
  email: String,
  role: String,
  sessionId: String,
  accessToken: Option[String] = None
) derives Codec.AsObject

case class LogoutDto(
  sessionId: String
) derives Codec.AsObject

case class UserDto(
  id: UUID,
  login: String,
  email: String,
  name: String,
  phone: Option[String],
  role: UserRole,
  createdAt: Instant,
  updatedAt: Instant
) derives Codec.AsObject

case class UpdateUserDto(
  name: String,
  email: String,
  phone: Option[String]
) derives Codec.AsObject

case class AdminCountDto(
  count: Int
) derives Codec.AsObject
