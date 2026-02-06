package com.consultant.api.dto

import com.consultant.core.domain.NotificationType
import io.circe.Codec
import java.time.Instant
import java.util.UUID

case class NotificationPreferenceDto(
  id: UUID,
  userId: UUID,
  notificationType: String,
  emailEnabled: Boolean,
  smsEnabled: Boolean,
  createdAt: Instant,
  updatedAt: Instant
) derives Codec.AsObject

case class UpdateNotificationPreferenceDto(
  emailEnabled: Boolean,
  smsEnabled: Boolean
) derives Codec.AsObject

case class UserNotificationPreferencesDto(
  userId: UUID,
  preferences: List[NotificationPreferenceDto]
) derives Codec.AsObject
