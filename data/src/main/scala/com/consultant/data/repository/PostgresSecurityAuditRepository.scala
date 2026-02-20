package com.consultant.data.repository

import cats.effect.IO
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import com.consultant.core.domain.security.SecurityAuditLog
import com.consultant.core.ports.SecurityAuditRepository
import java.util.UUID
import java.time.Instant

/** PostgreSQL implementation of SecurityAuditRepository */
class PostgresSecurityAuditRepository(xa: Transactor[IO]) extends SecurityAuditRepository:

  override def log(auditLog: SecurityAuditLog): IO[SecurityAuditLog] =
    sql"""
      INSERT INTO security_audit_log (id, user_id, action, ip_address, user_agent, success, timestamp, details)
      VALUES (${auditLog.id.toString}::uuid, ${auditLog.userId.toString}::uuid, ${auditLog.action}, ${auditLog.ipAddress},
              ${auditLog.userAgent}, ${auditLog.success}, ${auditLog.timestamp}, ${auditLog.details})
    """.update.run
      .transact(xa)
      .as(auditLog)

  override def findByUserId(userId: UUID, limit: Int = 100): IO[List[SecurityAuditLog]] =
    sql"""
      SELECT id, user_id, action, ip_address, user_agent, success, timestamp, details
      FROM security_audit_log
      WHERE user_id = $userId
      ORDER BY timestamp DESC
      LIMIT $limit
    """
      .query[(UUID, UUID, String, String, String, Boolean, Instant, Option[String])]
      .to[List]
      .map(_.map(toAuditLog))
      .transact(xa)

  override def findFailedLogins(email: String, since: Instant): IO[List[SecurityAuditLog]] =
    sql"""
      SELECT sal.id, sal.user_id, sal.action, sal.ip_address, sal.user_agent,
             sal.success, sal.timestamp, sal.details
      FROM security_audit_log sal
      JOIN credentials c ON sal.user_id = c.user_id
      WHERE c.email = $email
        AND sal.action = 'LOGIN_FAILED'
        AND sal.timestamp >= $since
      ORDER BY sal.timestamp DESC
    """
      .query[(UUID, UUID, String, String, String, Boolean, Instant, Option[String])]
      .to[List]
      .map(_.map(toAuditLog))
      .transact(xa)

  private def toAuditLog(
    t: (UUID, UUID, String, String, String, Boolean, Instant, Option[String])
  ): SecurityAuditLog =
    SecurityAuditLog(
      id = t._1,
      userId = t._2,
      action = t._3,
      ipAddress = t._4,
      userAgent = t._5,
      success = t._6,
      timestamp = t._7,
      details = t._8
    )
