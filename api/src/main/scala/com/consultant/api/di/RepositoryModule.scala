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
package com.consultant.api.di

import cats.effect.IO
import doobie.util.transactor.Transactor
import com.consultant.core.ports.*
import com.consultant.data.repository.*
import com.consultant.data.transaction.{ DoobieUnitOfWork, UnitOfWork }

/**
 * RepositoryModule provides all repository dependencies.
 *
 * This trait defines the repository layer using lazy vals to ensure proper initialization order and allow for easy
 * testing through mixin composition.
 */
trait RepositoryModule:
  def xa: Transactor[IO]

  // Transaction management
  lazy val unitOfWork: UnitOfWork = DoobieUnitOfWork(xa)

  // User & Authentication repositories
  lazy val userRepo: UserRepository                 = PostgresUserRepository(xa)
  lazy val sessionRepo: SessionRepository           = PostgresSessionRepository(xa)
  lazy val credentialsRepo: CredentialsRepository   = PostgresCredentialsRepository(xa)
  lazy val refreshTokenRepo: RefreshTokenRepository = PostgresRefreshTokenRepository(xa)
  lazy val auditRepo: SecurityAuditRepository       = PostgresSecurityAuditRepository(xa)

  // Connection repositories
  lazy val connectionTypeRepo: ConnectionTypeRepository = PostgresConnectionTypeRepository(xa)
  lazy val connectionRepo: ConnectionRepository         = PostgresConnectionRepository(xa)

  // Business entity repositories
  lazy val specialistRepo: SpecialistRepository     = PostgresSpecialistRepository(xa, connectionRepo)
  lazy val categoryRepo: CategoryRepository         = PostgresCategoryRepository(xa)
  lazy val consultationRepo: ConsultationRepository = PostgresConsultationRepository(xa)
  lazy val availabilityRepo: AvailabilityRepository = PostgresAvailabilityRepository(xa)
  lazy val notificationPreferenceRepo: NotificationPreferenceRepository =
    PostgresNotificationPreferenceRepository(xa)
  lazy val systemSettingRepo: SystemSettingRepository = PostgresSystemSettingRepository(xa)

end RepositoryModule
