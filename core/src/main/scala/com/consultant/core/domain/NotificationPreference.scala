package com.consultant.core.domain

import java.time.Instant
import java.util.UUID
import types.*

// Notification preference types
enum NotificationType:
  case ConsultationApproved
  case ConsultationDeclined
  case ConsultationCompleted
  case ConsultationMissed
  case ConsultationCancelled

// Notification preference domain model
case class NotificationPreference(
  id: UUID,
  userId: UserId,
  notificationType: NotificationType,
  emailEnabled: Boolean,
  smsEnabled: Boolean,
  createdAt: Instant,
  updatedAt: Instant
)

case class CreateNotificationPreferenceRequest(
  userId: UserId,
  notificationType: NotificationType,
  emailEnabled: Boolean,
  smsEnabled: Boolean = false // SMS disabled by default for now
)

case class UpdateNotificationPreferenceRequest(
  emailEnabled: Boolean,
  smsEnabled: Boolean
)

// Default preferences for a user (all notifications enabled)
object NotificationPreference:
  def defaultPreferences(userId: UserId): List[NotificationPreference] =
    val now = Instant.now()
    NotificationType.values.toList.map { notificationType =>
      NotificationPreference(
        id = UUID.randomUUID(),
        userId = userId,
        notificationType = notificationType,
        emailEnabled = true,
        smsEnabled = false,
        createdAt = now,
        updatedAt = now
      )
    }
