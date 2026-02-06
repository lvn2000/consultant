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
  val getUserPreferencesEndpoint = baseEndpoint.get
    .in(header[Option[String]]("X-User-Id"))
    .out(jsonBody[UserNotificationPreferencesDto])
    .errorOut(jsonBody[ErrorResponse])

  val getUserPreferences = getUserPreferencesEndpoint.serverLogic { userIdOpt =>
    userIdOpt match
      case None => IO.pure(Left(ErrorResponse("UNAUTHORIZED", "Missing X-User-Id header")))
      case Some(userIdStr) =>
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

  // Update a specific preference
  val updatePreferenceEndpoint = baseEndpoint.put
    .in(path[UUID]("preferenceId"))
    .in(header[Option[String]]("X-User-Id"))
    .in(jsonBody[UpdateNotificationPreferenceDto])
    .out(jsonBody[NotificationPreferenceDto])
    .errorOut(jsonBody[ErrorResponse])

  val updatePreference = updatePreferenceEndpoint.serverLogic { case (preferenceId, userIdOpt, updateDto) =>
    userIdOpt match
      case None =>
        IO.pure(Left(ErrorResponse("UNAUTHORIZED", "Missing X-User-Id header")))
      case Some(_) =>
        // TODO In production, you would verify the preference belongs to the user
        // For now, we just update it if found
        notificationPreferenceRepo
          .findByUserAndType(
            UUID.fromString("00000000-0000-0000-0000-000000000000"), // Placeholder - would get from actual lookup
            NotificationType.ConsultationApproved                    // Placeholder
          )
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

  // Update preference by notification type
  val updatePreferenceByTypeEndpoint = baseEndpoint.put
    .in(path[String]("notificationType"))
    .in(header[Option[String]]("X-User-Id"))
    .in(jsonBody[UpdateNotificationPreferenceDto])
    .out(jsonBody[NotificationPreferenceDto])
    .errorOut(jsonBody[ErrorResponse])

  val updatePreferenceByType = updatePreferenceByTypeEndpoint.serverLogic {
    case (notificationTypeStr, userIdOpt, updateDto) =>
      userIdOpt match
        case None =>
          IO.pure(Left(ErrorResponse("UNAUTHORIZED", "Missing X-User-Id header")))
        case Some(userIdStr) =>
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
