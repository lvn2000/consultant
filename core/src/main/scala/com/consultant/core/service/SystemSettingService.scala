package com.consultant.core.service

import cats.effect.IO
import cats.syntax.all.*
import com.consultant.core.domain.{ DomainError, SettingType, SystemSetting }
import com.consultant.core.ports.SystemSettingRepository
import java.util.UUID

class SystemSettingService(repo: SystemSettingRepository):

  def getSetting(key: String): IO[Option[SystemSetting]] =
    repo.findByKey(key)

  def getIntSetting(key: String): IO[Option[Int]] =
    repo.findByKey(key).map {
      case Some(setting) =>
        try Some(setting.value.toInt)
        catch case _: NumberFormatException => None
      case None => None
    }

  def getAllSettings: IO[List[SystemSetting]] =
    repo.findAll

  def getPublicSettings: IO[List[SystemSetting]] =
    repo.findPublicSettings

  def updateSetting(key: String, value: String): IO[Either[DomainError, Option[SystemSetting]]] =
    repo.findByKey(key).flatMap {
      case Some(setting) =>
        repo
          .update(setting.id, value, setting.description, None)
          .map(Right(_))
          .handleError(parseError)
      case None =>
        IO.pure(Right(None))
    }

  def createOrUpdateSetting(
    key: String,
    value: String,
    settingType: SettingType,
    description: Option[String] = None,
    isPublic: Boolean = true
  ): IO[Either[DomainError, SystemSetting]] =
    repo
      .upsert(key, value, settingType, description, isPublic)
      .map(Right(_))
      .handleError(parseError)

  def deleteSetting(key: String): IO[Either[DomainError, Boolean]] =
    repo.findByKey(key).flatMap {
      case Some(setting) => repo.delete(setting.id).map(Right(_)).handleError(parseError)
      case None          => IO.pure(Left(DomainError.ValidationError(s"Setting not found: $key")))
    }

  /** Parses database errors into structured domain errors */
  private def parseError(error: Throwable): Either[DomainError, Nothing] =
    import com.consultant.core.error.PostgresErrorParser
    Left(PostgresErrorParser.parseError(error))

  // Convenience methods for common settings
  def getIdleTimeoutMinutes: IO[Int] =
    getIntSetting("idle_timeout_minutes").map(_.getOrElse(30))

  def getIdleWarningMinutes: IO[Int] =
    getIntSetting("idle_warning_minutes").map(_.getOrElse(5))

  def setIdleTimeout(minutes: Int): IO[Either[DomainError, SystemSetting]] =
    createOrUpdateSetting(
      "idle_timeout_minutes",
      minutes.toString,
      SettingType.Integer,
      Some("Number of minutes of user inactivity before automatic logout"),
      true
    )

  def setIdleWarning(minutes: Int): IO[Either[DomainError, SystemSetting]] =
    createOrUpdateSetting(
      "idle_warning_minutes",
      minutes.toString,
      SettingType.Integer,
      Some("Number of minutes before timeout to show warning to user"),
      true
    )
