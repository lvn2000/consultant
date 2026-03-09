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
package com.consultant.infrastructure.security

import cats.effect.IO
import com.consultant.core.domain.security.AuthToken

trait TokenVerifier:
  def verify(token: String): IO[Either[String, AuthToken]]

class LegacyJwtTokenVerifier(jwtService: JwtTokenService) extends TokenVerifier:
  override def verify(token: String): IO[Either[String, AuthToken]] =
    jwtService.validateToken(token)

class CompositeTokenVerifier(primary: Option[TokenVerifier], fallback: TokenVerifier) extends TokenVerifier:
  override def verify(token: String): IO[Either[String, AuthToken]] =
    primary match
      case Some(verifier) =>
        verifier.verify(token).flatMap {
          case right @ Right(_) => IO.pure(right)
          case Left(_)          => fallback.verify(token)
        }
      case None =>
        fallback.verify(token)
