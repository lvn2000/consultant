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
package com.consultant.api.middleware

import cats.effect.IO
import cats.data.{ Kleisli, OptionT }
import org.http4s.{ AuthedRoutes, Request, Response, Status }
import org.http4s.server.AuthMiddleware
import com.consultant.core.domain.security.{ AuthToken, UserRole }
import com.consultant.infrastructure.security.TokenVerifier
import com.consultant.api.dto.ErrorResponse
import org.http4s.headers.Authorization
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.Credentials

/** Middleware for request authentication */
class AuthenticationMiddleware(tokenVerifier: TokenVerifier):

  /** Extracts and validates JWT token from header */
  private val authUser: Kleisli[IO, Request[IO], Either[String, AuthToken]] =
    Kleisli { request =>
      val tokenEither = request.headers.get[Authorization].map {
        case Authorization(Credentials.Token(scheme, token)) if scheme.toString == "Bearer" =>
          token
        case _ =>
          ""
      }

      tokenEither match
        case Some(token) if token.nonEmpty =>
          tokenVerifier.verify(token)
        case _ =>
          IO.pure(Left("No authorization token provided"))
    }

  /** Authentication error handler */
  private val onFailure: AuthedRoutes[String, IO] =
    Kleisli { req =>
      OptionT.liftF(
        IO.pure(
          Response[IO](status = Status.Unauthorized)
            .withEntity(ErrorResponse("UNAUTHORIZED", req.context))
        )
      )
    }

  /** Middleware for protected endpoints */
  val middleware: AuthMiddleware[IO, AuthToken] =
    AuthMiddleware(authUser, onFailure)

  /** Middleware with role checking */
  def requireRole(allowedRoles: UserRole*): Kleisli[IO, Request[IO], Either[String, AuthToken]] =
    authUser.flatMapF { tokenResult =>
      tokenResult match
        case Right(token) if allowedRoles.contains(token.role) =>
          IO.pure(Right(token))
        case Right(_) =>
          IO.pure(Left("Insufficient permissions"))
        case Left(error) =>
          IO.pure(Left(error))
    }

  /** Check for Admin role */
  val requireAdmin: Kleisli[IO, Request[IO], Either[String, AuthToken]] =
    requireRole(UserRole.Admin)

  /** Check for Specialist role */
  val requireSpecialist: Kleisli[IO, Request[IO], Either[String, AuthToken]] =
    requireRole(UserRole.Specialist, UserRole.Admin)

  /** Check for Client role or higher */
  val requireClient: Kleisli[IO, Request[IO], Either[String, AuthToken]] =
    requireRole(UserRole.Client, UserRole.Specialist, UserRole.Admin)
