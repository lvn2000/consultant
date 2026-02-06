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
      case (Some(sessionId), Some(userIdStr)) =>
        // CRITICAL SECURITY: Must validate that sessionId is owned by the requesting user
        // and that the userId from session matches userIdStr (X-User-Id header)
        // This prevents unauthorized access to other users' preferences
        // TODO: Implement session validation:
        //   1. Look up sessionId in SessionRepository to get actual userId
        //   2. Compare actual userId with userIdStr from X-User-Id header
        //   3. Return 403 Forbidden if they don't match
        //   4. Check session hasn't expired
        try
          val userId: UUID = UUID.fromString(userIdStr)

          // PLACEHOLDER: Once SessionRepository is available, replace with:
          // sessionRepo.findById(sessionId).flatMap {
          //   case Some(session) if session.userId.toString == userIdStr =>
          //     ... perform operation ...
          //   case Some(session) =>
          //     IO.pure(Left(ErrorResponse("FORBIDDEN", "User not authorized to access these preferences")))
          //   case None =>
          //     IO.pure(Left(ErrorResponse("UNAUTHORIZED", "Session not found or expired")))
          // }

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
        case (Some(sessionId), Some(userIdStr)) =>
          // CRITICAL SECURITY: Must validate that sessionId is owned by the requesting user
          // and that the userId from session matches userIdStr (X-User-Id header)
          // This prevents unauthorized modification of other users' preferences
          // TODO: Implement session validation:
          //   1. Look up sessionId in SessionRepository to get actual userId
          //   2. Compare actual userId with userIdStr from X-User-Id header
          //   3. Return 403 Forbidden if they don't match
          //   4. Check session hasn't expired
          try
            val userId: UUID     = UUID.fromString(userIdStr)
            val notificationType = NotificationType.valueOf(notificationTypeStr)

            // PLACEHOLDER: Once SessionRepository is available, replace with:
            // sessionRepo.findById(sessionId).flatMap {
            //   case Some(session) if session.userId.toString == userIdStr =>
            //     ... perform operation ...
            //   case Some(session) =>
            //     IO.pure(Left(ErrorResponse("FORBIDDEN", "User not authorized to modify these preferences")))
            //   case None =>
            //     IO.pure(Left(ErrorResponse("UNAUTHORIZED", "Session not found or expired")))
            // }

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
    getUserPreferences,
    updatePreferenceByType
  )

  val routes: HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(endpoints)

  /**
   * Helper method to extract sessionId from Authorization header
   *
   * Strictly parses the Authorization header to extract Bearer token. Only accepts headers in the format "Bearer
   * <token>" where token is non-empty. Rejects any other scheme (Basic, etc.) or malformed headers.
   *
   * CRITICAL SECURITY WARNING: This method only performs basic format validation.
   *
   * In production, this MUST be replaced with proper session validation:
   *   1. Look up sessionId in SessionRepository or similar persistent storage 2. Verify the session exists and hasn't
   *      expired 3. Extract the actual userId associated with the session 4. Return None if session is invalid/expired
   *      5. Caller must verify that the userId from session matches X-User-Id header 6. Return 403 Forbidden if userId
   *      from session != userId in X-User-Id header
   *
   * WITHOUT this implementation, any bearer token value is accepted, allowing:
   *   - Unauthorized access to read any user's preferences (by setting X-User-Id)
   *   - Unauthorized modification of any user's preferences
   *   - Auto-creation of default preferences for any user
   */
  private def extractSessionIdFromAuth(authHeader: String): Option[String] =
    authHeader.trim.split(" ", 2) match
      case Array("Bearer", token) if token.nonEmpty => Some(token)
      case _                                        => None
