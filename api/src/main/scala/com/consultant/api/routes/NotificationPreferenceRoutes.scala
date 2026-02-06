package com.consultant.api.routes

import cats.effect.IO
import org.http4s.HttpRoutes
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.http4s.Http4sServerInterpreter
import com.consultant.api.dto.*
import com.consultant.core.domain.*
import com.consultant.core.domain.security.UserRole
import com.consultant.core.ports.NotificationPreferenceRepository
import com.consultant.core.ports.SessionRepository
import java.util.UUID

class NotificationPreferenceRoutes(
  notificationPreferenceRepo: NotificationPreferenceRepository,
  sessionRepo: SessionRepository
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
        try
          val requestedUserId: UUID = UUID.fromString(userIdStr)

          // Validate session ownership: session userId must match requested userId
          sessionRepo.findById(sessionId).flatMap {
            case None =>
              // Session not found or expired
              IO.pure(Left(ErrorResponse("UNAUTHORIZED", "Session not found or expired")))
            case Some(session) if session.expiresAt.isBefore(java.time.Instant.now()) =>
              // Session has expired
              IO.pure(Left(ErrorResponse("UNAUTHORIZED", "Session expired")))
            case Some(session) =>
              // Check if requester is authorized to access the preferences (owner or admin)
              if session.userId != requestedUserId && session.role != UserRole.Admin then
                // Caller requesting another user's preferences without proper authorization
                IO.pure(Left(ErrorResponse("FORBIDDEN", "Not authorized to access these preferences")))
              else
                // Session valid and user is authorized, proceed with access
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
          try
            val requestedUserId: UUID = UUID.fromString(userIdStr)
            val notificationType      = NotificationType.valueOf(notificationTypeStr)

            // Validate session ownership: session userId must match requested userId
            sessionRepo.findById(sessionId).flatMap {
              case None =>
                // Session not found or expired
                IO.pure(Left(ErrorResponse("UNAUTHORIZED", "Session not found or expired")))
              case Some(session) if session.expiresAt.isBefore(java.time.Instant.now()) =>
                // Session has expired
                IO.pure(Left(ErrorResponse("UNAUTHORIZED", "Session expired")))
              case Some(session) =>
                // Check if requester owns the preferences being modified
                if session.userId != requestedUserId then
                  // Caller modifying another user's preferences without proper authorization
                  IO.pure(Left(ErrorResponse("FORBIDDEN", "Not authorized to modify these preferences")))
                else
                  // Session valid and user is authenticated, proceed with modification
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
   * NOTE: Session validation is implemented in getUserPreferences and updatePreferenceByType endpoints. Each endpoint
   * validates that:
   *   1. The sessionId exists in SessionRepository 2. The session hasn't expired (expiresAt is in the future) 3. The
   *      session's userId matches the X-User-Id header from the request 4. Returns 403 Forbidden if user attempts to
   *      access/modify another user's preferences 5. Returns 401 Unauthorized if session is not found or has expired
   */
  private def extractSessionIdFromAuth(authHeader: String): Option[String] =
    authHeader.trim.split(" ", 2) match
      case Array("Bearer", token) if token.nonEmpty => Some(token)
      case _                                        => None
