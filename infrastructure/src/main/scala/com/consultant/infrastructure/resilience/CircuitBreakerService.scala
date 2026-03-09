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
package com.consultant.infrastructure.resilience

import cats.effect.{ IO, Ref }
import cats.syntax.all.*
import scala.concurrent.duration.*

/** Circuit Breaker pattern for protection from cascading failures */
class CircuitBreakerService(
  maxFailures: Int = 5,
  resetTimeout: FiniteDuration = 30.seconds,
  halfOpenMaxCalls: Int = 3
):

  enum State:
    case Closed   // Normal operation
    case Open     // Block calls
    case HalfOpen // Test mode

  case class CircuitState(
    state: State,
    failures: Int,
    lastFailureTime: Option[Long],
    halfOpenCalls: Int
  )

  private val stateRef: Ref[IO, CircuitState] = Ref.unsafe(
    CircuitState(State.Closed, 0, None, 0)
  )

  def protect[A](action: IO[A]): IO[A] =
    for
      currentState <- stateRef.get
      result <- currentState.state match
        case State.Open =>
          // Check if it's time to transition to HalfOpen
          currentState.lastFailureTime match
            case Some(time) if System.currentTimeMillis() - time >= resetTimeout.toMillis =>
              stateRef.set(CircuitState(State.HalfOpen, 0, None, 0)) *>
                attemptCall(action)
            case _ =>
              IO.raiseError(new RuntimeException("Circuit breaker is OPEN"))

        case State.HalfOpen =>
          if currentState.halfOpenCalls >= halfOpenMaxCalls then
            IO.raiseError(new RuntimeException("Circuit breaker half-open limit reached"))
          else attemptCall(action)

        case State.Closed =>
          attemptCall(action)
    yield result

  private def attemptCall[A](action: IO[A]): IO[A] =
    action.attempt.flatMap {
      case Right(value) =>
        onSuccess() *> IO.pure(value)
      case Left(error) =>
        onFailure() *> IO.raiseError(error)
    }

  private def onSuccess(): IO[Unit] =
    stateRef.update { state =>
      state.state match
        case State.HalfOpen =>
          if state.halfOpenCalls + 1 >= halfOpenMaxCalls then CircuitState(State.Closed, 0, None, 0)
          else state.copy(halfOpenCalls = state.halfOpenCalls + 1)
        case State.Closed =>
          state.copy(failures = 0)
        case State.Open =>
          state
    }

  private def onFailure(): IO[Unit] =
    stateRef.update { state =>
      val newFailures = state.failures + 1
      if newFailures >= maxFailures then CircuitState(State.Open, newFailures, Some(System.currentTimeMillis()), 0)
      else state.copy(failures = newFailures)
    }

  def getState: IO[State] = stateRef.get.map(_.state)
