package com.consultant.core.ports

import cats.effect.IO
import com.consultant.core.domain.security.*
import java.util.UUID

/** Репозиторий для работы с учетными данными */
trait CredentialsRepository:
  def findByEmail(email: String): IO[Option[Credentials]]
  def findByUserId(userId: UUID): IO[Option[Credentials]]
  def create(credentials: Credentials): IO[Credentials]
  def update(credentials: Credentials): IO[Option[Credentials]]
  def incrementFailedAttempts(email: String): IO[Unit]
  def resetFailedAttempts(email: String): IO[Unit]
  def lockAccount(email: String, until: java.time.Instant): IO[Unit]

/** Репозиторий для refresh tokens */
trait RefreshTokenRepository:
  def create(token: RefreshToken): IO[RefreshToken]
  def findByToken(token: String): IO[Option[RefreshToken]]
  def findByUserId(userId: UUID): IO[List[RefreshToken]]
  def delete(token: String): IO[Boolean]
  def deleteByUserId(userId: UUID): IO[Int] // Logout from all devices

/** Репозиторий для аудита безопасности */
trait SecurityAuditRepository:
  def log(auditLog: SecurityAuditLog): IO[SecurityAuditLog]
  def findByUserId(userId: UUID, limit: Int = 100): IO[List[SecurityAuditLog]]
  def findFailedLogins(email: String, since: java.time.Instant): IO[List[SecurityAuditLog]]

/** Репозиторий для сессий */
trait SessionRepository:
  def create(session: UserSession): IO[UserSession]
  def findById(sessionId: String): IO[Option[UserSession]]
  def findByUserId(userId: UUID): IO[List[UserSession]]
  def update(session: UserSession): IO[Option[UserSession]]
  def delete(sessionId: String): IO[Boolean]
  def deleteExpired(): IO[Int]
