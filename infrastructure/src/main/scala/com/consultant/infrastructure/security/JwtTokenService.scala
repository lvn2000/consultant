package com.consultant.infrastructure.security

import cats.effect.IO
import cats.syntax.all.*
import com.consultant.core.domain.security.*
import pdi.jwt.{ JwtAlgorithm, JwtCirce, JwtClaim }
import io.circe.syntax.*
import io.circe.parser.decode
import io.circe.{ Decoder, Encoder }
import java.time.Instant
import scala.concurrent.duration.*
import java.util.UUID

/** JWT Token Service для аутентификации */
class JwtTokenService(
  secretKey: String,
  issuer: String = "consultant-api",
  accessTokenTTL: FiniteDuration = 15.minutes,
  refreshTokenTTL: FiniteDuration = 7.days
):

  private val algorithm = JwtAlgorithm.HS512

  case class TokenClaims(
    userId: String,
    role: String,
    email: String
  ) derives Encoder.AsObject,
        Decoder

  /** Генерирует access token (JWT) */
  def generateAccessToken(userId: UUID, role: UserRole, email: String): IO[AuthToken] =
    IO {
      val now       = Instant.now()
      val expiresAt = now.plusSeconds(accessTokenTTL.toSeconds)

      val claims = TokenClaims(
        userId = userId.toString,
        role = role.toString,
        email = email
      )

      val claim = JwtClaim(
        content = claims.asJson.noSpaces,
        issuer = Some(issuer),
        issuedAt = Some(now.getEpochSecond),
        expiration = Some(expiresAt.getEpochSecond)
      )

      val token = JwtCirce.encode(claim, secretKey, algorithm)

      AuthToken(
        token = token,
        userId = userId,
        role = role,
        expiresAt = expiresAt
      )
    }

  /** Валидирует и декодирует JWT token */
  def validateToken(token: String): IO[Either[String, AuthToken]] =
    IO {
      JwtCirce.decode(token, secretKey, Seq(algorithm)).toEither match
        case Right(claim) =>
          // Проверяем expiration
          val now = Instant.now().getEpochSecond
          if claim.expiration.exists(_ < now) then Left("Token expired")
          else
            // Декодируем claims
            decode[TokenClaims](claim.content) match
              case Right(claims) =>
                Right(
                  AuthToken(
                    token = token,
                    userId = UUID.fromString(claims.userId),
                    role = UserRole.valueOf(claims.role),
                    expiresAt = Instant.ofEpochSecond(claim.expiration.getOrElse(0))
                  )
                )
              case Left(error) =>
                Left(s"Invalid token claims: ${error.getMessage}")

        case Left(error) =>
          Left(s"Invalid token: ${error.getMessage}")
    }

  /** Генерирует refresh token */
  def generateRefreshToken(userId: UUID): IO[RefreshToken] =
    IO {
      val token     = UUID.randomUUID().toString
      val expiresAt = Instant.now().plusSeconds(refreshTokenTTL.toSeconds)

      RefreshToken(
        token = token,
        userId = userId,
        expiresAt = expiresAt
      )
    }

  /** Извлекает userId из токена без полной валидации (для логирования) */
  def extractUserId(token: String): IO[Option[UUID]] =
    IO {
      JwtCirce.decode(token, secretKey, Seq(algorithm)).toOption.flatMap { claim =>
        decode[TokenClaims](claim.content).toOption.map(c => UUID.fromString(c.userId))
      }
    }.handleError(_ => None)
