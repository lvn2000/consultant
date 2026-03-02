package com.consultant.data.repository

import cats.effect.IO
import doobie.ConnectionIO
import com.consultant.core.domain.{
  CategoryId,
  CreateSpecialistRequest,
  Specialist,
  SpecialistCategoryRate,
  SpecialistId
}
import java.util.UUID
import java.time.Instant

/** Trait for SpecialistRepository that supports transactional operations. */
trait TransactionalSpecialistRepository:

  /** Create a specialist within a transaction. */
  def createTransactional(request: CreateSpecialistRequest): ConnectionIO[Specialist]

  /**
   * Find a specialist by ID within a transaction. Returns a tuple of raw data that can be assembled into a Specialist.
   */
  def findByIdTransactional(id: SpecialistId): ConnectionIO[Option[
    (
      UUID,                        // id
      String,                      // email
      String,                      // name
      String,                      // phone
      String,                      // bio
      Boolean,                     // isAvailable
      Option[UUID],                // countryId
      Instant,                     // createdAt
      Instant,                     // updatedAt
      Set[UUID],                   // languages
      List[SpecialistCategoryRate] // categoryRates
    )
  ]]

  /** Update a specialist within a transaction. */
  def updateTransactional(specialist: Specialist): ConnectionIO[Specialist]

  /** Update category rating within a transaction. */
  def updateCategoryRatingTransactional(
    specialistId: SpecialistId,
    categoryId: CategoryId,
    rating: BigDecimal,
    consultationCount: Int
  ): ConnectionIO[Int]

  /** Delete a specialist within a transaction. */
  def deleteTransactional(id: SpecialistId): ConnectionIO[Int]

end TransactionalSpecialistRepository
