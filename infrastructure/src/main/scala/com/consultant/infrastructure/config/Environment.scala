package com.consultant.infrastructure.config

/** Represents the application runtime environment. */
enum Environment:
  case Development, Staging, Production

object Environment:
  /** Detects the current environment from ENV variable or defaults to Development. */
  @volatile private var detected: Environment =
    sys.env.get("APP_ENV").map(_.toLowerCase) match
      case Some("production") | Some("prod") => Production
      case Some("staging") | Some("stage")   => Staging
      case _                                 => Development

  /** Returns the detected environment (evaluated once at startup). */
  def current: Environment = detected

  /** Returns true if running in production. */
  def isProduction: Boolean = detected == Production

  /** Returns true if running in staging. */
  def isStaging: Boolean = detected == Staging

  /** Returns true if running in development. */
  def isDevelopment: Boolean = detected == Development

  /** Allows overriding the detected environment for testing purposes. */
  def setForTesting(env: Environment): Unit =
    detected = env
