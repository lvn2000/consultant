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
package com.consultant.infrastructure.security

import cats.effect.IO
import cats.syntax.all.*
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import java.security.SecureRandom
import java.util.Base64

/** Password hashing using PBKDF2 */
class PasswordHashingService:

  private val algorithm  = "PBKDF2WithHmacSHA512"
  private val iterations = 210000 // OWASP recommendation for 2024+
  private val keyLength  = 512
  private val saltLength = 32

  private val secureRandom = new SecureRandom()
  private val encoder      = Base64.getEncoder
  private val decoder      = Base64.getDecoder

  /** Generates cryptographically strong salt */
  def generateSalt(): IO[String] =
    IO {
      val salt = new Array[Byte](saltLength)
      secureRandom.nextBytes(salt)
      encoder.encodeToString(salt)
    }

  /** Hashes password with salt */
  def hashPassword(password: String, salt: String): IO[String] =
    IO {
      val saltBytes = decoder.decode(salt)
      val spec      = new PBEKeySpec(password.toCharArray, saltBytes, iterations, keyLength)
      val factory   = SecretKeyFactory.getInstance(algorithm)
      val hash      = factory.generateSecret(spec).getEncoded
      encoder.encodeToString(hash)
    }

  /** Verifies password */
  def verifyPassword(password: String, hash: String, salt: String): IO[Boolean] =
    hashPassword(password, salt).map(_ == hash).handleErrorWith { error =>
      IO.println(s"Password verification error: ${error.getMessage}") *> IO.pure(false)
    }

  /** Validates password complexity */
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

  /** Generates secure random token */
  def generateSecureToken(length: Int = 32): IO[String] =
    IO {
      val bytes = new Array[Byte](length)
      secureRandom.nextBytes(bytes)
      encoder.encodeToString(bytes)
    }
