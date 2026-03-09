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
package com.consultant.infrastructure.observability

import cats.effect.IO
import cats.syntax.all.*
import org.typelevel.log4cats.SelfAwareStructuredLogger
import com.consultant.infrastructure.logging.{ LogContext, LoggingSyntax }
import scala.concurrent.duration.*

/**
 * Provides automatic tracing for service calls.
 *
 * This trait wraps service operations with logging and timing for observability.
 */
trait TracedService:

  protected given logger: SelfAwareStructuredLogger[IO]

  import LoggingSyntax.*

  /**
   * Traces an operation with timing and logging.
   *
   * @param name
   *   The name of the operation being traced
   * @param ctx
   *   Additional context for the trace
   * @param fa
   *   The operation to trace
   * @return
   *   The result of the operation
   */
  def trace[A](name: String, ctx: LogContext = LogContext.empty)(fa: IO[A]): IO[A] =
    val traceCtx = ctx.withKeyValue("operation", name)

    IO.monotonic.flatMap { startTime =>
      logger.debugWithContext(traceCtx)(s"Starting operation: $name") >>
        fa.flatTap { result =>
          IO.monotonic.flatMap { endTime =>
            val duration = (endTime - startTime).toMillis
            val successCtx = traceCtx
              .withKeyValue("duration_ms", duration.toString)
              .withKeyValue("status", "success")
            logger.infoWithContext(successCtx)(s"Completed operation: $name in ${duration}ms")
          }
        }.handleErrorWith { error =>
          IO.monotonic.flatMap { endTime =>
            val duration = (endTime - startTime).toMillis
            val errorCtx = traceCtx
              .withKeyValue("duration_ms", duration.toString)
              .withKeyValue("status", "error")
              .withKeyValue("error_type", error.getClass.getSimpleName)
            logger.errorWithContext(errorCtx, error)(s"Failed operation: $name after ${duration}ms") >>
              IO.raiseError(error)
          }
        }
    }

  /**
   * Traces an operation that returns an Either.
   *
   * @param name
   *   The name of the operation being traced
   * @param ctx
   *   Additional context for the trace
   * @param fa
   *   The operation to trace
   * @return
   *   The result of the operation
   */
  def traceEither[E, A](name: String, ctx: LogContext = LogContext.empty)(
    fa: IO[Either[E, A]]
  )(using ev: E <:< Throwable): IO[Either[E, A]] =
    val traceCtx = ctx.withKeyValue("operation", name)

    IO.monotonic.flatMap { startTime =>
      logger.debugWithContext(traceCtx)(s"Starting operation: $name") >>
        fa.flatTap {
          case Right(result) =>
            IO.monotonic.flatMap { endTime =>
              val duration = (endTime - startTime).toMillis
              val successCtx = traceCtx
                .withKeyValue("duration_ms", duration.toString)
                .withKeyValue("status", "success")
              logger.infoWithContext(successCtx)(s"Completed operation: $name in ${duration}ms")
            }
          case Left(error) =>
            IO.monotonic.flatMap { endTime =>
              val duration = (endTime - startTime).toMillis
              val errorCtx = traceCtx
                .withKeyValue("duration_ms", duration.toString)
                .withKeyValue("status", "failure")
                .withKeyValue("error_type", error.getClass.getSimpleName)
              logger.warnWithContext(errorCtx)(s"Operation $name returned error after ${duration}ms")
            }
        }.handleErrorWith { error =>
          IO.monotonic.flatMap { endTime =>
            val duration = (endTime - startTime).toMillis
            val errorCtx = traceCtx
              .withKeyValue("duration_ms", duration.toString)
              .withKeyValue("status", "exception")
              .withKeyValue("error_type", error.getClass.getSimpleName)
            logger.errorWithContext(errorCtx, error)(s"Operation $name threw exception after ${duration}ms") >>
              IO.raiseError(error)
          }
        }
    }

end TracedService
