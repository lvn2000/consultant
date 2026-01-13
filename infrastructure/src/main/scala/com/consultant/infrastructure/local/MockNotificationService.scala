package com.consultant.infrastructure.local

import cats.effect.IO
import com.consultant.core.ports.NotificationService

// Mock notification service for development
class MockNotificationService extends NotificationService:

  override def sendEmail(to: String, subject: String, body: String): IO[Unit] =
    IO.println(s"[MOCK EMAIL] To: $to, Subject: $subject, Body: $body")

  override def sendSms(phoneNumber: String, message: String): IO[Unit] =
    IO.println(s"[MOCK SMS] To: $phoneNumber, Message: $message")
