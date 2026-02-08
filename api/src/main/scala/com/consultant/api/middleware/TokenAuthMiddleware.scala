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
                unauthorized(error)
            }
          case None =>
            unauthorized("No authorization token provided")
    }
  }

  private def extractBearerToken(req: Request[IO]): Option[String] =
    req.headers.get[Authorization].collect {
      case Authorization(Credentials.Token(scheme, token)) if scheme.toString == "Bearer" => token
    }

  private def attachHeaders(req: Request[IO], authToken: AuthToken): Request[IO] =
    req.putHeaders(
      Header.Raw(CIString("X-User-Id"), authToken.userId.toString),
      Header.Raw(CIString("X-User-Role"), authToken.role.toString)
    )

  private def unauthorized(message: String): OptionT[IO, Response[IO]] =
    OptionT.liftF(
      IO.pure(
        Response[IO](status = Status.Unauthorized)
          .withEntity(ErrorResponse("Unauthorized", message))
      )
    )
