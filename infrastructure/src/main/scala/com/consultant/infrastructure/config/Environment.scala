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
