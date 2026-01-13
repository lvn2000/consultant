package com.consultant.core.domain

import java.util.UUID
import java.time.Instant

object security:

  // Роли пользователей
  enum UserRole:
    case Client     // Обычный клиент
    case Specialist // Специалист
    case Admin      // Администратор

  // Права доступа
  enum Permission:
    case ReadUser
    case WriteUser
    case DeleteUser
    case ReadSpecialist
    case WriteSpecialist
    case ManageConsultations
    case ManageCategories
    case AdminAccess

  // Токен аутентификации
  case class AuthToken(
    token: String,
    userId: UUID,
    role: UserRole,
    expiresAt: Instant
  ):
    def isExpired: Boolean = Instant.now().isAfter(expiresAt)

  // Refresh token для продления сессии
  case class RefreshToken(
    token: String,
    userId: UUID,
    expiresAt: Instant,
    createdAt: Instant = Instant.now()
  ):
    def isExpired: Boolean = Instant.now().isAfter(expiresAt)

  // Учетные данные
  case class Credentials(
    email: String,
    passwordHash: String, // BCrypt/Argon2 hash
    salt: String,
    userId: UUID,
    role: UserRole,
    isActive: Boolean = true,
    lastLogin: Option[Instant] = None,
    failedLoginAttempts: Int = 0,
    lockedUntil: Option[Instant] = None
  ):
    def isLocked: Boolean =
      lockedUntil.exists(until => Instant.now().isBefore(until))

  // Аудит безопасности
  case class SecurityAuditLog(
    id: UUID,
    userId: UUID,
    action: String,
    ipAddress: String,
    userAgent: String,
    success: Boolean,
    timestamp: Instant = Instant.now(),
    details: Option[String] = None
  )

  // Сессия пользователя
  case class UserSession(
    sessionId: String,
    userId: UUID,
    role: UserRole,
    ipAddress: String,
    userAgent: String,
    createdAt: Instant,
    lastActivity: Instant,
    expiresAt: Instant
  ):
    def isExpired: Boolean = Instant.now().isAfter(expiresAt)
    def isActive: Boolean  = !isExpired

export security.*
