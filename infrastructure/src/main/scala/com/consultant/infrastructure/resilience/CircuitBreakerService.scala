package com.consultant.infrastructure.resilience

import cats.effect.{ IO, Ref }
import cats.syntax.all.*
import scala.concurrent.duration.*

/** Circuit Breaker pattern для защиты от каскадных сбоев */
class CircuitBreakerService(
  maxFailures: Int = 5,
  resetTimeout: FiniteDuration = 30.seconds,
  halfOpenMaxCalls: Int = 3
):

  enum State:
    case Closed   // Нормальная работа
    case Open     // Блокировка вызовов
    case HalfOpen // Пробный режим

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
          // Проверяем, не пора ли перейти в HalfOpen
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
