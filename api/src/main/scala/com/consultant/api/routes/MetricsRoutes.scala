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
package com.consultant.api.routes

import cats.effect.IO
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import com.consultant.api.dto.ErrorResponse
import com.consultant.infrastructure.metrics.MetricsCollector
import io.circe.Codec

/** Endpoint for system metrics monitoring */
class MetricsRoutes(metricsCollector: MetricsCollector):

  // DTOs for metrics
  case class RequestMetricsDto(
    totalRequests: Long,
    successfulRequests: Long,
    failedRequests: Long,
    totalResponseTime: Long,
    avgResponseTime: Double,
    successRate: Double
  ) derives Codec.AsObject

  case class ServiceMetricsDto(
    activeConnections: Int,
    queuedRequests: Int,
    cacheHits: Long,
    cacheMisses: Long,
    cacheHitRate: Double
  ) derives Codec.AsObject

  case class SystemMetricsDto(
    requests: RequestMetricsDto,
    service: ServiceMetricsDto
  ) derives Codec.AsObject

  private val baseEndpoint = endpoint
    .in("metrics")
    .errorOut(statusCode(sttp.model.StatusCode.InternalServerError))

  // GET /metrics - get all metrics
  val getMetricsEndpoint = baseEndpoint.get
    .out(jsonBody[SystemMetricsDto])
    .description("Get system metrics")

  def getMetricsRoute = getMetricsEndpoint.serverLogic { _ =>
    (for
      reqMetrics <- metricsCollector.getRequestMetrics
      svcMetrics <- metricsCollector.getServiceMetrics

      requestDto = RequestMetricsDto(
        totalRequests = reqMetrics.totalRequests,
        successfulRequests = reqMetrics.successfulRequests,
        failedRequests = reqMetrics.failedRequests,
        totalResponseTime = reqMetrics.totalResponseTime,
        avgResponseTime = reqMetrics.avgResponseTime,
        successRate =
          if reqMetrics.totalRequests > 0 then reqMetrics.successfulRequests.toDouble / reqMetrics.totalRequests * 100
          else 0.0
      )

      serviceDto = ServiceMetricsDto(
        activeConnections = svcMetrics.activeConnections,
        queuedRequests = svcMetrics.queuedRequests,
        cacheHits = svcMetrics.cacheHits,
        cacheMisses = svcMetrics.cacheMisses,
        cacheHitRate = svcMetrics.cacheHitRate
      )

      result = SystemMetricsDto(requestDto, serviceDto)
    yield result).attempt.map {
      case Right(metrics) => Right(metrics)
      case Left(error)    => Left(())
    }
  }

  val routes = List(getMetricsRoute)
