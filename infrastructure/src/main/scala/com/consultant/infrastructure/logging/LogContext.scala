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
import cats.syntax.all.*
import org.typelevel.log4cats.SelfAwareStructuredLogger

/**
 * Represents a logging context with key-value pairs.
 *
 * Context can be added to log entries for structured logging.
 */
opaque type LogContext = Map[String, String]

object LogContext:

  /** Creates an empty context. */
  def empty: LogContext = Map.empty

  /** Creates a context from key-value pairs. */
  def apply(pairs: (String, String)*): LogContext = Map(pairs*)

  /** Creates a context with a correlation ID for request tracing. */
  def withCorrelationId(correlationId: String): LogContext =
    Map("correlationId" -> correlationId)

  /** Creates a context with user information. */
  def withUser(userId: String, role: String): LogContext =
    Map("userId" -> userId, "userRole" -> role)

  /** Creates a context with request information. */
  def withRequest(method: String, path: String): LogContext =
    Map("requestMethod" -> method, "requestPath" -> path)

  extension (ctx: LogContext)
    /** Adds a key-value pair to the context. */
    def withKeyValue(key: String, value: String): LogContext =
      ctx + (key -> value)

    /** Adds multiple key-value pairs to the context. */
    def withValues(pairs: (String, String)*): LogContext =
      ctx ++ pairs

    /** Merges another context into this one. */
    def merge(other: LogContext): LogContext =
      ctx ++ other

    /** Converts to the Map representation for log4cats. */
    def toMap: Map[String, String] = ctx

end LogContext

/** Extension methods for structured logging with context. */
object LoggingSyntax:

  extension (logger: SelfAwareStructuredLogger[IO])
    /** Logs a trace message with context. */
    def traceWithContext(ctx: LogContext)(msg: => String): IO[Unit] =
      logger.trace(ctx.toMap)(msg)

    /** Logs a debug message with context. */
    def debugWithContext(ctx: LogContext)(msg: => String): IO[Unit] =
      logger.debug(ctx.toMap)(msg)

    /** Logs an info message with context. */
    def infoWithContext(ctx: LogContext)(msg: => String): IO[Unit] =
      logger.info(ctx.toMap)(msg)

    /** Logs a warning message with context. */
    def warnWithContext(ctx: LogContext)(msg: => String): IO[Unit] =
      logger.warn(ctx.toMap)(msg)

    /** Logs an error message with context. */
    def errorWithContext(ctx: LogContext)(msg: => String): IO[Unit] =
      logger.error(ctx.toMap)(msg)

    /** Logs an error message with context and exception. */
    def errorWithContext(ctx: LogContext, t: Throwable)(msg: => String): IO[Unit] =
      logger.error(ctx.toMap, t)(msg)

    /** Logs a message with context at the specified level. */
    def logWithContext(level: LogLevel)(ctx: LogContext)(msg: => String): IO[Unit] =
      level match
        case LogLevel.Trace => logger.trace(ctx.toMap)(msg)
        case LogLevel.Debug => logger.debug(ctx.toMap)(msg)
        case LogLevel.Info  => logger.info(ctx.toMap)(msg)
        case LogLevel.Warn  => logger.warn(ctx.toMap)(msg)
        case LogLevel.Error => logger.error(ctx.toMap)(msg)

end LoggingSyntax

/** Log levels for dynamic logging. */
enum LogLevel:
  case Trace, Debug, Info, Warn, Error
