package com.consultant.infrastructure.metrics

import cats.effect.{ IO, Ref }
import cats.syntax.all.*
import scala.concurrent.duration.*

/** Metrics collection for performance monitoring */
class MetricsCollector:

  case class RequestMetrics(
    totalRequests: Long,
    successfulRequests: Long,
    failedRequests: Long,
    totalResponseTime: Long,
    avgResponseTime: Double
  )

  case class ServiceMetrics(
    activeConnections: Int,
    queuedRequests: Int,
    cacheHits: Long,
    cacheMisses: Long,
    cacheHitRate: Double
  )

  private val requestMetricsRef: Ref[IO, RequestMetrics] =
    Ref.unsafe(RequestMetrics(0, 0, 0, 0, 0.0))

  private val serviceMetricsRef: Ref[IO, ServiceMetrics] =
    Ref.unsafe(ServiceMetrics(0, 0, 0, 0, 0.0))

  def recordRequest[A](action: IO[A]): IO[A] =
    for
      start  <- IO.realTime
      result <- action.attempt
      end    <- IO.realTime
      duration = (end - start).toMillis
      _ <- result match
        case Right(_) => recordSuccess(duration)
        case Left(_)  => recordFailure(duration)
      finalResult <- IO.fromEither(result)
    yield finalResult

  private def recordSuccess(responseTime: Long): IO[Unit] =
    requestMetricsRef.update { metrics =>
      val newTotal      = metrics.totalRequests + 1
      val newSuccessful = metrics.successfulRequests + 1
      val newTotalTime  = metrics.totalResponseTime + responseTime
      val newAvg        = newTotalTime.toDouble / newTotal

      metrics.copy(
        totalRequests = newTotal,
        successfulRequests = newSuccessful,
        totalResponseTime = newTotalTime,
        avgResponseTime = newAvg
      )
    }

  private def recordFailure(responseTime: Long): IO[Unit] =
    requestMetricsRef.update { metrics =>
      val newTotal     = metrics.totalRequests + 1
      val newFailed    = metrics.failedRequests + 1
      val newTotalTime = metrics.totalResponseTime + responseTime
      val newAvg       = newTotalTime.toDouble / newTotal

      metrics.copy(
        totalRequests = newTotal,
        failedRequests = newFailed,
        totalResponseTime = newTotalTime,
        avgResponseTime = newAvg
      )
    }

  def recordCacheHit(): IO[Unit] =
    serviceMetricsRef.update { metrics =>
      val newHits = metrics.cacheHits + 1
      val total   = newHits + metrics.cacheMisses
      val hitRate = if total > 0 then newHits.toDouble / total else 0.0
      metrics.copy(cacheHits = newHits, cacheHitRate = hitRate)
    }

  def recordCacheMiss(): IO[Unit] =
    serviceMetricsRef.update { metrics =>
      val newMisses = metrics.cacheMisses + 1
      val total     = metrics.cacheHits + newMisses
      val hitRate   = if total > 0 then metrics.cacheHits.toDouble / total else 0.0
      metrics.copy(cacheMisses = newMisses, cacheHitRate = hitRate)
    }

  def incrementActiveConnections(): IO[Unit] =
    serviceMetricsRef.update(m => m.copy(activeConnections = m.activeConnections + 1))

  def decrementActiveConnections(): IO[Unit] =
    serviceMetricsRef.update(m => m.copy(activeConnections = m.activeConnections - 1))

  def getRequestMetrics: IO[RequestMetrics] = requestMetricsRef.get
  def getServiceMetrics: IO[ServiceMetrics] = serviceMetricsRef.get

  def logMetrics(): IO[Unit] =
    for
      req <- requestMetricsRef.get
      svc <- serviceMetricsRef.get
      _ <- IO.println(s"""
                         |=== Request Metrics ===
                         |Total: ${req.totalRequests}
                         |Success: ${req.successfulRequests}
                         |Failed: ${req.failedRequests}
                         |Avg Response Time: ${req.avgResponseTime}ms
                         |
                         |=== Service Metrics ===
                         |Active Connections: ${svc.activeConnections}
                         |Queued: ${svc.queuedRequests}
                         |Cache Hit Rate: ${f"${svc.cacheHitRate * 100}%.2f"}%
                         |""".stripMargin)
    yield ()
