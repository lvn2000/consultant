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

/** Configuration validation errors. */
enum ConfigError:
  case Missing(field: String)
  case InvalidFormat(field: String, value: String, expected: String)
  case InvalidValue(field: String, value: String, reason: String)
  case DependencyError(field: String, dependsOn: String)
  case Multiple(errors: List[ConfigError])

object ConfigError:
  def fromValidatedList(errors: List[ConfigError]): ConfigError =
    errors match
      case Nil           => Multiple(Nil)
      case single :: Nil => single
      case multiple      => Multiple(multiple)

/** Validates configuration values. */
object ConfigValidation:

  /** Validates that a string is not empty. */
  def nonEmpty(value: String, field: String): Validated[ConfigError, String] =
    if value.trim.nonEmpty then Valid(value)
    else Invalid(ConfigError.Missing(field))

  /** Validates that an optional value is present in production. */
  def requiredInProduction(
    value: Option[String],
    field: String,
    env: Environment
  ): Validated[ConfigError, Option[String]] =
    (value, env) match
      case (None, Environment.Production) => Invalid(ConfigError.Missing(field))
      case _                              => Valid(value)

  /** Validates that a port number is in valid range. */
  def validPort(port: Int, field: String): Validated[ConfigError, Int] =
    if port > 0 && port <= 65535 then Valid(port)
    else Invalid(ConfigError.InvalidValue(field, port.toString, "Must be between 1 and 65535"))

  /** Validates that a pool size is reasonable. */
  def validPoolSize(size: Int, field: String): Validated[ConfigError, Int] =
    if size >= 1 && size <= 500 then Valid(size)
    else Invalid(ConfigError.InvalidValue(field, size.toString, "Must be between 1 and 500"))

  /** Validates that a duration is positive. */
  def positiveDuration(
    duration: scala.concurrent.duration.FiniteDuration,
    field: String
  ): Validated[ConfigError, scala.concurrent.duration.FiniteDuration] =
    if duration > scala.concurrent.duration.Duration.Zero then Valid(duration)
    else Invalid(ConfigError.InvalidValue(field, duration.toString, "Must be positive"))

  /** Validates that a JWT secret is strong enough. */
  def strongJwtSecret(secret: String, field: String): Validated[ConfigError, String] =
    if secret.length >= 32 then Valid(secret)
    else Invalid(ConfigError.InvalidValue(field, "***", "Must be at least 32 characters"))

  /** Validates URL format (basic check). */
  def validUrl(url: String, field: String): Validated[ConfigError, String] =
    if url.startsWith("http://") || url.startsWith("https://") || url.startsWith("jdbc:") then Valid(url)
    else Invalid(ConfigError.InvalidFormat(field, url, "Must start with http://, https://, or jdbc:"))

  /** Combines multiple validations. */
  def validateAll(
    validations: Validated[ConfigError, Any]*
  ): Validated[ConfigError, Unit] =
    // Collect all errors manually since Validated needs a Semigroup for errors
    val errors = validations.collect { case Invalid(e) => e }.toList
    errors match
      case Nil           => Valid(())
      case single :: Nil => Invalid(single)
      case multiple      => Invalid(ConfigError.Multiple(multiple))

  extension [A](validated: Validated[ConfigError, A])
    /** Converts validation result to Either with a combined error message. */
    def toEither: Either[String, A] = validated match
      case Valid(a)   => Right(a)
      case Invalid(e) => Left(formatError(e))

  private def formatError(error: ConfigError): String = error match
    case ConfigError.Missing(field) =>
      s"Missing required configuration: $field"
    case ConfigError.InvalidFormat(field, value, expected) =>
      s"Invalid format for $field: '$value' (expected: $expected)"
    case ConfigError.InvalidValue(field, value, reason) =>
      s"Invalid value for $field: '$value' ($reason)"
    case ConfigError.DependencyError(field, dependsOn) =>
      s"Configuration error: $field requires $dependsOn to be set"
    case ConfigError.Multiple(errors) =>
      errors.map(formatError).mkString("; ")

end ConfigValidation
