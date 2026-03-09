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
