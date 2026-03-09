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
    result.shouldBe(Left(DomainError.ConnectionTypeNotFound(connectionTypeId)))
  }

  // Add more tests for duplicate connection, successful creation, etc.
}
