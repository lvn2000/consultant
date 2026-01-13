package com.consultant.api.routes

import cats.effect.IO
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import io.circe.Codec

/** Health check эндпоинт для Kubernetes/Docker */
class HealthRoutes:

  case class HealthStatus(
    status: String,
    service: String,
    version: String
  ) derives Codec.AsObject

  private val baseEndpoint = endpoint
    .in("health")

  // GET /health - проверка работоспособности
  val healthEndpoint = baseEndpoint.get
    .out(jsonBody[HealthStatus])
    .description("Health check endpoint")

  def healthRoute = healthEndpoint.serverLogic { _ =>
    IO.pure(
      Right(
        HealthStatus(
          status = "UP",
          service = "consultant-api",
          version = "0.1.0"
        )
      )
    )
  }

  val routes = List(healthRoute)
