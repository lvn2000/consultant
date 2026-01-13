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
