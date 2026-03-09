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
import cats.syntax.all.*
import com.consultant.core.domain.security.{ AuthToken, UserRole }
import com.consultant.infrastructure.config.OidcConfig
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.jwk.{ JWK, JWKSet, KeyType, KeyUse }
import com.nimbusds.jose.proc.BadJWSException
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory
import com.nimbusds.jwt.SignedJWT
import java.net.URI
import java.net.http.{ HttpClient, HttpRequest, HttpResponse }
import java.time.{ Duration, Instant }
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import scala.jdk.CollectionConverters.*

class OidcTokenVerifier(config: OidcConfig) extends TokenVerifier:
  private val httpClient = HttpClient
    .newBuilder()
    .connectTimeout(Duration.ofSeconds(config.jwksTimeoutSeconds))
    .build()
  private val jwksCache = new AtomicReference[Option[(Instant, JWKSet)]](None)

  override def verify(token: String): IO[Either[String, AuthToken]] =
    if !config.enabled then IO.pure(Left("OIDC verification is disabled"))
    else
      (for
        issuer    <- IO.fromOption(config.issuer)(new RuntimeException("OIDC issuer is not configured"))
        jwksUri   <- IO.fromOption(config.jwksUri)(new RuntimeException("OIDC JWKS URI is not configured"))
        signedJwt <- IO(SignedJWT.parse(token))
        _         <- validateAlg(signedJwt.getHeader)
        jwks      <- getJwks(jwksUri)
        jwk <- IO.fromOption(selectJwk(jwks, signedJwt.getHeader))(
          new RuntimeException("No suitable JWK found for token")
        )
        _         <- verifySignature(signedJwt, jwk)
        claims    <- IO(signedJwt.getJWTClaimsSet)
        _         <- validateClaims(issuer, claims)
        authToken <- IO.fromEither(buildAuthToken(token, claims).leftMap(new RuntimeException(_)))
      yield authToken).attempt.map {
        case Right(authToken) => Right(authToken)
        case Left(error)      => Left(error.getMessage)
      }

  private def validateAlg(header: JWSHeader): IO[Unit] =
    IO {
      val alg     = header.getAlgorithm
      val algName = Option(alg).map(_.getName).getOrElse("")
      if !config.allowedAlgs.contains(algName) then throw new RuntimeException(s"Disallowed JWT alg: $algName")
    }

  private def getJwks(jwksUri: String): IO[JWKSet] =
    for
      now    <- IO(Instant.now())
      cached <- IO(Option(jwksCache.get()).flatten)
      jwks <- cached match
        case Some((cachedAt, jwks)) if !isCacheExpired(cachedAt, now) => IO.pure(jwks)
        case _ =>
          fetchJwks(jwksUri).flatTap { jwkSet =>
            IO(jwksCache.set(Some(now -> jwkSet)))
          }
    yield jwks

  private def isCacheExpired(cachedAt: Instant, now: Instant): Boolean =
    now.isAfter(cachedAt.plusSeconds(config.jwksCacheSeconds))

  private def fetchJwks(jwksUri: String): IO[JWKSet] =
    IO.blocking {
      val request = HttpRequest
        .newBuilder()
        .uri(URI.create(jwksUri))
        .timeout(Duration.ofSeconds(config.jwksTimeoutSeconds))
        .GET()
        .build()
      val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
      if response.statusCode() / 100 != 2 then
        throw new RuntimeException(s"Failed to fetch JWKS: ${response.statusCode()}")
      JWKSet.parse(response.body())
    }

  private def selectJwk(jwkSet: JWKSet, header: JWSHeader): Option[JWK] =
    val keys = jwkSet.getKeys.asScala.toList
    val kid  = Option(header.getKeyID)
    val alg  = Option(header.getAlgorithm).map(_.getName)

    kid.flatMap(id => Option(jwkSet.getKeyByKeyId(id))).orElse {
      keys.find { key =>
        val keyUseOk = Option(key.getKeyUse).contains(KeyUse.SIGNATURE) || key.getKeyUse == null
        // Accept keys without alg field (many JWKS entries omit it); signature verification validates correctness
        val algOk = Option(key.getAlgorithm) match
          case Some(keyAlg) =>
            alg.forall(tokenAlg => tokenAlg == keyAlg.getName) // If key declares alg, must match token's alg
          case None => true // If key omits alg, accept it
        keyUseOk && algOk
      }
    }

  private def verifySignature(signedJwt: SignedJWT, jwk: JWK): IO[Unit] =
    IO {
      val key = jwk.getKeyType match
        case KeyType.RSA => jwk.toRSAKey.toRSAPublicKey
        case KeyType.EC  => jwk.toECKey.toECPublicKey
        case other       => throw new RuntimeException(s"Unsupported JWK key type: ${other.getValue}")

      val verifier = new DefaultJWSVerifierFactory().createJWSVerifier(signedJwt.getHeader, key)
      if !signedJwt.verify(verifier) then throw new BadJWSException("Invalid JWT signature")
    }

  private def validateClaims(issuer: String, claims: com.nimbusds.jwt.JWTClaimsSet): IO[Unit] =
    IO {
      if claims.getIssuer != issuer then throw new RuntimeException("Invalid token issuer")

      config.audience.foreach { aud =>
        val audiences = Option(claims.getAudience).map(_.asScala.toSet).getOrElse(Set.empty)
        if !audiences.contains(aud) then throw new RuntimeException("Invalid token audience")
      }

      val now = Instant.now().getEpochSecond
      val exp = Option(claims.getExpirationTime).map(_.toInstant.getEpochSecond)
      val nbf = Option(claims.getNotBeforeTime).map(_.toInstant.getEpochSecond)

      if exp.forall(_ <= now) then throw new RuntimeException("Token expired")

      if nbf.exists(_ > now) then throw new RuntimeException("Token not active yet")
    }

  private def buildAuthToken(token: String, claims: com.nimbusds.jwt.JWTClaimsSet): Either[String, AuthToken] =
    val subject = Option(claims.getSubject).getOrElse("")
    val userId =
      try Right(UUID.fromString(subject))
      catch case _: IllegalArgumentException => Left("Token subject is not a valid UUID")

    val role = extractRole(claims)
    val expiresAt = Option(claims.getExpirationTime)
      .map(_.toInstant)
      .getOrElse(Instant.EPOCH)

    userId.map { id =>
      AuthToken(
        token = token,
        userId = id,
        role = role,
        expiresAt = expiresAt
      )
    }

  private def extractRole(claims: com.nimbusds.jwt.JWTClaimsSet): UserRole =
    val roles = rolesFromRealmAccess(claims) ++ rolesFromResourceAccess(claims) ++ rolesFromStringClaim(claims)

    if roles.contains("admin") then UserRole.Admin
    else if roles.contains("specialist") then UserRole.Specialist
    else UserRole.Client

  private def rolesFromRealmAccess(claims: com.nimbusds.jwt.JWTClaimsSet): Set[String] =
    Option(claims.getJSONObjectClaim("realm_access"))
      .flatMap(extractRolesFromMap)
      .getOrElse(Set.empty)

  private def rolesFromResourceAccess(claims: com.nimbusds.jwt.JWTClaimsSet): Set[String] =
    val audienceKey    = config.audience
    val resourceAccess = Option(claims.getJSONObjectClaim("resource_access"))
    (resourceAccess, audienceKey) match
      case (Some(map), Some(aud)) =>
        Option(map.get(aud))
          .flatMap(extractRolesFromValue)
          .getOrElse(Set.empty)
      case _ => Set.empty

  private def rolesFromStringClaim(claims: com.nimbusds.jwt.JWTClaimsSet): Set[String] =
    Option(claims.getStringClaim("role"))
      .map(_.toLowerCase.trim)
      .filter(_.nonEmpty)
      .map(Set(_))
      .getOrElse(Set.empty)

  private def extractRolesFromMap(value: java.util.Map[String, Object]): Option[Set[String]] =
    Option(value.get("roles")).flatMap(extractRolesFromValue)

  private def extractRolesFromValue(value: Any): Option[Set[String]] =
    value match
      case list: java.util.Collection[_] =>
        Some(list.asScala.toList.map(_.toString.toLowerCase).toSet)
      case map: java.util.Map[_, _] =>
        val stringKeyMap = map.asInstanceOf[java.util.Map[String, Object]]
        Option(stringKeyMap.get("roles")).flatMap(extractRolesFromValue)
      case _ => None
