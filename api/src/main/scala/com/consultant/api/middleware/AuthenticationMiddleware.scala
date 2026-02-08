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
            .withEntity(ErrorResponse("Unauthorized", req.context))
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
