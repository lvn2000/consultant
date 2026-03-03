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
