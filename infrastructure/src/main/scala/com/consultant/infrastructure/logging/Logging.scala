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
