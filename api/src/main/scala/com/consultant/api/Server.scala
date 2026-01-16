package com.consultant.api

import cats.effect.{ ExitCode, IO, IOApp, Resource }
import com.comcast.ip4s.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import org.http4s.server.middleware.CORS
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.server.http4s.Http4sServerInterpreter
import com.consultant.data.config.{ DatabaseConfig, DbConfig }
import com.consultant.data.repository.*
import com.consultant.core.service.*
import com.consultant.api.routes.*
import com.consultant.infrastructure.config.AppConfig
import com.consultant.infrastructure.local.MockNotificationService
import org.flywaydb.core.Flyway

object Server extends IOApp:

  def run(args: List[String]): IO[ExitCode] =
    resources.use {
      case (config, userService, specialistService, consultationService, categoryService, connectionService) =>
        val userRoutes         = UserRoutes(userService)
        val specialistRoutes   = SpecialistRoutes(specialistService)
        val consultationRoutes = ConsultationRoutes(consultationService)
        val categoryRoutes     = CategoryRoutes(categoryService)
        val connectionRoutes   = ConnectionRoutes(connectionService)

        // Swagger documentation
        val allEndpoints = userRoutes.endpoints ++ specialistRoutes.endpoints ++
          consultationRoutes.endpoints ++ categoryRoutes.endpoints ++ connectionRoutes.endpoints

        val docEndpoints = SwaggerInterpreter()
          .fromServerEndpoints(allEndpoints, "Consultant API", "1.0.0")
        val swaggerRoutes = Http4sServerInterpreter[IO]().toRoutes(docEndpoints)

        val routes = Router(
          "/api/users"            -> userRoutes.routes,
          "/api/specialists"      -> specialistRoutes.routes,
          "/api/consultations"    -> consultationRoutes.routes,
          "/api/categories"       -> categoryRoutes.routes,
          "/api/connection-types" -> connectionRoutes.routes
        ).orNotFound

        val corsRoutes = CORS.policy.withAllowOriginAll.withAllowCredentials(false).apply(routes)

        EmberServerBuilder
          .default[IO]
          .withHost(Host.fromString(config.server.host).getOrElse(ipv4"0.0.0.0"))
          .withPort(Port.fromInt(config.server.port).getOrElse(port"8090"))
          .withHttpApp(corsRoutes)
          .build
          .use { _ =>
            IO.println(s"Server started on http://${config.server.host}:${config.server.port}") >>
              IO.println(s"Swagger UI: http://${config.server.host}:${config.server.port}/docs") >>
              IO.println(s"Swagger endpoints count: ${allEndpoints.size}") >>
              IO.println("Swagger endpoints:") >>
              IO.println(allEndpoints.map(_.endpoint.show).mkString("\n---\n")) >>
              IO.never
          }
          .as(ExitCode.Success)
    }

  private def resources: Resource[
    IO,
    (AppConfig, UserService, SpecialistService, ConsultationService, CategoryService, ConnectionService)
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
      userRepo           = PostgresUserRepository(xa)
      connectionTypeRepo = PostgresConnectionTypeRepository(xa)
      connectionRepo     = PostgresConnectionRepository(xa)
      specialistRepo     = PostgresSpecialistRepository(xa, connectionRepo)
      categoryRepo       = PostgresCategoryRepository(xa)
      consultationRepo   = PostgresConsultationRepository(xa)

      // Infrastructure services
      notificationService = MockNotificationService()

      // Services
      userService       = UserService(userRepo)
      specialistService = SpecialistService(specialistRepo, categoryRepo)
      consultationService = ConsultationService(
        consultationRepo,
        specialistRepo,
        userRepo,
        notificationService
      )
      categoryService   = CategoryService(categoryRepo)
      connectionService = ConnectionService(connectionRepo, connectionTypeRepo, specialistRepo)
    yield (config, userService, specialistService, consultationService, categoryService, connectionService)
