package com.consultant.api

import cats.effect.{ ExitCode, IO, IOApp, Resource }
import com.comcast.ip4s.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.server.http4s.Http4sServerInterpreter
import com.consultant.data.config.{ DatabaseConfig, DbConfig }
import com.consultant.data.repository.*
import com.consultant.core.service.*
import com.consultant.api.routes.*
import com.consultant.infrastructure.config.AppConfig
import com.consultant.infrastructure.local.MockNotificationService

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
          "/"     -> userRoutes.routes,
          "/"     -> specialistRoutes.routes,
          "/"     -> consultationRoutes.routes,
          "/"     -> categoryRoutes.routes,
          "/"     -> connectionRoutes.routes,
          "/docs" -> swaggerRoutes
        ).orNotFound

        EmberServerBuilder
          .default[IO]
          .withHost(Host.fromString(config.server.host).getOrElse(ipv4"0.0.0.0"))
          .withPort(Port.fromInt(config.server.port).getOrElse(port"8080"))
          .withHttpApp(routes)
          .build
          .use { _ =>
            IO.println(s"Server started on http://${config.server.host}:${config.server.port}") >>
              IO.println(s"Swagger UI: http://${config.server.host}:${config.server.port}/docs") >>
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
