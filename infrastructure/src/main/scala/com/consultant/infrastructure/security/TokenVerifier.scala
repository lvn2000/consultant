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
