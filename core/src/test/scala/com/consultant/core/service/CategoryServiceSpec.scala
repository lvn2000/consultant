package com.consultant.core.service

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalamock.scalatest.MockFactory
import cats.effect.IO
import com.consultant.core.domain._
import com.consultant.core.ports._
import java.util.UUID
import cats.effect.unsafe.implicits.global

class CategoryServiceSpec extends AnyFlatSpec with Matchers with MockFactory {
  "createCategory" should "return error if category already exists" in {
    val categoryRepo = mock[CategoryRepository]
    val service      = new CategoryService(categoryRepo)
    val req          = CreateCategoryRequest("Health", "desc", None)
    categoryRepo.findByName
      .expects(req.name)
      .returning(IO.pure(Some(Category(UUID.randomUUID(), req.name, req.description, None))))
    val result = service.createCategory(req).unsafeRunSync()
    result.shouldBe(Left(DomainError.ValidationError(s"Category '${req.name}' already exists")))
  }

  // Add more tests for successful creation, not found, update, etc.
}
