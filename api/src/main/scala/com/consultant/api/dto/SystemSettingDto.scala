package com.consultant.api.dto

import io.circe.Codec
import java.util.UUID

case class SystemSettingDto(
  id: UUID,
  key: String,
  value: String,
  settingType: String,
  description: Option[String],
  isPublic: Boolean
) derives Codec.AsObject

case class CreateSettingDto(
  key: String,
  value: String,
  settingType: String,
  description: Option[String] = None,
  isPublic: Boolean = true
) derives Codec.AsObject

case class UpdateSettingDto(
  value: String,
  description: Option[String] = None,
  isPublic: Option[Boolean] = None
) derives Codec.AsObject

case class IdleTimeoutConfigDto(
  idleTimeoutMinutes: Int,
  idleWarningMinutes: Int
) derives Codec.AsObject

case class UpdateIdleTimeoutDto(
  idleTimeoutMinutes: Option[Int] = None,
  idleWarningMinutes: Option[Int] = None
) derives Codec.AsObject
