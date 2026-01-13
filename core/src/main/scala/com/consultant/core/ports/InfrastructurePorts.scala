package com.consultant.core.ports

import cats.effect.IO

// Infrastructure ports (abstractions for external services)
// Can be implemented for local development or AWS services

trait StorageService:
  def uploadFile(key: String, content: Array[Byte], contentType: String): IO[String]
  def downloadFile(key: String): IO[Array[Byte]]
  def deleteFile(key: String): IO[Unit]
  def generatePresignedUrl(key: String, expirationSeconds: Int): IO[String]

trait NotificationService:
  def sendEmail(to: String, subject: String, body: String): IO[Unit]
  def sendSms(phoneNumber: String, message: String): IO[Unit]

// Simple queue service with String messages
// For typed messages, use specific implementations with circe encoders/decoders
trait QueueService:
  def sendMessage(queueName: String, message: String): IO[Unit]
  def receiveMessages(queueName: String, maxMessages: Int): IO[List[String]]

trait CacheService:
  def get(key: String): IO[Option[String]]
  def set(key: String, value: String, ttlSeconds: Int): IO[Unit]
  def delete(key: String): IO[Unit]
