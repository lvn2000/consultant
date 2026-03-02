package com.consultant.infrastructure.config

/** Represents the application runtime environment. */
enum Environment:
  case Development, Staging, Production

object Environment:
  /** Detects the current environment from ENV variable or defaults to Development. */
  def current: Environment =
    sys.env.get("APP_ENV").map(_.toLowerCase) match
      case Some("production") | Some("prod") => Production
      case Some("staging") | Some("stage")   => Staging
      case _                                 => Development

  /** Returns true if running in production. */
  def isProduction: Boolean = current == Production

  /** Returns true if running in staging. */
  def isStaging: Boolean = current == Staging

  /** Returns true if running in development. */
  def isDevelopment: Boolean = current == Development
