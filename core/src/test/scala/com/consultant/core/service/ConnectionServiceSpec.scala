package com.consultant.core.service

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalamock.scalatest.MockFactory
import cats.effect.IO
import com.consultant.core.domain._
import com.consultant.core.ports._
import java.util.UUID
import cats.effect.unsafe.implicits.global

class ConnectionServiceSpec extends AnyFlatSpec with Matchers with MockFactory {
  "addConnection" should "return error if connection type not found" in {
    val connectionRepo     = mock[ConnectionRepository]
    val connectionTypeRepo = mock[ConnectionTypeRepository]
    val specialistRepo     = mock[SpecialistRepository]
    val service            = new ConnectionService(connectionRepo, connectionTypeRepo, specialistRepo)
    val specialistId       = java.util.UUID.randomUUID()
    val connectionTypeId   = java.util.UUID.randomUUID()
    val req                = CreateConnectionRequest(connectionTypeId, "value")
    connectionTypeRepo.findById.expects(req.connectionTypeId).returning(IO.pure(None))
    val result = service.addConnection(specialistId, req).unsafeRunSync()
    result.shouldBe(Left(DomainError.ValidationError("Connection type not found")))
  }

  // Add more tests for duplicate connection, successful creation, etc.
}
