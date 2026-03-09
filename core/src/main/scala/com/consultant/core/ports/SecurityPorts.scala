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
import com.consultant.core.domain.security.*
import java.util.UUID

/** Repository for working with credentials */
trait CredentialsRepository:
  def findByEmail(email: String): IO[Option[Credentials]]
  def findByLogin(login: String): IO[Option[Credentials]]
  def findByUserId(userId: UUID): IO[Option[Credentials]]
  def create(credentials: Credentials): IO[Credentials]
  def update(credentials: Credentials): IO[Option[Credentials]]
  def incrementFailedAttempts(email: String): IO[Unit]
  def resetFailedAttempts(email: String): IO[Unit]
  def lockAccount(email: String, until: java.time.Instant): IO[Unit]

/** Repository for refresh tokens */
trait RefreshTokenRepository:
  def create(token: RefreshToken): IO[RefreshToken]
  def findByToken(token: String): IO[Option[RefreshToken]]
  def findByUserId(userId: UUID): IO[List[RefreshToken]]
  def delete(token: String): IO[Boolean]
  def deleteByUserId(userId: UUID): IO[Int] // Logout from all devices

/** Repository for security audit */
trait SecurityAuditRepository:
  def log(auditLog: SecurityAuditLog): IO[SecurityAuditLog]
  def findByUserId(userId: UUID, limit: Int = 100): IO[List[SecurityAuditLog]]
  def findFailedLogins(email: String, since: java.time.Instant): IO[List[SecurityAuditLog]]

/** Repository for sessions */
trait SessionRepository:
  def create(session: UserSession): IO[UserSession]
  def findById(sessionId: String): IO[Option[UserSession]]
  def findByUserId(userId: UUID): IO[List[UserSession]]
  def update(session: UserSession): IO[Option[UserSession]]
  def delete(sessionId: String): IO[Boolean]
  def deleteExpired(): IO[Int]
