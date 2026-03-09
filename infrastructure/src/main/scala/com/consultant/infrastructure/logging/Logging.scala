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
package com.consultant.infrastructure.logging

import cats.effect.IO
import org.typelevel.log4cats.{ Logger, SelfAwareStructuredLogger }
import org.typelevel.log4cats.slf4j.Slf4jLogger

/**
 * Provides structured logging capabilities for the application.
 *
 * This object creates a logger instance that can be used across the application for consistent, structured logging with
 * context support.
 */
object Logging:

  /**
   * Creates a logger for the given class type.
   *
   * @tparam T
   *   The class type for which to create the logger
   * @return
   *   A structured logger instance
   */
  def loggerFor[T](using ev: reflect.ClassTag[T]): SelfAwareStructuredLogger[IO] =
    Slf4jLogger.getLoggerFromClass(ev.runtimeClass)

  /**
   * Creates a logger with the given name.
   *
   * @param name
   *   The name for the logger
   * @return
   *   A structured logger instance
   */
  def loggerForName(name: String): SelfAwareStructuredLogger[IO] =
    Slf4jLogger.getLoggerFromName(name)

  /** The default logger instance. */
  lazy val default: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

end Logging

/** Mixin trait for classes that need logging capabilities. */
trait Logging:

  /** Logger instance for this class. */
  protected given logger: SelfAwareStructuredLogger[IO] =
    Slf4jLogger.getLoggerFromClass(this.getClass)

end Logging
