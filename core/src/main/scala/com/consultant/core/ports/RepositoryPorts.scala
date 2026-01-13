package com.consultant.core.ports

import cats.effect.IO
import com.consultant.core.domain.*

// Repository ports (abstractions for data access)
// Implementations will be in 'data' module for PostgreSQL
// Can be easily replaced with AWS DynamoDB implementations

trait UserRepository:
  def create(request: CreateUserRequest): IO[User]
  def findById(id: UserId): IO[Option[User]]
  def findByEmail(email: String): IO[Option[User]]
  def update(user: User): IO[User]
  def delete(id: UserId): IO[Unit]
  def list(offset: Int, limit: Int): IO[List[User]]

trait SpecialistRepository:
  def create(request: CreateSpecialistRequest): IO[Specialist]
  def findById(id: SpecialistId): IO[Option[Specialist]]
  def findByEmail(email: String): IO[Option[Specialist]]
  def search(criteria: SpecialistSearchCriteria, offset: Int, limit: Int): IO[List[Specialist]]
  def update(specialist: Specialist): IO[Specialist]
  def delete(id: SpecialistId): IO[Unit]
  def updateRating(id: SpecialistId, rating: BigDecimal, consultationCount: Int): IO[Unit]

trait CategoryRepository:
  def create(request: CreateCategoryRequest): IO[Category]
  def findById(id: CategoryId): IO[Option[Category]]
  def findByName(name: String): IO[Option[Category]]
  def listAll(): IO[List[Category]]
  def update(category: Category): IO[Category]
  def delete(id: CategoryId): IO[Unit]

trait ConsultationRepository:
  def create(request: CreateConsultationRequest, price: BigDecimal): IO[Consultation]
  def findById(id: ConsultationId): IO[Option[Consultation]]
  def findByUser(userId: UserId, offset: Int, limit: Int): IO[List[Consultation]]
  def findBySpecialist(specialistId: SpecialistId, offset: Int, limit: Int): IO[List[Consultation]]
  def update(consultation: Consultation): IO[Consultation]
  def updateStatus(id: ConsultationId, status: ConsultationStatus): IO[Unit]
  def addReview(id: ConsultationId, rating: Int, review: String): IO[Unit]
