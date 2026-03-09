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

// Default preferences for a user (email enabled for all notification types, SMS disabled)
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
