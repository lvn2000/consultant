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
