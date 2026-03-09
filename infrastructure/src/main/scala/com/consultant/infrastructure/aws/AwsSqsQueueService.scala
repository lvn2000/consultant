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
import com.consultant.core.ports.QueueService
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.*
import scala.jdk.CollectionConverters.*

class AwsSqsQueueService(sqsClient: SqsClient) extends QueueService:

  override def sendMessage(queueName: String, message: String): IO[Unit] =
    IO.blocking {
      val queueUrl = getQueueUrl(queueName)

      val request = SendMessageRequest
        .builder()
        .queueUrl(queueUrl)
        .messageBody(message)
        .build()

      sqsClient.sendMessage(request)
      ()
    }

  override def receiveMessages(queueName: String, maxMessages: Int): IO[List[String]] =
    IO.blocking {
      val queueUrl = getQueueUrl(queueName)

      val request = ReceiveMessageRequest
        .builder()
        .queueUrl(queueUrl)
        .maxNumberOfMessages(maxMessages)
        .waitTimeSeconds(10)
        .build()

      val response = sqsClient.receiveMessage(request)

      response.messages().asScala.toList.map(_.body())
    }

  private def getQueueUrl(queueName: String): String =
    val request = GetQueueUrlRequest
      .builder()
      .queueName(queueName)
      .build()

    sqsClient.getQueueUrl(request).queueUrl()
