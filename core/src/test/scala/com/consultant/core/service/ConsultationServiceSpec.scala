package com.consultant.core.service

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalamock.scalatest.MockFactory
import cats.effect.IO
import com.consultant.core.domain._
import com.consultant.core.ports._
import cats.effect.unsafe.implicits.global

class ConsultationServiceSpec extends AnyFlatSpec with Matchers with MockFactory {
  "createConsultation" should "return error if specialist not found" in {
    val consultationRepo    = mock[ConsultationRepository]
    val specialistRepo      = mock[SpecialistRepository]
    val userRepo            = mock[UserRepository]
    val notificationService = mock[NotificationService]
    val service             = new ConsultationService(consultationRepo, specialistRepo, userRepo, notificationService)
    val userId              = java.util.UUID.randomUUID()
    val specialistId        = java.util.UUID.randomUUID()
    val categoryId          = java.util.UUID.randomUUID()
    val req                 = CreateConsultationRequest(userId, specialistId, categoryId, "desc", None, None, false)
    userRepo.findById
      .expects(req.userId)
      .returning(
        IO.pure(
          Some(
            com.consultant.core.domain.User(
              id = userId,
              login = "userlogin",
              email = "user@example.com",
              name = "User",
              phone = None,
              role = com.consultant.core.domain.security.UserRole.Client,
              countryId = Some(java.util.UUID.randomUUID()),
              languages = Set(java.util.UUID.randomUUID()),
              createdAt = java.time.Instant.now(),
              updatedAt = java.time.Instant.now()
            )
          )
        )
      )
    specialistRepo.findById.expects(req.specialistId).returning(IO.pure(None))
    val result = service.createConsultation(req).unsafeRunSync()
    result.shouldBe(Left(DomainError.SpecialistNotFound(req.specialistId)))
  }

  // Add more tests for successful creation, user not found, notification, etc.
}
