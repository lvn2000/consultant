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
package com.consultant.api.di

import cats.effect.IO
import com.consultant.core.ports.NotificationService
import com.consultant.infrastructure.config.AppConfig
import com.consultant.infrastructure.local.MockNotificationService
import com.consultant.infrastructure.security.{
  AuthenticationService,
  JwtTokenService,
  PasswordHashingService,
  TokenVerifier
}

/**
 * InfrastructureModule provides infrastructure-level services.
 *
 * This includes security services, notification services, and other external integrations.
 */
trait InfrastructureModule extends RepositoryModule:
  def config: AppConfig

  // Security services
  lazy val jwtService: JwtTokenService = new JwtTokenService(
    secretKey = config.jwt.secret,
    issuer = config.jwt.issuer,
    accessTokenTTL = config.jwt.accessTtl,
    refreshTokenTTL = config.jwt.refreshTtl
  )

  lazy val tokenVerifier: TokenVerifier = buildTokenVerifier()

  lazy val passwordService: PasswordHashingService = PasswordHashingService()

  lazy val authService: AuthenticationService = AuthenticationService(
    config,
    userRepo,
    specialistRepo,
    credentialsRepo,
    refreshTokenRepo,
    auditRepo,
    passwordService,
    jwtService
  )

  // Notification service (can be swapped for AWS SES in production)
  lazy val notificationService: NotificationService = MockNotificationService()

  private def buildTokenVerifier(): TokenVerifier =
    val oidcVerifier   = new com.consultant.infrastructure.security.OidcTokenVerifier(config.oidc)
    val legacyVerifier = new com.consultant.infrastructure.security.LegacyJwtTokenVerifier(jwtService)

    if config.oidc.enabled && config.legacyAuthEnabled then
      com.consultant.infrastructure.security.CompositeTokenVerifier(Some(oidcVerifier), legacyVerifier)
    else if config.oidc.enabled then oidcVerifier
    else if config.legacyAuthEnabled then legacyVerifier
    else
      throw new IllegalStateException(
        "Invalid authentication configuration: both OIDC and legacy authentication are disabled. " +
          "At least one must be enabled. Set either OIDC_ENABLED=true or LEGACY_AUTH_ENABLED=true in your environment."
      )

end InfrastructureModule
