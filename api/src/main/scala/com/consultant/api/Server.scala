package com.consultant.api

import cats.effect.{ ExitCode, IO, IOApp, Resource }
import cats.syntax.all.*
import com.comcast.ip4s.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.{ Header, HttpRoutes, Method, Request, Response }
import org.http4s.dsl.io.*
import org.http4s.headers.Location
import org.http4s.implicits.*
import org.http4s.server.Router
import org.http4s.server.middleware.CORS
import org.typelevel.ci.CIString
import org.typelevel.log4cats.slf4j.Slf4jLogger
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.server.http4s.Http4sServerInterpreter
import com.consultant.data.config.{ DatabaseConfig, DbConfig }
import com.consultant.api.di.AppModule
import com.consultant.api.routes.*
import com.consultant.api.middleware.TokenAuthMiddleware
import com.consultant.infrastructure.config.AppConfig
import com.consultant.infrastructure.security.TokenVerifier
import com.consultant.infrastructure.logging.LogContext
import org.flywaydb.core.Flyway

object Server extends IOApp:

  private val logger = Slf4jLogger.getLogger[IO]

  def run(args: List[String]): IO[ExitCode] =
    resources.use { module =>
      import module.*

      // Swagger documentation
      // Note: SwaggerUI is disabled in Docker build due to sbt-assembly webjars merging issue
      val swaggerRoutes    = HttpRoutes.empty[IO]
      val healthHttpRoutes = Http4sServerInterpreter[IO]().toRoutes(healthRoutes.routes)

      val rootRedirect: HttpRoutes[IO] = HttpRoutes.of[IO] {
        case req if req.pathInfo == Root =>
          PermanentRedirect(Location(uri"/docs"))
      }

      // swaggerRoutes already serve under /docs by default; do not double-prefix in Router
      // Mount everything at root to avoid pathInfo mismatches that lead to 404s
      val isPublic: Request[IO] => Boolean = { req =>
        val path   = req.uri.path.renderString
        val method = req.method
        path == "/api/settings/idle-timeout" ||
        // Allow OPTIONS preflight for COR requests on all API endpoints
        (method == Method.OPTIONS && path.startsWith("/api")) ||
        method == Method.OPTIONS ||
        path == "/" ||
        path.startsWith("/docs") ||
        path.startsWith("/health") ||
        // Explicit public user endpoints
        path.startsWith("/api/auth/register") ||
        path == "/api/auth/login" ||
        path == "/api/auth/refresh" ||
        path == "/api/users/register"
      }

      val protectedApiRoutes = TokenAuthMiddleware.protect(tokenVerifier, isPublic)(apiRoutes)

      // Add a preflight handler for CORS OPTIONS requests
      val preflightHandler: HttpRoutes[IO] = HttpRoutes.of[IO] {
        case req if req.method == Method.OPTIONS =>
          val origin = req.headers
            .get(CIString("Origin"))
            .map(_.head.value)
            .getOrElse("*")
          IO.pure(
            Response[IO](org.http4s.Status.Ok).withHeaders(
              Header.Raw(CIString("Access-Control-Allow-Origin"), origin),
              Header.Raw(CIString("Access-Control-Allow-Methods"), "GET, POST, PUT, DELETE, OPTIONS"),
              Header.Raw(
                CIString("Access-Control-Allow-Headers"),
                "Content-Type, Authorization, X-Auth-User-Id, X-User-Id, X-User-Role, Accept"
              ),
              Header.Raw(CIString("Access-Control-Max-Age"), "3600")
            )
          )
      }

      // New route handler for GET /api
      val apiRootRoute = org.http4s.HttpRoutes.of[IO] {
        case req if req.method == Method.GET && req.pathInfo == org.http4s.dsl.io.Root / "api" =>
          org.http4s.dsl.io.Ok("API is running. Date: February 28, 2026.")
      }

      // Mount all routes at root level
      // - preflightHandler: handles OPTIONS preflight requests for CORS
      // - rootRedirect: handles "/" -> "/docs"
      // - healthHttpRoutes: serves /health endpoint (public, no auth required)
      // - swaggerRoutes: serves /docs/* (public Swagger UI)
      // - protectedApiRoutes: serves /api/* endpoints (auth required, with public exceptions)
      // - adminRoutes: serves /api/admin-count (auth required)
      // - apiRootRoute: serves /api (simple health/info message)
      val routes = Router(
        "/" -> (preflightHandler <+> rootRedirect <+> healthHttpRoutes <+> swaggerRoutes <+> protectedApiRoutes <+> adminRoutes <+> apiRootRoute)
      ).orNotFound

      val corsRoutes = CORS.policy.withAllowOriginAll
        .withAllowCredentials(false)
        .withMaxAge(scala.concurrent.duration.FiniteDuration(1, scala.concurrent.duration.HOURS))
        .withAllowHeadersIn(
          Set(
            CIString("Content-Type"),
            CIString("Authorization"),
            CIString("X-Auth-User-Id"),
            CIString("X-User-Id"),
            CIString("X-User-Role"),
            CIString("Accept")
          )
        )
        .withAllowMethodsIn(
          Set(
            org.http4s.Method.GET,
            org.http4s.Method.POST,
            org.http4s.Method.PUT,
            org.http4s.Method.DELETE,
            org.http4s.Method.OPTIONS
          )
        )
        .apply(routes)

      EmberServerBuilder
        .default[IO]
        .withHost(Host.fromString(config.server.host).getOrElse(ipv4"0.0.0.0"))
        .withPort(Port.fromInt(config.server.port).getOrElse(port"8090"))
        .withHttpApp(corsRoutes)
        .build
        .use { _ =>
          val startupCtx = LogContext(
            "host"        -> config.server.host,
            "port"        -> config.server.port.toString,
            "environment" -> com.consultant.infrastructure.config.Environment.current.toString
          )
          logger.info(startupCtx.toMap)(
            s"Server started at http://${config.server.host}:${config.server.port}"
          ) >> IO.never
        }
        .as(ExitCode.Success)
    }

  private def resources: Resource[IO, AppModule] =
    for
      config <- Resource.eval(AppConfig.load)
      dbConfig = DbConfig(
        config.database.driver,
        config.database.url,
        config.database.user,
        config.database.password
      )
      // Run Flyway migrations
      _ <- Resource.eval {
        val migrationCtx = LogContext("database" -> dbConfig.url.replaceAll("//.*@", "//***@"))
        logger.info(migrationCtx.toMap)("Running database migrations...") >>
          IO {
            val flyway = Flyway
              .configure()
              .dataSource(dbConfig.url, dbConfig.user, dbConfig.password)
              .locations("classpath:db/migration")
              .cleanDisabled(true)
              .baselineOnMigrate(true)
              .load()
            val result = flyway.migrate()
            (flyway, result)
          }.flatMap { (flyway, result) =>
            val successCtx = migrationCtx.withValues(
              "migrationsExecuted" -> result.migrationsExecuted.toString,
              "schemaVersion"      -> Option(result.targetSchemaVersion).map(_.toString).getOrElse("current")
            )
            logger.info(successCtx.toMap)("Database migrations completed successfully")
          }.handleErrorWith { error =>
            logger.error(migrationCtx.toMap, error)("Database migrations failed") >>
              IO.raiseError(error)
          }
      }
      xa <- DatabaseConfig.makeTransactor[IO](dbConfig)
    yield AppModule(xa, config)
