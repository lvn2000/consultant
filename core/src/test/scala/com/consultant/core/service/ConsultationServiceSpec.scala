package com.consultant.core.service

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalamock.scalatest.MockFactory
import cats.effect.IO
import com.consultant.core.domain._
import com.consultant.core.ports._
import cats.effect.unsafe.implicits.global
import java.time.Instant

class ConsultationServiceSpec extends AnyFlatSpec with Matchers with MockFactory {

  private def createMockUser(id: java.util.UUID, email: String): User =
    User(
      id = id,
      login = "userlogin",
      email = email,
      name = "User Name",
      phone = None,
      role = security.UserRole.Client,
      countryId = Some(java.util.UUID.randomUUID()),
      languages = Set(java.util.UUID.randomUUID()),
      createdAt = Instant.now(),
      updatedAt = Instant.now()
    )

  private def createMockSpecialist(
    id: java.util.UUID,
    email: String,
    categoryId: java.util.UUID,
    isAvailable: Boolean = true
  ): Specialist =
    Specialist(
      id = id,
      email = email,
      name = "Specialist Name",
      phone = "+1234567890",
      bio = "Specialist bio",
      categoryRates = List(
        SpecialistCategoryRate(
          categoryId = categoryId,
          hourlyRate = BigDecimal("50.00"),
          experienceYears = 5,
          rating = None,
          totalConsultations = 0
        )
      ),
      isAvailable = isAvailable,
      connections = List(),
      countryId = Some(java.util.UUID.randomUUID()),
      languages = Set(java.util.UUID.randomUUID()),
      createdAt = Instant.now(),
      updatedAt = Instant.now()
    )

  "createConsultation" should "return error if user not found" in {
    val consultationRepo           = mock[ConsultationRepository]
    val specialistRepo             = mock[SpecialistRepository]
    val userRepo                   = mock[UserRepository]
    val notificationService        = mock[NotificationService]
    val notificationPreferenceRepo = mock[NotificationPreferenceRepository]
    val service = new ConsultationService(
      consultationRepo,
      specialistRepo,
      userRepo,
      notificationService,
      notificationPreferenceRepo
    )
    val userId       = java.util.UUID.randomUUID()
    val specialistId = java.util.UUID.randomUUID()
    val categoryId   = java.util.UUID.randomUUID()
    val req          = CreateConsultationRequest(userId, specialistId, categoryId, "desc", Instant.now())

    userRepo.findById.expects(req.userId).returning(IO.pure(None))
    specialistRepo.findById.expects(req.specialistId).returning(IO.pure(None))
    val result = service.createConsultation(req).unsafeRunSync()

    result.shouldBe(Left(DomainError.UserNotFound(req.userId)))
  }

  it should "return error if specialist not found" in {
    val consultationRepo           = mock[ConsultationRepository]
    val specialistRepo             = mock[SpecialistRepository]
    val userRepo                   = mock[UserRepository]
    val notificationService        = mock[NotificationService]
    val notificationPreferenceRepo = mock[NotificationPreferenceRepository]
    val service = new ConsultationService(
      consultationRepo,
      specialistRepo,
      userRepo,
      notificationService,
      notificationPreferenceRepo
    )
    val userId       = java.util.UUID.randomUUID()
    val specialistId = java.util.UUID.randomUUID()
    val categoryId   = java.util.UUID.randomUUID()
    val req          = CreateConsultationRequest(userId, specialistId, categoryId, "desc", Instant.now())
    val user         = createMockUser(userId, "user@example.com")

    userRepo.findById.expects(req.userId).returning(IO.pure(Some(user)))
    specialistRepo.findById.expects(req.specialistId).returning(IO.pure(None))
    val result = service.createConsultation(req).unsafeRunSync()

    result.shouldBe(Left(DomainError.SpecialistNotFound(req.specialistId)))
  }

  it should "return error if specialist is not available for category" in {
    val consultationRepo           = mock[ConsultationRepository]
    val specialistRepo             = mock[SpecialistRepository]
    val userRepo                   = mock[UserRepository]
    val notificationService        = mock[NotificationService]
    val notificationPreferenceRepo = mock[NotificationPreferenceRepository]
    val service = new ConsultationService(
      consultationRepo,
      specialistRepo,
      userRepo,
      notificationService,
      notificationPreferenceRepo
    )
    val userId          = java.util.UUID.randomUUID()
    val specialistId    = java.util.UUID.randomUUID()
    val categoryId      = java.util.UUID.randomUUID()
    val otherCategoryId = java.util.UUID.randomUUID()
    val req             = CreateConsultationRequest(userId, specialistId, categoryId, "desc", Instant.now())
    val user            = createMockUser(userId, "user@example.com")
    val specialist      = createMockSpecialist(specialistId, "specialist@example.com", otherCategoryId)

    userRepo.findById.expects(req.userId).returning(IO.pure(Some(user)))
    specialistRepo.findById.expects(req.specialistId).returning(IO.pure(Some(specialist)))
    val result = service.createConsultation(req).unsafeRunSync()

    result.isLeft shouldBe true
    result.left.toOption.get match
      case DomainError.ValidationError(msg) => msg.contains("not available for category") shouldBe true
      case _                                => fail("Expected ValidationError")
  }

  it should "return error if specialist is not available" in {
    val consultationRepo           = mock[ConsultationRepository]
    val specialistRepo             = mock[SpecialistRepository]
    val userRepo                   = mock[UserRepository]
    val notificationService        = mock[NotificationService]
    val notificationPreferenceRepo = mock[NotificationPreferenceRepository]
    val service = new ConsultationService(
      consultationRepo,
      specialistRepo,
      userRepo,
      notificationService,
      notificationPreferenceRepo
    )
    val userId       = java.util.UUID.randomUUID()
    val specialistId = java.util.UUID.randomUUID()
    val categoryId   = java.util.UUID.randomUUID()
    val req          = CreateConsultationRequest(userId, specialistId, categoryId, "desc", Instant.now())
    val user         = createMockUser(userId, "user@example.com")
    val specialist   = createMockSpecialist(specialistId, "specialist@example.com", categoryId, isAvailable = false)

    userRepo.findById.expects(req.userId).returning(IO.pure(Some(user)))
    specialistRepo.findById.expects(req.specialistId).returning(IO.pure(Some(specialist)))
    val result = service.createConsultation(req).unsafeRunSync()

    result.shouldBe(Left(DomainError.SpecialistNotAvailable(specialistId)))
  }

  it should "successfully create consultation" in {
    val consultationRepo           = mock[ConsultationRepository]
    val specialistRepo             = mock[SpecialistRepository]
    val userRepo                   = mock[UserRepository]
    val notificationService        = mock[NotificationService]
    val notificationPreferenceRepo = mock[NotificationPreferenceRepository]
    val service = new ConsultationService(
      consultationRepo,
      specialistRepo,
      userRepo,
      notificationService,
      notificationPreferenceRepo
    )
    val userId         = java.util.UUID.randomUUID()
    val specialistId   = java.util.UUID.randomUUID()
    val categoryId     = java.util.UUID.randomUUID()
    val consultationId = java.util.UUID.randomUUID()
    val req            = CreateConsultationRequest(userId, specialistId, categoryId, "desc", Instant.now())
    val user           = createMockUser(userId, "user@example.com")
    val specialist     = createMockSpecialist(specialistId, "specialist@example.com", categoryId)
    val createdConsultation = Consultation(
      id = consultationId,
      userId = userId,
      specialistId = specialistId,
      categoryId = categoryId,
      description = "desc",
      status = ConsultationStatus.Requested,
      scheduledAt = req.scheduledAt,
      duration = None,
      price = BigDecimal("50.00"),
      rating = None,
      review = None,
      createdAt = Instant.now(),
      updatedAt = Instant.now()
    )

    userRepo.findById.expects(req.userId).returning(IO.pure(Some(user)))
    specialistRepo.findById.expects(req.specialistId).returning(IO.pure(Some(specialist)))
    consultationRepo.create
      .expects(req, BigDecimal("50.00"))
      .returning(IO.pure(createdConsultation))
    notificationService.sendEmail
      .expects(user.email, "Consultation Request Created", *)
      .returning(IO.unit)

    val result = service.createConsultation(req).unsafeRunSync()

    result.isRight shouldBe true
    result.toOption.get.id shouldBe consultationId
  }

  "getConsultation" should "return consultation if found" in {
    val consultationRepo           = mock[ConsultationRepository]
    val specialistRepo             = mock[SpecialistRepository]
    val userRepo                   = mock[UserRepository]
    val notificationService        = mock[NotificationService]
    val notificationPreferenceRepo = mock[NotificationPreferenceRepository]
    val service = new ConsultationService(
      consultationRepo,
      specialistRepo,
      userRepo,
      notificationService,
      notificationPreferenceRepo
    )
    val consultationId = java.util.UUID.randomUUID()
    val consultation = Consultation(
      id = consultationId,
      userId = java.util.UUID.randomUUID(),
      specialistId = java.util.UUID.randomUUID(),
      categoryId = java.util.UUID.randomUUID(),
      description = "desc",
      status = ConsultationStatus.Requested,
      scheduledAt = Instant.now(),
      duration = None,
      price = BigDecimal("50.00"),
      rating = None,
      review = None,
      createdAt = Instant.now(),
      updatedAt = Instant.now()
    )

    consultationRepo.findById.expects(consultationId).returning(IO.pure(Some(consultation)))
    val result = service.getConsultation(consultationId).unsafeRunSync()

    result.shouldBe(Right(consultation))
  }

  it should "return error if consultation not found" in {
    val consultationRepo           = mock[ConsultationRepository]
    val specialistRepo             = mock[SpecialistRepository]
    val userRepo                   = mock[UserRepository]
    val notificationService        = mock[NotificationService]
    val notificationPreferenceRepo = mock[NotificationPreferenceRepository]
    val service = new ConsultationService(
      consultationRepo,
      specialistRepo,
      userRepo,
      notificationService,
      notificationPreferenceRepo
    )
    val consultationId = java.util.UUID.randomUUID()

    consultationRepo.findById.expects(consultationId).returning(IO.pure(None))
    val result = service.getConsultation(consultationId).unsafeRunSync()

    result.shouldBe(Left(DomainError.ConsultationNotFound(consultationId)))
  }

  "approveConsultation" should "return error if consultation not found when approving" in {
    val consultationRepo           = mock[ConsultationRepository]
    val specialistRepo             = mock[SpecialistRepository]
    val userRepo                   = mock[UserRepository]
    val notificationService        = mock[NotificationService]
    val notificationPreferenceRepo = mock[NotificationPreferenceRepository]
    val service = new ConsultationService(
      consultationRepo,
      specialistRepo,
      userRepo,
      notificationService,
      notificationPreferenceRepo
    )
    val consultationId = java.util.UUID.randomUUID()

    consultationRepo.findById.expects(consultationId).returning(IO.pure(None))
    val result = service.approveConsultation(consultationId, 60).unsafeRunSync()

    result.shouldBe(Left(DomainError.ConsultationNotFound(consultationId)))
  }

  "updateConsultationStatus" should "send notifications only for notification-triggering status transitions" in {
    val consultationRepo           = mock[ConsultationRepository]
    val specialistRepo             = mock[SpecialistRepository]
    val userRepo                   = mock[UserRepository]
    val notificationService        = mock[NotificationService]
    val notificationPreferenceRepo = mock[NotificationPreferenceRepository]
    val service = new ConsultationService(
      consultationRepo,
      specialistRepo,
      userRepo,
      notificationService,
      notificationPreferenceRepo
    )
    val consultationId = java.util.UUID.randomUUID()
    val consultation = Consultation(
      id = consultationId,
      userId = java.util.UUID.randomUUID(),
      specialistId = java.util.UUID.randomUUID(),
      categoryId = java.util.UUID.randomUUID(),
      description = "desc",
      status = ConsultationStatus.Scheduled,
      scheduledAt = Instant.now(),
      duration = Some(60),
      price = BigDecimal("50.00"),
      rating = None,
      review = None,
      createdAt = Instant.now(),
      updatedAt = Instant.now()
    )

    consultationRepo.findById.expects(consultationId).returning(IO.pure(Some(consultation)))
    consultationRepo.updateStatus.expects(consultationId, ConsultationStatus.Completed).returning(IO.unit)
    // Should try to fetch user/specialist only for notification-triggering transitions
    userRepo.findById.expects(consultation.userId).returning(IO.pure(None))
    specialistRepo.findById.expects(consultation.specialistId).returning(IO.pure(None))

    val result = service.updateConsultationStatus(consultationId, ConsultationStatus.Completed).unsafeRunSync()

    // Should succeed - notification triggers are correctly identified
    result.shouldBe(Right(()))
  }

  it should "skip user/specialist fetches for non-triggering status transitions" in {
    val consultationRepo           = mock[ConsultationRepository]
    val specialistRepo             = mock[SpecialistRepository]
    val userRepo                   = mock[UserRepository]
    val notificationService        = mock[NotificationService]
    val notificationPreferenceRepo = mock[NotificationPreferenceRepository]
    val service = new ConsultationService(
      consultationRepo,
      specialistRepo,
      userRepo,
      notificationService,
      notificationPreferenceRepo
    )
    val consultationId = java.util.UUID.randomUUID()
    val consultation = Consultation(
      id = consultationId,
      userId = java.util.UUID.randomUUID(),
      specialistId = java.util.UUID.randomUUID(),
      categoryId = java.util.UUID.randomUUID(),
      description = "desc",
      status = ConsultationStatus.Completed,
      scheduledAt = Instant.now(),
      duration = Some(60),
      price = BigDecimal("50.00"),
      rating = None,
      review = None,
      createdAt = Instant.now(),
      updatedAt = Instant.now()
    )

    consultationRepo.findById.expects(consultationId).returning(IO.pure(Some(consultation)))
    consultationRepo.updateStatus.expects(consultationId, ConsultationStatus.Completed).returning(IO.unit)
    // Should NOT fetch user/specialist for non-triggering transitions (Completed -> Completed is invalid but serves as non-triggering)
    // Explicitly set no expectations for userRepo/specialistRepo to verify they are not called

    val result = service.updateConsultationStatus(consultationId, ConsultationStatus.Completed).unsafeRunSync()

    // Should succeed even without fetching user/specialist
    result.shouldBe(Right(()))
  }

}
