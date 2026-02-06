package com.consultant.data.repository

import cats.effect.IO
import cats.syntax.all.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import com.consultant.core.domain.*
import com.consultant.core.ports.NotificationPreferenceRepository
import java.util.UUID
import java.time.Instant

// Doobie meta instances for reading/writing NotificationPreference
object NotificationPreferenceMetaInstances:
  implicit val notificationTypeRead: Read[NotificationType] =
    Read[String].map(NotificationType.valueOf)

  implicit val notificationTypeWrite: Write[NotificationType] =
    Write[String].contramap(_.toString)

  implicit val notificationPreferenceRead: Read[NotificationPreference] =
    Read[(UUID, UUID, NotificationType, Boolean, Boolean, Instant, Instant)].map {
      case (id, userId, notificationType, emailEnabled, smsEnabled, createdAt, updatedAt) =>
        NotificationPreference(id, userId, notificationType, emailEnabled, smsEnabled, createdAt, updatedAt)
    }

class PostgresNotificationPreferenceRepository(xa: Transactor[IO]) extends NotificationPreferenceRepository:

  import NotificationPreferenceMetaInstances.*

  // Create default preferences for a user (all enabled by default)
  // Uses ON CONFLICT to make the operation idempotent and a single transaction for atomicity
  override def createDefaults(userId: UserId): IO[List[NotificationPreference]] =
    val defaults = NotificationPreference.defaultPreferences(userId)

    // Build a bulk insert with ON CONFLICT clause for idempotency
    val insertStatements = defaults.map { pref =>
      sql"""
          INSERT INTO notification_preferences (id, user_id, notification_type, email_enabled, sms_enabled, created_at, updated_at)
          VALUES (${pref.id}, ${pref.userId}, ${pref.notificationType.toString}, ${pref.emailEnabled}, ${pref.smsEnabled}, ${pref.createdAt}, ${pref.updatedAt})
          ON CONFLICT (user_id, notification_type) DO NOTHING
        """.update
    }

    // Execute all inserts in a single transaction
    insertStatements
      .traverse(_.run)
      .transact(xa)
      .map(_ => defaults)

  // Get preference for a specific notification type
  override def findByUserAndType(
    userId: UserId,
    notificationType: NotificationType
  ): IO[Option[NotificationPreference]] =
    sql"""
      SELECT id, user_id, notification_type, email_enabled, sms_enabled, created_at, updated_at
      FROM notification_preferences
      WHERE user_id = $userId AND notification_type = ${notificationType.toString}
    """.query[NotificationPreference].option.transact(xa)

  // Get all preferences for a user
  override def findByUser(userId: UserId): IO[List[NotificationPreference]] =
    sql"""
      SELECT id, user_id, notification_type, email_enabled, sms_enabled, created_at, updated_at
      FROM notification_preferences
      WHERE user_id = $userId
      ORDER BY notification_type
    """.query[NotificationPreference].to[List].transact(xa)

  // Update a preference
  override def update(preference: NotificationPreference): IO[NotificationPreference] =
    sql"""
      UPDATE notification_preferences
      SET email_enabled = ${preference.emailEnabled},
          sms_enabled = ${preference.smsEnabled},
          updated_at = ${Instant.now()}
      WHERE id = ${preference.id}
    """.update.run
      .transact(xa)
      .map(_ => preference.copy(updatedAt = Instant.now()))

  // Delete all preferences for a user
  override def deleteByUser(userId: UserId): IO[Unit] =
    sql"""
      DELETE FROM notification_preferences
      WHERE user_id = $userId
    """.update.run
      .transact(xa)
      .void
