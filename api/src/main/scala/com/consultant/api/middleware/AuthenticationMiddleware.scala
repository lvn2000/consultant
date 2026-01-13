package com.consultant.api.middleware

import cats.effect.IO
import cats.data.{ Kleisli, OptionT }
import org.http4s.{ AuthedRoutes, Request, Response, Status }
import org.http4s.server.AuthMiddleware
import com.consultant.core.domain.security.{ AuthToken, UserRole }
import com.consultant.infrastructure.security.JwtTokenService
import org.http4s.headers.Authorization
import org.http4s.Credentials

/** Middleware для аутентификации запросов */
class AuthenticationMiddleware(jwtService: JwtTokenService):

  /** Извлекает и валидирует JWT токен из заголовка */
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
          jwtService.validateToken(token)
        case _ =>
          IO.pure(Left("No authorization token provided"))
    }

  /** Обработчик ошибок аутентификации */
  private val onFailure: AuthedRoutes[String, IO] =
    Kleisli { req =>
      OptionT.liftF(
        IO.pure(
          Response[IO](status = Status.Unauthorized)
            .withEntity(s"""{"error": "Unauthorized", "message": "${req.context}"}""")
        )
      )
    }

  /** Middleware для защищенных endpoint'ов */
  val middleware: AuthMiddleware[IO, AuthToken] =
    AuthMiddleware(authUser, onFailure)

  /** Middleware с проверкой роли */
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

  /** Проверка роли Admin */
  val requireAdmin: Kleisli[IO, Request[IO], Either[String, AuthToken]] =
    requireRole(UserRole.Admin)

  /** Проверка роли Specialist */
  val requireSpecialist: Kleisli[IO, Request[IO], Either[String, AuthToken]] =
    requireRole(UserRole.Specialist, UserRole.Admin)

  /** Проверка роли Client или выше */
  val requireClient: Kleisli[IO, Request[IO], Either[String, AuthToken]] =
    requireRole(UserRole.Client, UserRole.Specialist, UserRole.Admin)
