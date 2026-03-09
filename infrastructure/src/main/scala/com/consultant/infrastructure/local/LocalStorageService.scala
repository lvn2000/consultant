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
package com.consultant.infrastructure.local

import cats.effect.IO
import com.consultant.core.ports.StorageService
import java.nio.file.{ Files, Paths, StandardOpenOption }

// Local file system implementation for development
class LocalStorageService(basePath: String) extends StorageService:

  override def uploadFile(key: String, content: Array[Byte], contentType: String): IO[String] =
    IO.blocking {
      val path = Paths.get(basePath, key)
      Files.createDirectories(path.getParent)
      Files.write(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
      s"file://$basePath/$key"
    }

  override def downloadFile(key: String): IO[Array[Byte]] =
    IO.blocking {
      val path = Paths.get(basePath, key)
      Files.readAllBytes(path)
    }

  override def deleteFile(key: String): IO[Unit] =
    IO.blocking {
      val path = Paths.get(basePath, key)
      Files.deleteIfExists(path)
      ()
    }

  override def generatePresignedUrl(key: String, expirationSeconds: Int): IO[String] =
    IO.pure(s"file://$basePath/$key")
