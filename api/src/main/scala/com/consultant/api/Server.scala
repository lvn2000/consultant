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
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.server.http4s.Http4sServerInterpreter
import com.consultant.data.config.{ DatabaseConfig, DbConfig }
import com.consultant.data.repository.*
import com.consultant.core.service.*
import com.consultant.core.ports.{ AvailabilityRepository, NotificationPreferenceRepository, SessionRepository }
import com.consultant.api.routes.*
import com.consultant.api.middleware.TokenAuthMiddleware
import com.consultant.infrastructure.config.AppConfig
import com.consultant.infrastructure.local.MockNotificationService
import com.consultant.infrastructure.security.{
  AuthenticationService,
  CompositeTokenVerifier,
  JwtTokenService,
  LegacyJwtTokenVerifier,
  OidcTokenVerifier,
  PasswordHashingService,
  TokenVerifier
}
import org.flywaydb.core.Flyway

object Server extends IOApp:

  def run(args: List[String]): IO[ExitCode] =
    resources.use {
      case (
            config,
            tokenVerifier,
            jwtService,
            authenticationService,
            userService,
            specialistService,
            consultationService,
            categoryService,
            connectionService,
            availabilityRepository,
            notificationPreferenceRepository,
            sessionRepository
          ) =>
        val authRoutes         = AuthRoutes(authenticationService, config.legacyAuthEnabled)
        val userRoutes         = UserRoutes(userService, Some(jwtService))
        val specialistRoutes   = SpecialistRoutes(specialistService)
        val consultationRoutes = ConsultationRoutes(consultationService)
        val categoryRoutes     = CategoryRoutes(categoryService)
        val connectionRoutes   = ConnectionRoutes(connectionService)
        val availabilityRoutes = AvailabilitySlotRoutes(consultationService, availabilityRepository)
        val notificationPreferenceRoutes =
          NotificationPreferenceRoutes(notificationPreferenceRepository)
        val healthRoutes = HealthRoutes()

        // Swagger documentation
        // Note: SwaggerUI is disabled in Docker build due to sbt-assembly webjars merging issue
        // To enable locally: sbt "api/run" instead of Docker
        // For production: Use separate non-fat JAR deployment or fix merge strategy per:
        // https://tapir.softwaremill.com/en/latest/docs/openapi.html#using-swaggerui-with-sbt-assembly
        // val allEndpoints = authRoutes.endpoints ++ userRoutes.endpoints ++ specialistRoutes.endpoints ++
        //   consultationRoutes.endpoints ++ categoryRoutes.endpoints ++ connectionRoutes.endpoints ++
        //   availabilityRoutes.endpoints ++ notificationPreferenceRoutes.endpoints
        // val docEndpoints = SwaggerInterpreter()
        //   .fromServerEndpoints(allEndpoints, "Consultant API", "1.0.0")
        // val swaggerRoutes = Http4sServerInterpreter[IO]().toRoutes(docEndpoints)
        val swaggerRoutes    = HttpRoutes.empty[IO] // SwaggerUI disabled in Docker
        val healthHttpRoutes = Http4sServerInterpreter[IO]().toRoutes(healthRoutes.routes)

        val rootRedirect: HttpRoutes[IO] = HttpRoutes.of[IO] {
          case req if req.pathInfo == Root =>
            // Redirect only the bare root to Swagger UI to avoid loops
            PermanentRedirect(Location(uri"/docs"))
        }

        val apiRoutes = Router(
          "/api/auth"  -> authRoutes.routes,
          "/api/users" -> (connectionRoutes.clientConnectionRoutes <+> userRoutes.routes),
          "/api/specialists" -> (availabilityRoutes.routes <+> connectionRoutes.specialistConnectionRoutes <+> specialistRoutes.routes),
          "/api/consultations"            -> consultationRoutes.routes,
          "/api/categories"               -> categoryRoutes.routes,
          "/api/connection-types"         -> connectionRoutes.connectionTypeRoutes,
          "/api/notification-preferences" -> notificationPreferenceRoutes.routes
        )

        // Separate admin-count endpoint to avoid path conflicts with UUID paths
        val adminCountRoutes = Router(
          "/api/admin-count" -> userRoutes.getAdminCountRoute
        )

        // swaggerRoutes already serve under /docs by default; do not double-prefix in Router
        // Mount everything at root to avoid pathInfo mismatches that lead to 404s
        val isPublic: Request[IO] => Boolean = { req =>
          val path   = req.uri.path.renderString
          val method = req.method

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
          path == "/api/users/register" ||
          path == "/api/users/login"
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

        // Mount all routes at root level
        // - preflightHandler: handles OPTIONS preflight requests for CORS
        // - rootRedirect: handles "/" -> "/docs"
        // - healthHttpRoutes: serves /health endpoint (public, no auth required)
        // - swaggerRoutes: serves /docs/* (public Swagger UI)
        // - protectedApiRoutes: serves /api/* endpoints (auth required, with public exceptions)
        // - adminCountRoutes: serves /api/admin-count (auth required)
        val routes = Router(
          "/" -> (preflightHandler <+> rootRedirect <+> healthHttpRoutes <+> swaggerRoutes <+> protectedApiRoutes <+> adminCountRoutes)
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
            // Keep startup logs concise to avoid noisy console output
            IO.println(
              s"Server up at http://${config.server.host}:${config.server.port}"
            ) >> IO.never
          }
          .as(ExitCode.Success)
    }

  private def resources: Resource[
    IO,
    (
      AppConfig,
      TokenVerifier,
      JwtTokenService,
      AuthenticationService,
      UserService,
      SpecialistService,
      ConsultationService,
      CategoryService,
      ConnectionService,
      AvailabilityRepository,
      NotificationPreferenceRepository,
      SessionRepository
    )
  ] =
    for
      config <- Resource.eval(AppConfig.load)
      jwtService = new JwtTokenService(
        secretKey = config.jwt.secret,
        issuer = config.jwt.issuer,
        accessTokenTTL = config.jwt.accessTtl,
        refreshTokenTTL = config.jwt.refreshTtl
      )
      tokenVerifier <- Resource.eval(buildTokenVerifier(config, jwtService))
      dbConfig = DbConfig(
        config.database.driver,
        config.database.url,
        config.database.user,
        config.database.password
      )
      // Run Flyway migrations
      _ <- Resource.eval(IO {
        val flyway = Flyway
          .configure()
          .dataSource(dbConfig.url, dbConfig.user, dbConfig.password)
          .locations("classpath:db/migration")
          .cleanDisabled(true)
          .baselineOnMigrate(true)
          .load()
        flyway.migrate()
      })
      xa <- DatabaseConfig.makeTransactor[IO](dbConfig)

      // Repositories
      userRepo                   = PostgresUserRepository(xa)
      sessionRepo                = PostgresSessionRepository(xa)
      credentialsRepo            = PostgresCredentialsRepository(xa)
      refreshTokenRepo           = PostgresRefreshTokenRepository(xa)
      auditRepo                  = PostgresSecurityAuditRepository(xa)
      connectionTypeRepo         = PostgresConnectionTypeRepository(xa)
      connectionRepo             = PostgresConnectionRepository(xa)
      specialistRepo             = PostgresSpecialistRepository(xa, connectionRepo)
      categoryRepo               = PostgresCategoryRepository(xa)
      consultationRepo           = PostgresConsultationRepository(xa)
      availabilityRepo           = PostgresAvailabilityRepository(xa)
      notificationPreferenceRepo = PostgresNotificationPreferenceRepository(xa)

      // Infrastructure services
      passwordService     = PasswordHashingService()
      notificationService = MockNotificationService()

      // Services
      authService = AuthenticationService(
        config,
        userRepo,
        specialistRepo,
        credentialsRepo,
        refreshTokenRepo,
        auditRepo,
        passwordService,
        jwtService
      )
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
      tokenVerifier,
      jwtService,
      authService,
      userService,
      specialistService,
      consultationService,
      categoryService,
      connectionService,
      availabilityRepo,
      notificationPreferenceRepo,
      sessionRepo
    )

  private def buildTokenVerifier(config: AppConfig, jwtService: JwtTokenService): IO[TokenVerifier] =
    IO {
      val oidcVerifier   = new OidcTokenVerifier(config.oidc)
      val legacyVerifier = new LegacyJwtTokenVerifier(jwtService)

      if config.oidc.enabled && config.legacyAuthEnabled then CompositeTokenVerifier(Some(oidcVerifier), legacyVerifier)
      else if config.oidc.enabled then oidcVerifier
      else if config.legacyAuthEnabled then legacyVerifier
      else throw new RuntimeException("Both OIDC and legacy auth are disabled")
    }
