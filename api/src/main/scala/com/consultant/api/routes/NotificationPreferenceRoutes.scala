package com.consultant.api.routes

import cats.effect.IO
import org.http4s.HttpRoutes
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.http4s.Http4sServerInterpreter
import com.consultant.api.dto.*
import com.consultant.core.domain.*
import com.consultant.core.ports.NotificationPreferenceRepository
import java.util.UUID

class NotificationPreferenceRoutes(
  notificationPreferenceRepo: NotificationPreferenceRepository
):

  private val baseEndpoint = endpoint

  // Get all preferences for authenticated user
  // SECURITY: Uses Authorization header containing sessionId for authentication
  // The userId in X-User-Id must match the requesting user (owner validation)
  val getUserPreferencesEndpoint = baseEndpoint.get
    .in(header[String]("Authorization"))
    .in(header[Option[String]]("X-User-Id"))
    .out(jsonBody[UserNotificationPreferencesDto])
    .errorOut(jsonBody[ErrorResponse])

  val getUserPreferences = getUserPreferencesEndpoint.serverLogic { case (authHeader, userIdOpt) =>
    // SECURITY: TokenAuthMiddleware has already validated the JWT token and extracted X-User-Id
    // We trust the middleware's validation and use the X-User-Id header directly
    userIdOpt match
      case None =>
        IO.pure(Left(ErrorResponse("BAD_REQUEST", "Missing X-User-Id header - token validation failed")))
      case Some(userIdStr) =>
        try
          val requestedUserId: UUID = UUID.fromString(userIdStr)

          // Token was already validated by TokenAuthMiddleware, so proceed with access
          notificationPreferenceRepo
            .findByUser(requestedUserId)
            .flatMap { preferences =>
              // If no preferences exist, create defaults
              if preferences.isEmpty then
                notificationPreferenceRepo
                  .createDefaults(requestedUserId)
                  .map { createdPrefs =>
                    val dtos = createdPrefs.map { pref =>
                      NotificationPreferenceDto(
                        id = pref.id,
                        userId = pref.userId,
                        notificationType = pref.notificationType.toString,
                        emailEnabled = pref.emailEnabled,
                        smsEnabled = pref.smsEnabled,
                        createdAt = pref.createdAt,
                        updatedAt = pref.updatedAt
                      )
                    }
                    Right(
                      UserNotificationPreferencesDto(
                        userId = requestedUserId,
                        preferences = dtos
                      )
                    )
                  }
              else
                val dtos = preferences.map { pref =>
                  NotificationPreferenceDto(
                    id = pref.id,
                    userId = pref.userId,
                    notificationType = pref.notificationType.toString,
                    emailEnabled = pref.emailEnabled,
                    smsEnabled = pref.smsEnabled,
                    createdAt = pref.createdAt,
                    updatedAt = pref.updatedAt
                  )
                }
                IO.pure(
                  Right(
                    UserNotificationPreferencesDto(
                      userId = requestedUserId,
                      preferences = dtos
                    )
                  )
                )
            }
        catch
          case _: IllegalArgumentException =>
            IO.pure(Left(ErrorResponse("VALIDATION_ERROR", "Invalid user ID format")))
  }

  // Update preference by notification type
  // SECURITY: Requires both Authorization header (sessionId) and X-User-Id ownership validation
  val updatePreferenceByTypeEndpoint = baseEndpoint.put
    .in(path[String]("notificationType"))
    .in(header[String]("Authorization"))
    .in(header[Option[String]]("X-User-Id"))
    .in(jsonBody[UpdateNotificationPreferenceDto])
    .out(jsonBody[NotificationPreferenceDto])
    .errorOut(jsonBody[ErrorResponse])

  val updatePreferenceByType = updatePreferenceByTypeEndpoint.serverLogic {
    case (notificationTypeStr, authHeader, userIdOpt, updateDto) =>
      // SECURITY: TokenAuthMiddleware has already validated the JWT token and extracted X-User-Id
      // We trust the middleware's validation and use the X-User-Id header directly
      userIdOpt match
        case None =>
          IO.pure(Left(ErrorResponse("BAD_REQUEST", "Missing X-User-Id header - token validation failed")))
        case Some(userIdStr) =>
          try
            val requestedUserId: UUID = UUID.fromString(userIdStr)
            val notificationType      = NotificationType.valueOf(notificationTypeStr)

            // Token was already validated by TokenAuthMiddleware, so proceed with modification
            notificationPreferenceRepo
              .findByUserAndType(requestedUserId, notificationType)
              .flatMap {
                case None =>
                  IO.pure(Left(ErrorResponse("NOT_FOUND", "Preference not found")))
                case Some(pref) =>
                  val updated = pref.copy(
                    emailEnabled = updateDto.emailEnabled,
                    smsEnabled = updateDto.smsEnabled
                  )
                  notificationPreferenceRepo
                    .update(updated)
                    .map { updatedPref =>
                      Right(
                        NotificationPreferenceDto(
                          id = updatedPref.id,
                          userId = updatedPref.userId,
                          notificationType = updatedPref.notificationType.toString,
                          emailEnabled = updatedPref.emailEnabled,
                          smsEnabled = updatedPref.smsEnabled,
                          createdAt = updatedPref.createdAt,
                          updatedAt = updatedPref.updatedAt
                        )
                      )
                    }
              }
          catch
            case _: IllegalArgumentException =>
              IO.pure(Left(ErrorResponse("VALIDATION_ERROR", "Invalid notification type or user ID")))
  }

  val endpoints = List(
    getUserPreferences,
    updatePreferenceByType
  )

  val routes: HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(endpoints)
