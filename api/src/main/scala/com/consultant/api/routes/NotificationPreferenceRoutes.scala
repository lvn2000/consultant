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
    // Extract sessionId from Authorization header
    val sessionIdOpt = extractSessionIdFromAuth(authHeader)
    
    (sessionIdOpt, userIdOpt) match
      case (None, _) =>
        IO.pure(Left(ErrorResponse("UNAUTHORIZED", "Missing or invalid Authorization header")))
      case (_, None) =>
        IO.pure(Left(ErrorResponse("BAD_REQUEST", "Missing X-User-Id header")))
      case (Some(_sessionId), Some(userIdStr)) =>
        // TODO: In production, validate that _sessionId belongs to the requesting user
        // and that the userId in sessionId matches the userIdStr provided
        try
          val userId: UUID = UUID.fromString(userIdStr)
          notificationPreferenceRepo
            .findByUser(userId)
            .flatMap { preferences =>
              // If no preferences exist, create defaults
              if preferences.isEmpty then
                notificationPreferenceRepo
                  .createDefaults(userId)
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
                        userId = userId,
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
                      userId = userId,
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
      val sessionIdOpt = extractSessionIdFromAuth(authHeader)
      
      (sessionIdOpt, userIdOpt) match
        case (None, _) =>
          IO.pure(Left(ErrorResponse("UNAUTHORIZED", "Missing or invalid Authorization header")))
        case (_, None) =>
          IO.pure(Left(ErrorResponse("BAD_REQUEST", "Missing X-User-Id header")))
        case (Some(_sessionId), Some(userIdStr)) =>
          // TODO: In production, validate that _sessionId belongs to the requesting user
          // and that the userId in sessionId matches the userIdStr provided
          try
            val userId: UUID     = UUID.fromString(userIdStr)
            val notificationType = NotificationType.valueOf(notificationTypeStr)

            notificationPreferenceRepo
              .findByUserAndType(userId, notificationType)
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
    getUserPreferencesEndpoint,
    updatePreferenceByTypeEndpoint
  )

  val routes: HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(
    List(getUserPreferences, updatePreferenceByType)
  )

  /** Helper method to extract sessionId from Authorization header
    * 
    * IMPORTANT: This is a placeholder for session validation
    * In production, this should:
    * 1. Validate the sessionId exists in the session store
    * 2. Extract the associated userId from the session
    * 3. Verify the session has not expired
    * 4. Perform the ownership check (userId in session == userId in X-User-Id)
    * 
    * Current implementation assumes Bearer tokens are valid sessionIds
    */
  private def extractSessionIdFromAuth(authHeader: String): Option[String] =
    authHeader
      .stripPrefix("Bearer ")
      .trim match
      case sessionId if sessionId.nonEmpty => Some(sessionId)
      case _                               => None
