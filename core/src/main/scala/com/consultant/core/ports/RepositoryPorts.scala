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
package com.consultant.core.ports

import cats.effect.IO
import com.consultant.core.domain.*
import java.util.UUID

// Repository ports (abstractions for data access)
// Implementations will be in 'data' module for PostgreSQL
// Can be easily replaced with AWS DynamoDB implementations

trait UserRepository:
  def create(request: CreateUserRequest): IO[User]
  def findById(id: UserId): IO[Option[User]]
  def findByEmail(email: String): IO[Option[User]]
  def findByLogin(login: String): IO[Option[User]]
  def update(user: User): IO[User]
  def delete(id: UserId): IO[Unit]
  def list(offset: Int, limit: Int): IO[List[User]]
  def login(login: String, password: String): IO[Option[User]]
  def countAdmins(): IO[Int]

trait SpecialistRepository:
  def create(request: CreateSpecialistRequest): IO[Specialist]
  def findById(id: SpecialistId): IO[Option[Specialist]]
  def findByEmail(email: String): IO[Option[Specialist]]
  def search(criteria: SpecialistSearchCriteria, offset: Int, limit: Int): IO[List[Specialist]]
  def update(specialist: Specialist): IO[Specialist]
  def delete(id: SpecialistId): IO[Unit]
  def updateCategoryRating(
    specialistId: SpecialistId,
    categoryId: CategoryId,
    rating: BigDecimal,
    consultationCount: Int
  ): IO[Unit]

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
  def findByUserCount(userId: UserId): IO[Long]
  def findByUserWithCount(userId: UserId, offset: Int, limit: Int): IO[(List[Consultation], Long)]
  def findBySpecialist(specialistId: SpecialistId, offset: Int, limit: Int): IO[List[Consultation]]
  def findBySpecialistCount(specialistId: SpecialistId): IO[Long]
  def findBySpecialistWithCount(specialistId: SpecialistId, offset: Int, limit: Int): IO[(List[Consultation], Long)]
  def update(consultation: Consultation): IO[Consultation]
  def updateStatus(id: ConsultationId, status: ConsultationStatus): IO[Unit]
  def addReview(id: ConsultationId, rating: Int, review: String): IO[Unit]
trait ConnectionRepository:
  def create(specialistId: SpecialistId, request: CreateConnectionRequest): IO[SpecialistConnection]
  def findById(id: UUID): IO[Option[SpecialistConnection]]
  def findBySpecialist(specialistId: SpecialistId): IO[List[SpecialistConnection]]
  def findBySpecialistAndType(
    specialistId: SpecialistId,
    connectionTypeId: ConnectionTypeId
  ): IO[Option[SpecialistConnection]]
  def update(connection: SpecialistConnection): IO[SpecialistConnection]
  def delete(id: UUID): IO[Unit]
  def deleteBySpecialist(specialistId: SpecialistId): IO[Unit]

  // Client connections
  def createClientConnection(userId: UserId, request: CreateConnectionRequest): IO[ClientConnection]
  def findClientConnectionById(id: UUID): IO[Option[ClientConnection]]
  def findClientConnectionsByUser(userId: UserId): IO[List[ClientConnection]]
  def findClientConnectionByUserAndType(
    userId: UserId,
    connectionTypeId: ConnectionTypeId
  ): IO[Option[ClientConnection]]
  def updateClientConnection(connection: ClientConnection): IO[ClientConnection]
  def deleteClientConnection(id: UUID): IO[Unit]
  def deleteClientConnectionsByUser(userId: UserId): IO[Unit]

trait ConnectionTypeRepository:
  def create(request: CreateConnectionTypeRequest): IO[ConnectionType]
  def findById(id: ConnectionTypeId): IO[Option[ConnectionType]]
  def listAll(): IO[List[ConnectionType]]
  def findByName(name: String): IO[Option[ConnectionType]]
  def update(connectionType: ConnectionType): IO[ConnectionType]
  def delete(id: ConnectionTypeId): IO[Unit]
trait AvailabilityRepository:
  def create(specialistId: SpecialistId, request: CreateAvailabilityRequest): IO[SpecialistAvailability]
  def findById(id: UUID): IO[Option[SpecialistAvailability]]
  def findBySpecialist(specialistId: SpecialistId): IO[List[SpecialistAvailability]]
  def findBySpecialistAndDay(specialistId: SpecialistId, dayOfWeek: Int): IO[List[SpecialistAvailability]]
  def update(availability: SpecialistAvailability): IO[SpecialistAvailability]
  def delete(id: UUID): IO[Unit]
  def deleteBySpecialist(specialistId: SpecialistId): IO[Unit]

trait NotificationPreferenceRepository:
  // Create default preferences for a user
  def createDefaults(userId: UserId): IO[List[NotificationPreference]]
  // Get preference for a specific notification type
  def findByUserAndType(
    userId: UserId,
    notificationType: NotificationType
  ): IO[Option[NotificationPreference]]
  // Get all preferences for a user
  def findByUser(userId: UserId): IO[List[NotificationPreference]]
  // Update a preference
  def update(preference: NotificationPreference): IO[NotificationPreference]
  // Delete all preferences for a user(when user is deleted)
  def deleteByUser(userId: UserId): IO[Unit]
