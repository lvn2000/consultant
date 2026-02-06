package com.consultant.api

import cats.effect.{ ExitCode, IO, IOApp, Resource }
import cats.syntax.all.*
import com.comcast.ip4s.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.{ HttpRoutes, Response }
import org.http4s.dsl.io.*
import org.http4s.headers.Location
import org.http4s.implicits.*
import org.http4s.server.Router
import org.http4s.server.middleware.CORS
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.server.http4s.Http4sServerInterpreter
import com.consultant.data.config.{ DatabaseConfig, DbConfig }
import com.consultant.data.repository.*
import com.consultant.core.service.*
import com.consultant.core.ports.{ AvailabilityRepository, NotificationPreferenceRepository }
import com.consultant.api.routes.*
import com.consultant.infrastructure.config.AppConfig
import com.consultant.infrastructure.local.MockNotificationService
import org.flywaydb.core.Flyway

object Server extends IOApp:

  def run(args: List[String]): IO[ExitCode] =
    resources.use {
      case (
            config,
            userService,
            specialistService,
            consultationService,
            categoryService,
            connectionService,
            availabilityRepository,
            notificationPreferenceRepository
          ) =>
        val userRoutes                   = UserRoutes(userService)
        val specialistRoutes             = SpecialistRoutes(specialistService)
        val consultationRoutes           = ConsultationRoutes(consultationService)
        val categoryRoutes               = CategoryRoutes(categoryService)
        val connectionRoutes             = ConnectionRoutes(connectionService)
        val availabilityRoutes           = AvailabilitySlotRoutes(consultationService, availabilityRepository)
        val notificationPreferenceRoutes = NotificationPreferenceRoutes(notificationPreferenceRepository)

        // Swagger documentation
        val allEndpoints = userRoutes.endpoints ++ specialistRoutes.endpoints ++
          consultationRoutes.endpoints ++ categoryRoutes.endpoints ++ connectionRoutes.endpoints ++
          availabilityRoutes.endpoints
        // Notification preference routes are handled separately (have custom auth headers)
        // Count computed dynamically to prevent drift when endpoints change
        val notificationEndpointCount = notificationPreferenceRoutes.endpoints.size

        val docEndpoints = SwaggerInterpreter()
          .fromServerEndpoints(allEndpoints, "Consultant API", "1.0.0")
        val swaggerRoutes = Http4sServerInterpreter[IO]().toRoutes(docEndpoints)

        val rootRedirect: HttpRoutes[IO] = HttpRoutes.of[IO] {
          case req if req.pathInfo == Root =>
            // Redirect only the bare root to Swagger UI to avoid loops
            PermanentRedirect(Location(uri"/docs"))
        }

        val apiRoutes = Router(
          "/api/users" -> (connectionRoutes.clientConnectionRoutes <+> userRoutes.routes),
          "/api/specialists" -> (availabilityRoutes.routes <+> connectionRoutes.specialistConnectionRoutes <+> specialistRoutes.routes),
          "/api/consultations"            -> consultationRoutes.routes,
          "/api/categories"               -> categoryRoutes.routes,
          "/api/connection-types"         -> connectionRoutes.connectionTypeRoutes,
          "/api/notification-preferences" -> notificationPreferenceRoutes.routes
        )

        // swaggerRoutes already serve under /docs by default; do not double-prefix in Router
        // Mount everything at root to avoid pathInfo mismatches that lead to 404s
        val routes = Router(
          "/" -> (rootRedirect <+> swaggerRoutes <+> apiRoutes)
        ).orNotFound

        val corsRoutes = CORS.policy.withAllowOriginAll.withAllowCredentials(false).apply(routes)

        EmberServerBuilder
          .default[IO]
          .withHost(Host.fromString(config.server.host).getOrElse(ipv4"0.0.0.0"))
          .withPort(Port.fromInt(config.server.port).getOrElse(port"8090"))
          .withHttpApp(corsRoutes)
          .build
          .use { _ =>
            // Keep startup logs concise to avoid noisy console output
            IO.println(
              s"Server up at http://${config.server.host}:${config.server.port} | Swagger: /docs | " +
              s"Total endpoints: ${allEndpoints.size + notificationEndpointCount}"
            ) >> IO.never
          }
          .as(ExitCode.Success)
    }

  private def resources: Resource[
    IO,
    (
      AppConfig,
      UserService,
      SpecialistService,
      ConsultationService,
      CategoryService,
      ConnectionService,
      AvailabilityRepository,
      NotificationPreferenceRepository
    )
  ] =
    for
      config <- Resource.eval(AppConfig.load)
      dbConfig = DbConfig(
        config.database.driver,
        config.database.url,
        config.database.user,
        config.database.password
      )
      // Run Flyway migrations before creating transactor
      _ <- Resource.eval(IO {
        val flyway = Flyway
          .configure()
          .dataSource(dbConfig.url, dbConfig.user, dbConfig.password)
          .locations("classpath:db/migration")
          .load()
        flyway.migrate()
      })
      xa <- DatabaseConfig.makeTransactor[IO](dbConfig)

      // Repositories
      userRepo                   = PostgresUserRepository(xa)
      sessionRepo                = PostgresSessionRepository(xa)
      connectionTypeRepo         = PostgresConnectionTypeRepository(xa)
      connectionRepo             = PostgresConnectionRepository(xa)
      specialistRepo             = PostgresSpecialistRepository(xa, connectionRepo)
      categoryRepo               = PostgresCategoryRepository(xa)
      consultationRepo           = PostgresConsultationRepository(xa)
      availabilityRepo           = PostgresAvailabilityRepository(xa)
      notificationPreferenceRepo = PostgresNotificationPreferenceRepository(xa)

      // Infrastructure services
      notificationService = MockNotificationService()

      // Services
      userService       = UserService(userRepo, sessionRepo, Some(notificationPreferenceRepo))
      specialistService = SpecialistService(specialistRepo, categoryRepo)
      consultationService = ConsultationService(
        consultationRepo,
        specialistRepo,
        userRepo,
        notificationService,
        notificationPreferenceRepo
      )
      categoryService   = CategoryService(categoryRepo)
      connectionService = ConnectionService(connectionRepo, connectionTypeRepo, specialistRepo)
    yield (
      config,
      userService,
      specialistService,
      consultationService,
      categoryService,
      connectionService,
      availabilityRepo,
      notificationPreferenceRepo
    )
