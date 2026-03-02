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
    else throw new RuntimeException("Both OIDC and legacy auth are disabled")

end InfrastructureModule
