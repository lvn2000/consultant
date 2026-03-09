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
package com.consultant.infrastructure.config

import cats.data.Validated
import cats.data.Validated.{ Invalid, Valid }
import cats.syntax.all.*
import scala.concurrent.duration.*

/** Validates complete AppConfig for a specific environment. */
object ConfigValidator:

  /**
   * Validates the configuration for the given environment.
   *
   * @param config
   *   The configuration to validate
   * @param env
   *   The target environment
   * @return
   *   Either a list of validation errors or Unit if valid
   */
  def validate(config: AppConfig, env: Environment): Either[String, Unit] =
    val validations = List(
      validateServer(config.server, env),
      validateDatabase(config.database, env),
      validateJwt(config.jwt, env),
      validateSecurity(config.security),
      validateOidc(config.oidc),
      validateAws(config.aws, config.storage),
      validateStorage(config.storage)
    )

    // Accumulate all errors
    val errors = validations.collect { case Invalid(e) => e }
    if errors.isEmpty then Right(())
    else Left(formatErrors(errors))

  private def formatErrors(errors: List[ConfigError]): String =
    errors.map(formatSingleError).mkString("; ")

  private def formatSingleError(error: ConfigError): String = error match
    case ConfigError.Missing(field) =>
      s"Missing required configuration: $field"
    case ConfigError.InvalidFormat(field, value, expected) =>
      s"Invalid format for $field: '$value' (expected: $expected)"
    case ConfigError.InvalidValue(field, value, reason) =>
      s"Invalid value for $field: '$value' ($reason)"
    case ConfigError.DependencyError(field, dependsOn) =>
      s"Configuration error: $field requires $dependsOn to be set"
    case ConfigError.Multiple(errs) =>
      errs.map(formatSingleError).mkString("; ")

  private def validateServer(config: ServerConfig, env: Environment): Validated[ConfigError, Unit] =
    import ConfigValidation.*
    validateAll(
      validPort(config.port, "SERVER_PORT"),
      if env == Environment.Production then nonEmpty(config.host, "SERVER_HOST")
      else Valid(())
    )

  private def validateDatabase(config: DatabaseConfig, env: Environment): Validated[ConfigError, Unit] =
    import ConfigValidation.*
    validateAll(
      nonEmpty(config.driver, "DB_DRIVER"),
      validUrl(config.url, "DB_URL"),
      nonEmpty(config.user, "DB_USER"),
      nonEmpty(config.password, "DB_PASSWORD"),
      validPoolSize(config.poolSize, "DB_POOL_SIZE")
    )

  private def validateJwt(config: JwtConfig, env: Environment): Validated[ConfigError, Unit] =
    import ConfigValidation.*
    validateAll(
      if env == Environment.Production then strongJwtSecret(config.secret, "JWT_SECRET")
      else Valid(config.secret),
      nonEmpty(config.issuer, "JWT_ISSUER"),
      positiveDuration(config.accessTtl, "JWT_ACCESS_TTL"),
      positiveDuration(config.refreshTtl, "JWT_REFRESH_TTL")
    )

  private def validateSecurity(config: SecurityConfig): Validated[ConfigError, Unit] =
    import ConfigValidation.*
    validateAll(
      Validated.cond(
        config.maxFailedLoginAttempts > 0,
        (),
        ConfigError.InvalidValue(
          "SECURITY_MAX_FAILED_LOGIN_ATTEMPTS",
          config.maxFailedLoginAttempts.toString,
          "Must be positive"
        )
      ),
      positiveDuration(config.accountLockDuration, "SECURITY_ACCOUNT_LOCK_DURATION")
    )

  private def validateOidc(config: OidcConfig): Validated[ConfigError, Unit] =
    if config.enabled then
      val issuerV = config.issuer.fold[Validated[ConfigError, Unit]](
        Invalid(ConfigError.Missing("OIDC_ISSUER (required when OIDC_ENABLED=true)"))
      )(_ => Valid(()))
      val jwksV = config.jwksUri.fold[Validated[ConfigError, Unit]](
        Invalid(ConfigError.Missing("OIDC_JWKS_URI (required when OIDC_ENABLED=true)"))
      )(_ => Valid(()))
      val algsV = Validated.cond(
        config.allowedAlgs.nonEmpty,
        (),
        ConfigError.InvalidValue("OIDC_ALLOWED_ALGS", "", "Must specify at least one algorithm")
      )
      // Collect errors manually
      val errors = List(issuerV, jwksV, algsV).collect { case Invalid(e) => e }
      errors match
        case Nil           => Valid(())
        case single :: Nil => Invalid(single)
        case multiple      => Invalid(ConfigError.Multiple(multiple))
    else Valid(())

  private def validateAws(awsConfig: Option[AwsConfig], storage: StorageConfig): Validated[ConfigError, Unit] =
    (awsConfig, storage.useAws) match
      case (None, true) =>
        Invalid(ConfigError.DependencyError("STORAGE_USE_AWS", "AWS configuration must be provided when USE_AWS=true"))
      case _ =>
        Valid(())

  private def validateStorage(config: StorageConfig): Validated[ConfigError, Unit] =
    if !config.useAws then
      config.localBasePath.fold[Validated[ConfigError, Unit]](
        Invalid(ConfigError.Missing("LOCAL_STORAGE_PATH (required when USE_AWS=false)"))
      )(_ => Valid(()))
    else Valid(())

end ConfigValidator
