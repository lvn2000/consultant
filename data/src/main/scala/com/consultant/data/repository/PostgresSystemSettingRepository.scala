package com.consultant.data.repository

import cats.effect.IO
import com.consultant.core.domain.{ SettingType, SystemSetting }
import com.consultant.core.ports.SystemSettingRepository
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.transactor.Transactor
import java.util.UUID
import java.time.Instant

class PostgresSystemSettingRepository(xa: Transactor[IO]) extends SystemSettingRepository:

  def findById(id: UUID): IO[Option[SystemSetting]] =
    fr"SELECT id, setting_key, setting_value, setting_type, description, is_public, created_at, updated_at FROM system_settings WHERE id = $id"
      .query[SystemSettingRow]
      .option
      .transact(xa)
      .map(_.map(_.toDomain))

  def findByKey(key: String): IO[Option[SystemSetting]] =
    fr"SELECT id, setting_key, setting_value, setting_type, description, is_public, created_at, updated_at FROM system_settings WHERE setting_key = $key"
      .query[SystemSettingRow]
      .option
      .transact(xa)
      .map(_.map(_.toDomain))

  def findAll: IO[List[SystemSetting]] =
    fr"SELECT id, setting_key, setting_value, setting_type, description, is_public, created_at, updated_at FROM system_settings ORDER BY setting_key"
      .query[SystemSettingRow]
      .to[List]
      .transact(xa)
      .map(_.map(_.toDomain))

  def findPublicSettings: IO[List[SystemSetting]] =
    fr"SELECT id, setting_key, setting_value, setting_type, description, is_public, created_at, updated_at FROM system_settings WHERE is_public = true ORDER BY setting_key"
      .query[SystemSettingRow]
      .to[List]
      .transact(xa)
      .map(_.map(_.toDomain))

  def create(setting: SystemSetting): IO[Unit] =
    sql"""
      INSERT INTO system_settings (id, setting_key, setting_value, setting_type, description, is_public, created_at, updated_at)
      VALUES (${setting.id}, ${setting.key}, ${setting.value}, ${setting.settingType.toString}, ${setting.description}, ${setting.isPublic}, ${setting.createdAt}, ${setting.updatedAt})
    """.update.run.transact(xa).void

  def update(
    id: UUID,
    value: String,
    description: Option[String],
    isPublic: Option[Boolean]
  ): IO[Option[SystemSetting]] =
    for
      _ <- sql"""
        UPDATE system_settings
        SET setting_value = $value,
            description = $description,
            is_public = COALESCE($isPublic, is_public),
            updated_at = NOW()
        WHERE id = $id
      """.update.run.transact(xa)
      setting <- findById(id)
    yield setting

  def delete(id: UUID): IO[Boolean] =
    sql"""
      DELETE FROM system_settings WHERE id = $id
    """.update.run.transact(xa).map(_ > 0)

  def getByKey(key: String): IO[Option[SystemSetting]] = findByKey(key)

  def upsert(
    key: String,
    value: String,
    settingType: SettingType,
    description: Option[String],
    isPublic: Boolean
  ): IO[SystemSetting] =
    for
      existing <- findByKey(key)
      result <- existing match
        case Some(setting) =>
          update(setting.id, value, description, Some(isPublic)).map(_.get)
        case None =>
          val newSetting = SystemSetting(
            id = UUID.randomUUID(),
            key = key,
            value = value,
            settingType = settingType,
            description = description,
            isPublic = isPublic,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
          )
          create(newSetting).as(newSetting)
    yield result

// Helper case class for database row mapping
case class SystemSettingRow(
  id: UUID,
  settingKey: String,
  settingValue: String,
  settingType: String,
  description: Option[String],
  isPublic: Boolean,
  createdAt: Instant,
  updatedAt: Instant
):
  def toDomain: SystemSetting =
    val stype = settingType.toLowerCase match
      case "integer" => SettingType.Integer
      case "boolean" => SettingType.Boolean
      case "json"    => SettingType.Json
      case _         => SettingType.String

    SystemSetting(
      id = id,
      key = settingKey,
      value = settingValue,
      settingType = stype,
      description = description,
      isPublic = isPublic,
      createdAt = createdAt,
      updatedAt = updatedAt
    )

// Given instance for Doobie Read
given Get[SettingType] = Get[String].temap {
  case "Integer" => Right(SettingType.Integer)
  case "Boolean" => Right(SettingType.Boolean)
  case "Json"    => Right(SettingType.Json)
  case _         => Right(SettingType.String)
}
