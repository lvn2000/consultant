package com.consultant.infrastructure.config

import cats.effect.IO
import cats.syntax.all.*
import ciris.*
import scala.concurrent.duration.*

case class AppConfig(
  server: ServerConfig,
  database: DatabaseConfig,
  aws: Option[AwsConfig],
  storage: StorageConfig,
  oidc: OidcConfig,
  jwt: JwtConfig,
  security: SecurityConfig,
  legacyAuthEnabled: Boolean
)

case class ServerConfig(
  host: String,
  port: Int
)

case class DatabaseConfig(
  driver: String,
  url: String,
  user: String,
  password: String,
  poolSize: Int
)

case class AwsConfig(
  region: String,
  s3BucketName: String,
  sqsQueuePrefix: String,
  senderEmail: String
)

case class StorageConfig(
  useAws: Boolean,
  localBasePath: Option[String]
)

case class OidcConfig(
  enabled: Boolean,
  issuer: Option[String],
  jwksUri: Option[String],
  audience: Option[String],
  allowedAlgs: List[String],
  jwksCacheSeconds: Long,
  jwksTimeoutSeconds: Long
)

case class JwtConfig(
  secret: String,
  issuer: String,
  accessTtl: FiniteDuration,
  refreshTtl: FiniteDuration
)

case class SecurityConfig(
  maxFailedLoginAttempts: Int,
  accountLockDuration: FiniteDuration
)

object AppConfig:

  private case class BaseConfig(
    server: ServerConfig,
    database: DatabaseConfig,
    aws: Option[AwsConfig],
    storage: StorageConfig
  )

  /**
   * Loads configuration based on the current environment.
   *
   * Uses APP_ENV environment variable to determine which configuration to load:
   *   - production: Strict validation, no unsafe defaults
   *   - staging: Production-like settings with relaxed constraints
   *   - development (default): Permissive defaults for local development
   */
  def load: IO[AppConfig] = Configs.loadForEnvironment()

  /**
   * Loads configuration for a specific environment.
   *
   * @param env
   *   The environment to load configuration for
   */
  def loadFor(env: Environment): IO[AppConfig] = Configs.loadForEnvironment(env)

  /**
   * Legacy load method for backward compatibility.
   * @deprecated
   *   Use loadFor(environment) instead
   */
  @deprecated("Use loadFor(environment) for environment-specific configuration", "2.0")
  def loadLegacy: IO[AppConfig] =
    val baseConfig = (
      env("SERVER_HOST").as[String].default("0.0.0.0"),
      env("SERVER_PORT").as[Int].default(8090),
      env("DB_DRIVER").as[String].default("org.postgresql.Driver"),
      env("DB_URL").as[String].default("jdbc:postgresql://localhost:5432/consultant"),
      env("DB_USER").as[String].default("consultant_user"),
      env("DB_PASSWORD").as[String].default("consultant_pass"),
      env("DB_POOL_SIZE").as[Int].default(32),
      env("USE_AWS").as[Boolean].default(false),
      env("AWS_REGION").as[String].option,
      env("AWS_S3_BUCKET").as[String].option,
      env("AWS_SQS_QUEUE_PREFIX").as[String].option,
      env("AWS_SENDER_EMAIL").as[String].option,
      env("LOCAL_STORAGE_PATH").as[String].option
    ).parMapN {
      (
        host,
        port,
        driver,
        url,
        user,
        pass,
        poolSize,
        useAws,
        region,
        bucket,
        queuePrefix,
        email,
        localPath
      ) =>
        val awsConfig =
          if useAws then
            Some(
              AwsConfig(
                region.getOrElse("us-east-1"),
                bucket.getOrElse("consultant-files"),
                queuePrefix.getOrElse("consultant"),
                email.getOrElse("[email protected]")
              )
            )
          else None

        BaseConfig(
          ServerConfig(host, port),
          DatabaseConfig(driver, url, user, pass, poolSize),
          awsConfig,
          StorageConfig(useAws, localPath.orElse(Some("./storage")))
        )
    }

    val oidcConfig = (
      env("OIDC_ENABLED").as[Boolean].default(false),
      env("OIDC_ISSUER").as[String].option,
      env("OIDC_JWKS_URI").as[String].option,
      env("OIDC_AUDIENCE").as[String].option,
      env("OIDC_ALLOWED_ALGS").as[String].default("RS256"),
      env("OIDC_JWKS_CACHE_SECONDS").as[Long].default(600L),
      env("OIDC_JWKS_TIMEOUT_SECONDS").as[Long].default(10L)
    ).parMapN {
      (
        oidcEnabled,
        oidcIssuer,
        oidcJwksUri,
        oidcAudience,
        oidcAllowedAlgs,
        oidcJwksCacheSeconds,
        oidcJwksTimeoutSeconds
      ) =>
        val allowedAlgs =
          oidcAllowedAlgs
            .split(",")
            .map(_.trim)
            .filter(_.nonEmpty)
            .toList
            .distinct

        OidcConfig(
          enabled = oidcEnabled,
          issuer = oidcIssuer,
          jwksUri = oidcJwksUri,
          audience = oidcAudience,
          allowedAlgs = allowedAlgs,
          jwksCacheSeconds = oidcJwksCacheSeconds,
          jwksTimeoutSeconds = oidcJwksTimeoutSeconds
        )
    }

    val jwtConfig = (
      env("JWT_SECRET").as[String],
      env("JWT_ISSUER").as[String].default("consultant-api"),
      env("JWT_ACCESS_TTL").as[String].default("15m"),
      env("JWT_REFRESH_TTL").as[String].default("7d")
    ).parMapN { (jwtSecret, jwtIssuer, jwtAccessTtl, jwtRefreshTtl) =>
      val accessTtl  = parseDuration(jwtAccessTtl, "JWT_ACCESS_TTL")
      val refreshTtl = parseDuration(jwtRefreshTtl, "JWT_REFRESH_TTL")

      JwtConfig(
        secret = jwtSecret,
        issuer = jwtIssuer,
        accessTtl = accessTtl,
        refreshTtl = refreshTtl
      )
    }

    val securityConfig = (
      env("SECURITY_MAX_FAILED_LOGIN_ATTEMPTS").as[Int].default(5),
      env("SECURITY_ACCOUNT_LOCK_DURATION").as[String].default("15m")
    ).parMapN { (maxAttempts, lockDuration) =>
      val lockDur = parseDuration(lockDuration, "SECURITY_ACCOUNT_LOCK_DURATION")
      SecurityConfig(
        maxFailedLoginAttempts = maxAttempts,
        accountLockDuration = lockDur
      )
    }

    val legacyAuthEnabled = env("LEGACY_AUTH_ENABLED").as[Boolean].default(true)

    (baseConfig, oidcConfig, jwtConfig, securityConfig, legacyAuthEnabled).parMapN {
      (base, oidc, jwt, security, legacy) =>
        AppConfig(
          base.server,
          base.database,
          base.aws,
          base.storage,
          oidc,
          jwt,
          security,
          legacyAuthEnabled = legacy
        )
    }.load[IO]

  private def parseDuration(value: String, name: String): FiniteDuration =
    Duration(value) match
      case fd: FiniteDuration => fd
      case _                  => throw new RuntimeException(s"$name must be a finite duration")
