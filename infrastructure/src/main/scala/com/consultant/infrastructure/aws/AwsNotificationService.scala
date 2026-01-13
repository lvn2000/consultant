package com.consultant.infrastructure.aws

import cats.effect.IO
import com.consultant.core.ports.NotificationService
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.services.ses.model.*

class AwsNotificationService(
  snsClient: SnsClient,
  sesClient: SesClient,
  senderEmail: String
) extends NotificationService:

  override def sendEmail(to: String, subject: String, body: String): IO[Unit] =
    IO.blocking {
      val destination = Destination
        .builder()
        .toAddresses(to)
        .build()

      val content = Content
        .builder()
        .data(body)
        .build()

      val subjectContent = Content
        .builder()
        .data(subject)
        .build()

      val emailBody = Body
        .builder()
        .text(content)
        .build()

      val message = Message
        .builder()
        .subject(subjectContent)
        .body(emailBody)
        .build()

      val request = SendEmailRequest
        .builder()
        .source(senderEmail)
        .destination(destination)
        .message(message)
        .build()

      sesClient.sendEmail(request)
      ()
    }

  override def sendSms(phoneNumber: String, message: String): IO[Unit] =
    IO.blocking {
      val request = PublishRequest
        .builder()
        .phoneNumber(phoneNumber)
        .message(message)
        .build()

      snsClient.publish(request)
      ()
    }
