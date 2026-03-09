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
import com.consultant.core.ports.StorageService
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.core.sync.RequestBody
import java.time.Duration

class AwsS3StorageService(
  s3Client: S3Client,
  presigner: S3Presigner,
  bucketName: String
) extends StorageService:

  override def uploadFile(key: String, content: Array[Byte], contentType: String): IO[String] =
    IO.blocking {
      val request = PutObjectRequest
        .builder()
        .bucket(bucketName)
        .key(key)
        .contentType(contentType)
        .build()

      s3Client.putObject(request, RequestBody.fromBytes(content))
      s"s3://$bucketName/$key"
    }

  override def downloadFile(key: String): IO[Array[Byte]] =
    IO.blocking {
      val request = GetObjectRequest
        .builder()
        .bucket(bucketName)
        .key(key)
        .build()

      s3Client.getObjectAsBytes(request).asByteArray()
    }

  override def deleteFile(key: String): IO[Unit] =
    IO.blocking {
      val request = DeleteObjectRequest
        .builder()
        .bucket(bucketName)
        .key(key)
        .build()

      s3Client.deleteObject(request)
      ()
    }

  override def generatePresignedUrl(key: String, expirationSeconds: Int): IO[String] =
    IO.blocking {
      val getObjectRequest = GetObjectRequest
        .builder()
        .bucket(bucketName)
        .key(key)
        .build()

      val presignRequest = GetObjectPresignRequest
        .builder()
        .signatureDuration(Duration.ofSeconds(expirationSeconds.toLong))
        .getObjectRequest(getObjectRequest)
        .build()

      presigner.presignGetObject(presignRequest).url().toString
    }
