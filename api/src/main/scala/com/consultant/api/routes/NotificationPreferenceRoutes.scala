/*
 * Copyright (c) 2026 Volodymyr Lubenchenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
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

  // Get current authenticated user's preferences
  // SECURITY: Uses X-Auth-User-Id from the authenticated token
  // This is a convenience endpoint that gets the current user's ID from the auth header
  val getCurrentUserPreferencesEndpoint = baseEndpoint.get
    .in(header[Option[String]]("X-Auth-User-Id"))
    .in(header[Option[String]]("X-User-Role"))
    .out(jsonBody[UserNotificationPreferencesDto])
    .errorOut(jsonBody[ErrorResponse])

  val getCurrentUserPreferences = getCurrentUserPreferencesEndpoint.serverLogic { case (authUserIdOpt, roleOpt) =>
    (authUserIdOpt, roleOpt) match
      case (None, _) | (_, None) =>
        IO.pure(Left(ErrorResponse("UNAUTHORIZED", "Missing authentication headers")))
      case (Some(authUserIdStr), Some(roleName)) =>
        try
          val requestedUserId: UUID = UUID.fromString(authUserIdStr)

          // Always allow users to access their own preferences using this endpoint
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

  // Get preferences for a specific user (owner or admin)
  // SECURITY: Uses X-Auth-User-Id for the authenticated principal and X-User-Role for authorization
  // userId in path specifies which user's preferences to retrieve
  // Access is allowed if: userId matches authenticated user OR user has Admin role
  val getUserPreferencesEndpoint = baseEndpoint.get
    .in(path[String]("userId"))
    .in(header[Option[String]]("X-Auth-User-Id"))
    .in(header[Option[String]]("X-User-Role"))
    .out(jsonBody[UserNotificationPreferencesDto])
    .errorOut(jsonBody[ErrorResponse])

  val getUserPreferences = getUserPreferencesEndpoint.serverLogic { case (userIdStr, authUserIdOpt, roleOpt) =>
    (authUserIdOpt, roleOpt) match
      case (None, _) | (_, None) =>
        IO.pure(Left(ErrorResponse("UNAUTHORIZED", "Missing authentication headers")))
      case (Some(authUserIdStr), Some(roleName)) =>
        try
          val requestedUserId: UUID     = UUID.fromString(userIdStr)
          val authenticatedUserId: UUID = UUID.fromString(authUserIdStr)

          // SECURITY: Enforce role-based access control
          // Only owner or admin can access preferences
          val isOwner = authenticatedUserId == requestedUserId
          val isAdmin = roleName.equalsIgnoreCase("Admin")

          if !isOwner && !isAdmin then
            IO.pure(Left(ErrorResponse("FORBIDDEN", "Not authorized to access these preferences")))
          else
            // Access granted, proceed with fetching preferences
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

  // Update preference by notification type (owner or admin)
  // SECURITY: Uses X-Auth-User-Id for the authenticated principal and X-User-Role for authorization
  // userId in path specifies which user's preferences to update
  // Access is allowed if: userId matches authenticated user OR user has Admin role
  val updatePreferenceByTypeEndpoint = baseEndpoint.put
    .in(path[String]("userId"))
    .in(path[String]("notificationType"))
    .in(header[Option[String]]("X-Auth-User-Id"))
    .in(header[Option[String]]("X-User-Role"))
    .in(jsonBody[UpdateNotificationPreferenceDto])
    .out(jsonBody[NotificationPreferenceDto])
    .errorOut(jsonBody[ErrorResponse])

  val updatePreferenceByType = updatePreferenceByTypeEndpoint.serverLogic {
    case (userIdStr, notificationTypeStr, authUserIdOpt, roleOpt, updateDto) =>
      (authUserIdOpt, roleOpt) match
        case (None, _) | (_, None) =>
          IO.pure(Left(ErrorResponse("UNAUTHORIZED", "Missing authentication headers")))
        case (Some(authUserIdStr), Some(roleName)) =>
          try
            val requestedUserId: UUID     = UUID.fromString(userIdStr)
            val authenticatedUserId: UUID = UUID.fromString(authUserIdStr)
            val notificationType          = NotificationType.valueOf(notificationTypeStr)

            // SECURITY: Enforce role-based access control
            // Only owner or admin can modify preferences
            val isOwner = authenticatedUserId == requestedUserId
            val isAdmin = roleName.equalsIgnoreCase("Admin")

            if !isOwner && !isAdmin then
              IO.pure(Left(ErrorResponse("FORBIDDEN", "Not authorized to modify these preferences")))
            else
              // Access granted, proceed with modification
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
    getCurrentUserPreferences,
    getUserPreferences,
    updatePreferenceByType
  )

  val routes: HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(endpoints)
