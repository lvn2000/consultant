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
