package com.consultant.infrastructure.security

import cats.effect.IO
import cats.syntax.all.*
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import java.security.SecureRandom
import java.util.Base64

/** Хеширование паролей с использованием PBKDF2 */
class PasswordHashingService:

  private val algorithm  = "PBKDF2WithHmacSHA512"
  private val iterations = 210000 // OWASP рекомендация для 2024+
  private val keyLength  = 512
  private val saltLength = 32

  private val secureRandom = new SecureRandom()
  private val encoder      = Base64.getEncoder
  private val decoder      = Base64.getDecoder

  /** Генерирует криптографически стойкую соль */
  def generateSalt(): IO[String] =
    IO {
      val salt = new Array[Byte](saltLength)
      secureRandom.nextBytes(salt)
      encoder.encodeToString(salt)
    }

  /** Хеширует пароль с солью */
  def hashPassword(password: String, salt: String): IO[String] =
    IO {
      val saltBytes = decoder.decode(salt)
      val spec      = new PBEKeySpec(password.toCharArray, saltBytes, iterations, keyLength)
      val factory   = SecretKeyFactory.getInstance(algorithm)
      val hash      = factory.generateSecret(spec).getEncoded
      encoder.encodeToString(hash)
    }

  /** Верифицирует пароль */
  def verifyPassword(password: String, hash: String, salt: String): IO[Boolean] =
    hashPassword(password, salt).map(_ == hash).handleErrorWith { error =>
      IO.println(s"Password verification error: ${error.getMessage}") *> IO.pure(false)
    }

  /** Проверяет сложность пароля */
  def validatePasswordStrength(password: String): IO[Either[String, Unit]] =
    IO {
      if password.length < 8 then Left("Password must be at least 8 characters long")
      else if !password.exists(_.isLower) then Left("Password must contain at least one lowercase letter")
      else if !password.exists(_.isUpper) then Left("Password must contain at least one uppercase letter")
      else if !password.exists(_.isDigit) then Left("Password must contain at least one digit")
      else if !password.exists(c => !c.isLetterOrDigit) then
        Left("Password must contain at least one special character")
      else Right(())
    }

  /** Генерирует безопасный случайный токен */
  def generateSecureToken(length: Int = 32): IO[String] =
    IO {
      val bytes = new Array[Byte](length)
      secureRandom.nextBytes(bytes)
      encoder.encodeToString(bytes)
    }
