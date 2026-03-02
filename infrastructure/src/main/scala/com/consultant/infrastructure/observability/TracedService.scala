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
    val startTime = System.nanoTime()
    val traceCtx  = ctx.withKeyValue("operation", name)

    logger.debugWithContext(traceCtx)(s"Starting operation: $name") >>
      fa.flatTap { result =>
        val duration = (System.nanoTime() - startTime).nanos.toMillis
        val successCtx = traceCtx
          .withKeyValue("duration_ms", duration.toString)
          .withKeyValue("status", "success")
        logger.infoWithContext(successCtx)(s"Completed operation: $name in ${duration}ms")
      }.handleErrorWith { error =>
        val duration = (System.nanoTime() - startTime).nanos.toMillis
        val errorCtx = traceCtx
          .withKeyValue("duration_ms", duration.toString)
          .withKeyValue("status", "error")
          .withKeyValue("error_type", error.getClass.getSimpleName)
        logger.errorWithContext(errorCtx, error)(s"Failed operation: $name after ${duration}ms") >>
          IO.raiseError(error)
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
    val startTime = System.nanoTime()
    val traceCtx  = ctx.withKeyValue("operation", name)

    logger.debugWithContext(traceCtx)(s"Starting operation: $name") >>
      fa.flatTap {
        case Right(result) =>
          val duration = (System.nanoTime() - startTime).nanos.toMillis
          val successCtx = traceCtx
            .withKeyValue("duration_ms", duration.toString)
            .withKeyValue("status", "success")
          logger.infoWithContext(successCtx)(s"Completed operation: $name in ${duration}ms")
        case Left(error) =>
          val duration = (System.nanoTime() - startTime).nanos.toMillis
          val errorCtx = traceCtx
            .withKeyValue("duration_ms", duration.toString)
            .withKeyValue("status", "failure")
            .withKeyValue("error_type", error.getClass.getSimpleName)
          logger.warnWithContext(errorCtx)(s"Operation $name returned error after ${duration}ms")
      }.handleErrorWith { error =>
        val duration = (System.nanoTime() - startTime).nanos.toMillis
        val errorCtx = traceCtx
          .withKeyValue("duration_ms", duration.toString)
          .withKeyValue("status", "exception")
          .withKeyValue("error_type", error.getClass.getSimpleName)
        logger.errorWithContext(errorCtx, error)(s"Operation $name threw exception after ${duration}ms") >>
          IO.raiseError(error)
      }

end TracedService
