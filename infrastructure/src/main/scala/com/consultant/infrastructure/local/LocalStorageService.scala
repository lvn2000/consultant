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
