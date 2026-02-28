package com.consultant.core.domain

import java.time.Instant
import java.util.UUID

// System setting types
enum SettingType:
  case Integer, String, Boolean, Json

// System Setting domain model
case class SystemSetting(
  id: UUID,
  key: String,
  value: String,
  settingType: SettingType,
  description: Option[String],
  isPublic: Boolean,
  createdAt: Instant,
  updatedAt: Instant
)

// Create/Update requests
case class CreateSystemSettingRequest(
  key: String,
  value: String,
  settingType: SettingType,
  description: Option[String] = None,
  isPublic: Boolean = true
)

case class UpdateSystemSettingRequest(
  value: String,
  description: Option[String] = None,
  isPublic: Option[Boolean] = None
)

// Helper methods to parse values
object SystemSetting:
  def parseIntValue(setting: SystemSetting): Option[Int] =
    try Some(setting.value.toInt)
    catch case _: NumberFormatException => None

  def parseBooleanValue(setting: SystemSetting): Option[Boolean] =
    setting.value.toLowerCase match
      case "true" | "yes" | "1" => Some(true)
      case "false" | "no" | "0" => Some(false)
      case _                    => None
