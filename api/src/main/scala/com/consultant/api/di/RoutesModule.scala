package com.consultant.api.di

import cats.effect.IO
import cats.syntax.all.*
import org.http4s.HttpRoutes
import org.http4s.server.Router
import com.consultant.api.routes.*
import com.consultant.infrastructure.security.JwtTokenService

/**
 * RoutesModule creates and wires all HTTP routes.
 *
 * This module depends on ServiceModule and creates route instances using the wired services.
 */
trait RoutesModule extends ServiceModule:

  // Individual route instances
  lazy val authRoutes: AuthRoutes =
    AuthRoutes(authService, config.legacyAuthEnabled)

  lazy val userRoutes: UserRoutes =
    UserRoutes(userService, Some(jwtService))

  lazy val specialistRoutes: SpecialistRoutes =
    SpecialistRoutes(specialistService)

  lazy val consultationRoutes: ConsultationRoutes =
    ConsultationRoutes(consultationService)

  lazy val categoryRoutes: CategoryRoutes =
    CategoryRoutes(categoryService)

  lazy val connectionRoutes: ConnectionRoutes =
    ConnectionRoutes(connectionService)

  lazy val availabilityRoutes: AvailabilitySlotRoutes =
    AvailabilitySlotRoutes(consultationService, availabilityRepo)

  lazy val notificationPreferenceRoutes: NotificationPreferenceRoutes =
    NotificationPreferenceRoutes(notificationPreferenceRepo)

  lazy val systemSettingRoutes: SystemSettingRoutes =
    SystemSettingRoutes(systemSettingService)

  lazy val healthRoutes: HealthRoutes =
    HealthRoutes(Some(xa))

  /**
   * Composes all API routes into a single router.
   *
   * This follows the composition pattern where individual routes are mounted at specific paths.
   */
  lazy val apiRoutes: HttpRoutes[IO] = Router(
    "/api/auth"  -> authRoutes.routes,
    "/api/users" -> (connectionRoutes.clientConnectionRoutes <+> userRoutes.routes),
    "/api/specialists" -> (
      availabilityRoutes.routes <+>
        connectionRoutes.specialistConnectionRoutes <+>
        specialistRoutes.routes
    ),
    "/api/consultations"            -> consultationRoutes.routes,
    "/api/categories"               -> categoryRoutes.routes,
    "/api/connection-types"         -> connectionRoutes.connectionTypeRoutes,
    "/api/notification-preferences" -> notificationPreferenceRoutes.routes,
    "/api/settings"                 -> systemSettingRoutes.routes
  )

  /** Admin-only routes that need special handling. */
  lazy val adminRoutes: HttpRoutes[IO] = Router(
    "/api/admin-count" -> userRoutes.getAdminCountRoute
  )

end RoutesModule
