package com.consultant.api.di

import com.consultant.core.service.*

/**
 * ServiceModule provides all business logic services.
 *
 * This layer depends on both RepositoryModule and InfrastructureModule.
 */
trait ServiceModule extends InfrastructureModule:

  lazy val userService: UserService =
    UserService(userRepo, sessionRepo, Some(notificationPreferenceRepo))

  lazy val specialistService: SpecialistService =
    SpecialistService(specialistRepo, categoryRepo)

  lazy val consultationService: ConsultationService =
    ConsultationService(
      consultationRepo,
      specialistRepo,
      userRepo,
      notificationService,
      notificationPreferenceRepo
    )

  lazy val categoryService: CategoryService =
    CategoryService(categoryRepo)

  lazy val connectionService: ConnectionService =
    ConnectionService(connectionRepo, connectionTypeRepo, specialistRepo)

  lazy val availabilityService: AvailabilityService =
    AvailabilityService(availabilityRepo, consultationRepo)

  lazy val systemSettingService: SystemSettingService =
    new SystemSettingService(systemSettingRepo)

end ServiceModule
