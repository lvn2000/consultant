package com.consultant.core.service

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalamock.scalatest.MockFactory
import cats.effect.IO
import com.consultant.core.domain._
import com.consultant.core.ports._
import cats.effect.unsafe.implicits.global

class SpecialistServiceSpec extends AnyFlatSpec with Matchers with MockFactory {
  "createSpecialist" should "return error if email already exists" in {
    val specialistRepo = mock[SpecialistRepository]
    val categoryRepo   = mock[CategoryRepository]
    val service        = new SpecialistService(specialistRepo, categoryRepo)
    val req = CreateSpecialistRequest(
      email = "test@example.com",
      name = "Test",
      phone = "1234567890",
      bio = "Bio",
      categoryRates = List(),
      isAvailable = true
    )
    specialistRepo.findByEmail
      .expects(req.email)
      .returning(
        IO.pure(
          Some(
            Specialist(
              id = java.util.UUID.randomUUID(),
              email = req.email,
              name = req.name,
              phone = req.phone,
              bio = req.bio,
              categoryRates = List(),
              isAvailable = true,
              connections = List(),
              createdAt = java.time.Instant.now(),
              updatedAt = java.time.Instant.now()
            )
          )
        )
      )
    val result = service.createSpecialist(req).unsafeRunSync()
    result.shouldBe(Left(DomainError.EmailAlreadyExists(req.email)))
  }

  // Add more tests for validation errors, successful creation, etc.
}
