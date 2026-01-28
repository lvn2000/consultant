package com.consultant.core.service

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalamock.scalatest.MockFactory
import cats.effect.IO
import com.consultant.core.domain._
import com.consultant.core.ports._
import java.util.UUID
import java.time.Instant
import com.consultant.core.domain.security.UserRole
import cats.effect.unsafe.implicits.global

class UserServiceSpec extends AnyFlatSpec with Matchers with MockFactory {
  "createUser" should "return error if email already exists" in {
    val userRepo    = mock[UserRepository]
    val sessionRepo = mock[SessionRepository]
    val service     = new UserService(userRepo, sessionRepo)
    val userId      = UUID.randomUUID()
    val now         = Instant.now()
    val user = User(
      id = userId,
      login = "testlogin",
      email = "test@example.com",
      name = "Test",
      phone = Some("1234567890"),
      role = UserRole.Client,
      countryId = Some(UUID.randomUUID()),
      languages = Set(UUID.randomUUID()),
      createdAt = now,
      updatedAt = now
    )
    userRepo.findByEmail.expects("test@example.com").returning(IO.pure(Some(user)))
    val req = CreateUserRequest(
      login = "testlogin",
      email = "test@example.com",
      name = "Test",
      phone = Some("1234567890"),
      role = UserRole.Client,
      countryId = Some(UUID.randomUUID()),
      languages = Set(UUID.randomUUID())
    )
    val result = service.createUser(req).unsafeRunSync()
    result.shouldBe(Left(DomainError.EmailAlreadyExists("test@example.com")))
  }

  "getUser" should "return user if found" in {
    val userRepo    = mock[UserRepository]
    val sessionRepo = mock[SessionRepository]
    val service     = new UserService(userRepo, sessionRepo)
    val userId      = UUID.randomUUID()
    val now         = Instant.now()
    val user = User(
      id = userId,
      login = "userlogin",
      email = "user@example.com",
      name = "User",
      phone = Some("9876543210"),
      role = UserRole.Client,
      countryId = Some(UUID.randomUUID()),
      languages = Set(UUID.randomUUID()),
      createdAt = now,
      updatedAt = now
    )
    userRepo.findById.expects(userId).returning(IO.pure(Some(user)))
    val result = service.getUser(userId).unsafeRunSync()
    result.shouldBe(Right(user))
  }

  // Add more tests for updateUser, listUsers, login, etc.
}
