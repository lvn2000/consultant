package com.consultant.core.domain

import java.util.UUID
import java.time.Instant

object security:

  // User roles
  enum UserRole:
    case Client     // Regular client
    case Specialist // Specialist
    case Admin      // Administrator

  // Access permissions
  enum Permission:
    case ReadUser
    case WriteUser
    case DeleteUser
    case ReadSpecialist
    case WriteSpecialist
    case ManageConsultations
    case ManageCategories
    case AdminAccess

  // Authentication token
  case class AuthToken(
    token: String,
    userId: UUID,
    role: UserRole,
    expiresAt: Instant
  ):
    def isExpired: Boolean = Instant.now().isAfter(expiresAt)

  // Refresh token for session renewal
  case class RefreshToken(
    token: String,
    userId: UUID,
    expiresAt: Instant,
    createdAt: Instant = Instant.now()
  ):
    def isExpired: Boolean = Instant.now().isAfter(expiresAt)

  // Credentials
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

  // Security audit
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

  // User session
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
