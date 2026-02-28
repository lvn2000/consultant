package com.consultant.core.ports

import cats.effect.IO
import com.consultant.core.domain.{ SettingType, SystemSetting }
import java.util.UUID

trait SystemSettingRepository:
  def findById(id: UUID): IO[Option[SystemSetting]]
  def findByKey(key: String): IO[Option[SystemSetting]]
  def findAll: IO[List[SystemSetting]]
  def findPublicSettings: IO[List[SystemSetting]]
  def create(setting: SystemSetting): IO[Unit]
  def update(id: UUID, value: String, description: Option[String], isPublic: Option[Boolean]): IO[Option[SystemSetting]]
  def delete(id: UUID): IO[Boolean]
  def getByKey(key: String): IO[Option[SystemSetting]]
  def upsert(
    key: String,
    value: String,
    settingType: SettingType,
    description: Option[String],
    isPublic: Boolean
  ): IO[SystemSetting]
