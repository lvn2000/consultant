package com.consultant.infrastructure.config

import cats.effect.IO
import cats.syntax.all.*
import ciris.*
import scala.concurrent.duration.*

/**
 * Environment-specific configuration loaders.
 *
 * Provides different default configurations for development, staging, and production environments. Each environment can
 * override specific values while maintaining a consistent structure.
 */
object Configs:

  /** Loads configuration for the current environment. */
  def loadForEnvironment(env: Environment = Environment.current): IO[AppConfig] =
    env match
      case Environment.Development => loadDevelopmentConfig
      case Environment.Staging     => loadStagingConfig
      case Environment.Production  => loadProductionConfig

  /** Development configuration with permissive defaults. */
  private def loadDevelopmentConfig: IO[AppConfig] =
    loadConfigWithDefaults(
      serverDefaults = ServerDefaults(
        host = "0.0.0.0",
        port = 8090
      ),
      databaseDefaults = DatabaseDefaults(
        driver = "org.postgresql.Driver",
        url = "jdbc:postgresql://localhost:5432/consultant",
        user = "consultant_user",
        password = "consultant_pass",
        poolSize = 10
      ),
      jwtDefaults = JwtDefaults(
        issuer = "consultant-api-dev",
        accessTtl = "15m",
        refreshTtl = "7d"
      ),
      securityDefaults = SecurityDefaults(
        maxFailedLoginAttempts = 5,
        accountLockDuration = "15m"
      ),
      oidcDefaults = OidcDefaults(
        enabled = false,
        allowedAlgs = "RS256",
        jwksCacheSeconds = 600L,
        jwksTimeoutSeconds = 10L
      ),
      awsDefaults = AwsDefaults(
        useAws = false,
        region = "us-east-1",
        s3BucketName = "consultant-files-dev",
        sqsQueuePrefix = "consultant-dev",
        senderEmail = "[email protected]"
      ),
      storageDefaults = StorageDefaults(
        localBasePath = "./storage"
      ),
      legacyAuthEnabled = true
    )

  /** Staging configuration with production-like settings but relaxed constraints. */
  private def loadStagingConfig: IO[AppConfig] =
    loadConfigWithDefaults(
      serverDefaults = ServerDefaults(
        host = "0.0.0.0",
        port = 8090
      ),
      databaseDefaults = DatabaseDefaults(
        driver = "org.postgresql.Driver",
        url = "jdbc:postgresql://localhost:5432/consultant",
        user = "consultant_user",
        password = "consultant_pass",
        poolSize = 20
      ),
      jwtDefaults = JwtDefaults(
        issuer = "consultant-api-staging",
        accessTtl = "15m",
        refreshTtl = "7d"
      ),
      securityDefaults = SecurityDefaults(
        maxFailedLoginAttempts = 5,
        accountLockDuration = "15m"
      ),
      oidcDefaults = OidcDefaults(
        enabled = false,
        allowedAlgs = "RS256",
        jwksCacheSeconds = 600L,
        jwksTimeoutSeconds = 10L
      ),
      awsDefaults = AwsDefaults(
        useAws = false,
        region = "us-east-1",
        s3BucketName = "consultant-files-staging",
        sqsQueuePrefix = "consultant-staging",
        senderEmail = "[email protected]"
      ),
      storageDefaults = StorageDefaults(
        localBasePath = "./storage"
      ),
      legacyAuthEnabled = true
    )

  /** Production configuration with strict validation and no unsafe defaults. */
  private def loadProductionConfig: IO[AppConfig] =
    // In production, we require explicit configuration for sensitive values
    val baseConfig = (
      env("SERVER_HOST").as[String].default("0.0.0.0"),
      env("SERVER_PORT").as[Int].default(8090),
      env("DB_DRIVER").as[String].default("org.postgresql.Driver"),
      env("DB_URL").as[String],
      env("DB_USER").as[String],
      env("DB_PASSWORD").as[String],
      env("DB_POOL_SIZE").as[Int].default(32),
      env("USE_AWS").as[Boolean].default(false),
      env("AWS_REGION").as[String].option,
      env("AWS_S3_BUCKET").as[String].option,
      env("AWS_SQS_QUEUE_PREFIX").as[String].option,
      env("AWS_SENDER_EMAIL").as[String].option,
      env("LOCAL_STORAGE_PATH").as[String].option
    ).parMapN {
      (host, port, driver, url, user, pass, poolSize, useAws, region, bucket, queuePrefix, email, localPath) =>
        val awsConfig =
          if useAws then
            Some(
              AwsConfig(
                region = region.getOrElse("us-east-1"),
                s3BucketName = bucket.getOrElse("consultant-files"),
                sqsQueuePrefix = queuePrefix.getOrElse("consultant"),
                senderEmail = email.getOrElse("[email protected]")
              )
            )
          else None

        BaseConfig(
          ServerConfig(host, port),
          DatabaseConfig(driver, url, user, pass, poolSize),
          awsConfig,
          StorageConfig(useAws, localPath)
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
        val allowedAlgs = oidcAllowedAlgs.split(",").map(_.trim).filter(_.nonEmpty).toList.distinct

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

    val legacyAuthEnabled = env("LEGACY_AUTH_ENABLED").as[Boolean].default(false)

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
    }.load[IO].flatMap { config =>
      // Validate production configuration
      ConfigValidator.validate(config, Environment.Production) match
        case Right(_) => IO.pure(config)
        case Left(errors) =>
          IO.raiseError(new RuntimeException(s"Configuration validation failed: $errors"))
    }

  /** Generic config loader with environment-specific defaults. */
  private def loadConfigWithDefaults(
    serverDefaults: ServerDefaults,
    databaseDefaults: DatabaseDefaults,
    jwtDefaults: JwtDefaults,
    securityDefaults: SecurityDefaults,
    oidcDefaults: OidcDefaults,
    awsDefaults: AwsDefaults,
    storageDefaults: StorageDefaults,
    legacyAuthEnabled: Boolean
  ): IO[AppConfig] =
    val baseConfig = (
      env("SERVER_HOST").as[String].default(serverDefaults.host),
      env("SERVER_PORT").as[Int].default(serverDefaults.port),
      env("DB_DRIVER").as[String].default(databaseDefaults.driver),
      env("DB_URL").as[String].default(databaseDefaults.url),
      env("DB_USER").as[String].default(databaseDefaults.user),
      env("DB_PASSWORD").as[String].default(databaseDefaults.password),
      env("DB_POOL_SIZE").as[Int].default(databaseDefaults.poolSize),
      env("USE_AWS").as[Boolean].default(awsDefaults.useAws),
      env("AWS_REGION").as[String].default(awsDefaults.region).option,
      env("AWS_S3_BUCKET").as[String].default(awsDefaults.s3BucketName).option,
      env("AWS_SQS_QUEUE_PREFIX").as[String].default(awsDefaults.sqsQueuePrefix).option,
      env("AWS_SENDER_EMAIL").as[String].default(awsDefaults.senderEmail).option,
      env("LOCAL_STORAGE_PATH").as[String].default(storageDefaults.localBasePath).option
    ).parMapN {
      (host, port, driver, url, user, pass, poolSize, useAws, region, bucket, queuePrefix, email, localPath) =>
        val awsConfig =
          if useAws then
            Some(
              AwsConfig(
                region = region.getOrElse(awsDefaults.region),
                s3BucketName = bucket.getOrElse(awsDefaults.s3BucketName),
                sqsQueuePrefix = queuePrefix.getOrElse(awsDefaults.sqsQueuePrefix),
                senderEmail = email.getOrElse(awsDefaults.senderEmail)
              )
            )
          else None

        BaseConfig(
          ServerConfig(host, port),
          DatabaseConfig(driver, url, user, pass, poolSize),
          awsConfig,
          StorageConfig(useAws, localPath)
        )
    }

    val oidcConfig = (
      env("OIDC_ENABLED").as[Boolean].default(oidcDefaults.enabled),
      env("OIDC_ISSUER").as[String].option,
      env("OIDC_JWKS_URI").as[String].option,
      env("OIDC_AUDIENCE").as[String].option,
      env("OIDC_ALLOWED_ALGS").as[String].default(oidcDefaults.allowedAlgs),
      env("OIDC_JWKS_CACHE_SECONDS").as[Long].default(oidcDefaults.jwksCacheSeconds),
      env("OIDC_JWKS_TIMEOUT_SECONDS").as[Long].default(oidcDefaults.jwksTimeoutSeconds)
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
        val allowedAlgs = oidcAllowedAlgs.split(",").map(_.trim).filter(_.nonEmpty).toList.distinct

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
      env("JWT_SECRET").as[String].default("dev-secret-do-not-use-in-production-32-chars"),
      env("JWT_ISSUER").as[String].default(jwtDefaults.issuer),
      env("JWT_ACCESS_TTL").as[String].default(jwtDefaults.accessTtl),
      env("JWT_REFRESH_TTL").as[String].default(jwtDefaults.refreshTtl)
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
      env("SECURITY_MAX_FAILED_LOGIN_ATTEMPTS").as[Int].default(securityDefaults.maxFailedLoginAttempts),
      env("SECURITY_ACCOUNT_LOCK_DURATION").as[String].default(securityDefaults.accountLockDuration)
    ).parMapN { (maxAttempts, lockDuration) =>
      val lockDur = parseDuration(lockDuration, "SECURITY_ACCOUNT_LOCK_DURATION")
      SecurityConfig(
        maxFailedLoginAttempts = maxAttempts,
        accountLockDuration = lockDur
      )
    }

    val legacyAuth = env("LEGACY_AUTH_ENABLED").as[Boolean].default(legacyAuthEnabled)

    (baseConfig, oidcConfig, jwtConfig, securityConfig, legacyAuth).parMapN { (base, oidc, jwt, security, legacy) =>
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

  // Default configuration case classes
  private case class ServerDefaults(host: String, port: Int)
  private case class DatabaseDefaults(driver: String, url: String, user: String, password: String, poolSize: Int)
  private case class JwtDefaults(issuer: String, accessTtl: String, refreshTtl: String)
  private case class SecurityDefaults(maxFailedLoginAttempts: Int, accountLockDuration: String)
  private case class OidcDefaults(
    enabled: Boolean,
    allowedAlgs: String,
    jwksCacheSeconds: Long,
    jwksTimeoutSeconds: Long
  )
  private case class AwsDefaults(
    useAws: Boolean,
    region: String,
    s3BucketName: String,
    sqsQueuePrefix: String,
    senderEmail: String
  )
  private case class StorageDefaults(localBasePath: String)
  private case class BaseConfig(
    server: ServerConfig,
    database: DatabaseConfig,
    aws: Option[AwsConfig],
    storage: StorageConfig
  )

end Configs
