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
import cats.syntax.all.*
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.http4s.Http4sServerInterpreter
import org.http4s.HttpRoutes
import com.consultant.api.dto.*
import com.consultant.core.service.SystemSettingService
import com.consultant.core.domain.SettingType

/** System settings endpoints for admin configuration */
class SystemSettingRoutes(settingService: SystemSettingService):

  private val baseEndpoint = endpoint
    .errorOut(
      oneOf[ErrorResponse](
        oneOfVariant(statusCode(sttp.model.StatusCode.BadRequest).and(jsonBody[ErrorResponse])),
        oneOfVariant(statusCode(sttp.model.StatusCode.Unauthorized).and(jsonBody[ErrorResponse])),
        oneOfVariant(statusCode(sttp.model.StatusCode.Forbidden).and(jsonBody[ErrorResponse])),
        oneOfVariant(statusCode(sttp.model.StatusCode.InternalServerError).and(jsonBody[ErrorResponse]))
      )
    )

  // GET /api/settings - Get all public settings
  val getPublicSettingsEndpoint = baseEndpoint.get
    .in("settings")
    .out(jsonBody[List[SystemSettingDto]])
    .description("Get all public system settings")

  def getPublicSettingsRoute = getPublicSettingsEndpoint.serverLogic { _ =>
    settingService.getPublicSettings.map { settings =>
      Right(
        settings.map(s =>
          SystemSettingDto(
            id = s.id,
            key = s.key,
            value = s.value,
            settingType = s.settingType.toString,
            description = s.description,
            isPublic = s.isPublic
          )
        )
      )
    }
  }

  // GET /api/settings/idle-timeout - Get idle timeout configuration
  val getIdleTimeoutEndpoint = baseEndpoint.get
    .in("idle-timeout")
    .out(jsonBody[IdleTimeoutConfigDto])
    .description("Get idle timeout configuration")

  def getIdleTimeoutRoute = getIdleTimeoutEndpoint.serverLogic { _ =>
    for
      timeout <- settingService.getIdleTimeoutMinutes
      warning <- settingService.getIdleWarningMinutes
    yield Right(IdleTimeoutConfigDto(timeout, warning))
  }

  // PUT /api/settings/idle-timeout - Update idle timeout configuration (admin only)
  val updateIdleTimeoutEndpoint = baseEndpoint.put
    .in("idle-timeout")
    .in(header[Option[String]]("X-User-Role"))
    .in(jsonBody[UpdateIdleTimeoutDto])
    .out(jsonBody[IdleTimeoutConfigDto])
    .description("Update idle timeout configuration (admin only)")

  def updateIdleTimeoutRoute = updateIdleTimeoutEndpoint.serverLogic { case (userRoleOpt, dto) =>
    import com.consultant.api.mappers.ErrorMappers.toErrorResponse
    // X-User-Role header is set by TokenAuthMiddleware from verified JWT
    if userRoleOpt.exists(_.equalsIgnoreCase("Admin")) then
      for
        timeoutResult <- dto.idleTimeoutMinutes.traverse(mins => settingService.setIdleTimeout(mins))
        warningResult <- dto.idleWarningMinutes.traverse(mins => settingService.setIdleWarning(mins))
        timeout       <- settingService.getIdleTimeoutMinutes
        warning       <- settingService.getIdleWarningMinutes
      yield (timeoutResult, warningResult) match
        case (Some(Left(error)), _) => Left(toErrorResponse(error))
        case (_, Some(Left(error))) => Left(toErrorResponse(error))
        case _                      => Right(IdleTimeoutConfigDto(timeout, warning))
    else IO.pure(Left(ErrorResponse("FORBIDDEN", "Admin role required")))
  }

  // GET /api/settings/admin - Get all settings (admin only)
  val getAllSettingsEndpoint = baseEndpoint.get
    .in("admin")
    .in(header[Option[String]]("X-User-Role"))
    .out(jsonBody[List[SystemSettingDto]])
    .description("Get all system settings including private ones (admin only)")

  def getAllSettingsRoute = getAllSettingsEndpoint.serverLogic { userRoleOpt =>
    // X-User-Role header is set by TokenAuthMiddleware from verified JWT
    if userRoleOpt.exists(_.equalsIgnoreCase("Admin")) then
      settingService.getAllSettings.map { settings =>
        Right(
          settings.map(s =>
            SystemSettingDto(
              id = s.id,
              key = s.key,
              value = s.value,
              settingType = s.settingType.toString,
              description = s.description,
              isPublic = s.isPublic
            )
          )
        )
      }
    else IO.pure(Left(ErrorResponse("FORBIDDEN", "Admin role required")))
  }

  // PUT /api/settings/admin/:key - Update a specific setting (admin only)
  val updateSettingEndpoint = baseEndpoint.put
    .in("admin" / path[String]("key"))
    .in(header[Option[String]]("X-User-Role"))
    .in(jsonBody[UpdateSettingDto])
    .out(jsonBody[SystemSettingDto])
    .description("Update a specific system setting (admin only)")

  def updateSettingRoute = updateSettingEndpoint.serverLogic { case (key, userRoleOpt, dto) =>
    import com.consultant.api.mappers.ErrorMappers.toErrorResponse
    // X-User-Role header is set by TokenAuthMiddleware from verified JWT
    if userRoleOpt.exists(_.equalsIgnoreCase("Admin")) then
      settingService.updateSetting(key, dto.value).map {
        case Right(Some(setting)) =>
          Right(
            SystemSettingDto(
              id = setting.id,
              key = setting.key,
              value = setting.value,
              settingType = setting.settingType.toString,
              description = setting.description,
              isPublic = setting.isPublic
            )
          )
        case Right(None) =>
          Left(ErrorResponse("SETTING_NOT_FOUND", s"Setting with key '$key' not found"))
        case Left(error) =>
          Left(toErrorResponse(error))
      }
    else IO.pure(Left(ErrorResponse("FORBIDDEN", "Admin role required")))
  }

  private val endpoints = List(
    getPublicSettingsRoute,
    getIdleTimeoutRoute,
    updateIdleTimeoutRoute,
    getAllSettingsRoute,
    updateSettingRoute
  )

  val routes: HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(endpoints)
