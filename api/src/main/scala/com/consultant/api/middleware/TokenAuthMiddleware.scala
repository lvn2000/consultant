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

import cats.data.{ Kleisli, OptionT }
import cats.effect.IO
import com.consultant.core.domain.security.AuthToken
import com.consultant.infrastructure.security.TokenVerifier
import com.consultant.api.dto.ErrorResponse
import org.http4s.{ Credentials, Header, HttpRoutes, Request, Response, Status }
import org.http4s.headers.Authorization
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.typelevel.ci.CIString

object TokenAuthMiddleware:

  def protect(
    tokenVerifier: TokenVerifier,
    isPublic: Request[IO] => Boolean
  ): HttpRoutes[IO] => HttpRoutes[IO] = { routes =>
    Kleisli { req =>
      if isPublic(req) then routes(req)
      else
        extractBearerToken(req) match
          case Some(token) =>
            OptionT.liftF(tokenVerifier.verify(token)).flatMap {
              case Right(authToken) =>
                val authedReq = attachHeaders(req, authToken)
                routes(authedReq)
              case Left(error) =>
                unauthorized(req, error)
            }
          case None =>
            unauthorized(req, "No authorization token provided")
    }
  }

  private def extractBearerToken(req: Request[IO]): Option[String] =
    req.headers.get[Authorization].collect {
      case Authorization(Credentials.Token(scheme, token)) if scheme.toString.equalsIgnoreCase("Bearer") => token
    }

  private def attachHeaders(req: Request[IO], authToken: AuthToken): Request[IO] =
    req.putHeaders(
      // SECURITY: These headers are set from the verified JWT token, NOT from the client request.
      // putHeaders() overwrites any client-provided headers with these names, ensuring the routes
      // always receive trusted values extracted from the authenticated token.
      // X-Auth-User-Id contains the authenticated principal (from token)
      Header.Raw(CIString("X-Auth-User-Id"), authToken.userId.toString),
      // X-User-Role contains the authenticated user's role for authorization checks
      Header.Raw(CIString("X-User-Role"), authToken.role.toString)
    )

  private def unauthorized(req: Request[IO], message: String): OptionT[IO, Response[IO]] =
    OptionT.liftF {
      IO.pure {
        val origin = req.headers
          .get(CIString("Origin"))
          .map(_.head.value)
          .getOrElse("*")

        Response[IO](status = Status.Unauthorized)
          .withEntity(ErrorResponse("UNAUTHORIZED", message))
          .withHeaders(
            Header.Raw(CIString("Access-Control-Allow-Origin"), origin),
            Header.Raw(CIString("Access-Control-Allow-Methods"), "GET, POST, PUT, DELETE, OPTIONS"),
            Header.Raw(
              CIString("Access-Control-Allow-Headers"),
              "Content-Type, Authorization, X-Auth-User-Id, X-User-Id, X-User-Role, Accept"
            )
          )
      }
    }
