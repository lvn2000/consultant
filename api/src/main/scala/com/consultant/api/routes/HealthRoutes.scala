package com.consultant.api.routes

import cats.effect.IO
import cats.syntax.all.*
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import io.circe.Codec
import doobie.util.transactor.Transactor
import doobie.implicits.*
import scala.concurrent.duration.*

/** Health check endpoint for Kubernetes/Docker */
class HealthRoutes(xa: Option[Transactor[IO]] = None):

  case class HealthStatus(
    status: String,
    service: String,
    version: String
  ) derives Codec.AsObject

  case class ComponentHealth(
    name: String,
    status: String,
    responseTimeMs: Option[Long] = None,
    error: Option[String] = None
  ) derives Codec.AsObject

  case class DeepHealthStatus(
    status: String,
    service: String,
    version: String,
    timestamp: String,
    components: List[ComponentHealth]
  ) derives Codec.AsObject

  private val baseEndpoint = endpoint
    .in("health")

  // GET /health - basic health check
  val healthEndpoint = baseEndpoint.get
    .out(jsonBody[HealthStatus])
    .description("Basic health check endpoint")

  // GET /health/deep - deep health check with dependency verification
  val deepHealthEndpoint = baseEndpoint.get
    .in("deep")
    .out(jsonBody[DeepHealthStatus])
    .description("Deep health check with dependency verification")

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

  def deepHealthRoute = deepHealthEndpoint.serverLogic { _ =>
    val startTime = System.nanoTime()

    // Check database connectivity
    val dbCheck = xa match
      case Some(transactor) =>
        val dbStart = System.nanoTime()
        sql"SELECT 1"
          .query[Int]
          .unique
          .transact(transactor)
          .map(_ =>
            val duration = (System.nanoTime() - dbStart).nanos.toMillis
            ComponentHealth("database", "UP", Some(duration), None)
          )
          .handleError { error =>
            val duration = (System.nanoTime() - dbStart).nanos.toMillis
            ComponentHealth("database", "DOWN", Some(duration), Some(error.getMessage))
          }
      case None =>
        IO.pure(ComponentHealth("database", "UNKNOWN", None, Some("No transactor configured")))

    // Check JVM memory
    val memoryCheck = IO {
      val runtime     = Runtime.getRuntime
      val maxMemory   = runtime.maxMemory()
      val totalMemory = runtime.totalMemory()
      val freeMemory  = runtime.freeMemory()
      val usedMemory  = totalMemory - freeMemory
      val memoryUsage = (usedMemory.toDouble / maxMemory.toDouble * 100).toInt

      val status = if memoryUsage > 90 then "WARNING" else "UP"
      ComponentHealth(
        "memory",
        status,
        None,
        if status == "WARNING" then Some(s"Memory usage at $memoryUsage%") else None
      )
    }

    // Combine all checks
    (dbCheck, memoryCheck).mapN { (db, memory) =>
      val components    = List(db, memory)
      val overallStatus = if components.exists(_.status == "DOWN") then "DOWN" else "UP"

      Right(
        DeepHealthStatus(
          status = overallStatus,
          service = "consultant-api",
          version = "0.1.0",
          timestamp = java.time.Instant.now().toString,
          components = components
        )
      )
    }
  }

  val routes = List(healthRoute, deepHealthRoute)
