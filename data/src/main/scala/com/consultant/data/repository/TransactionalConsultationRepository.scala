package com.consultant.data.repository

import cats.effect.IO
import doobie.ConnectionIO
import com.consultant.core.domain.{ Consultation, ConsultationId, CreateConsultationRequest }
import java.util.UUID

/** Trait for ConsultationRepository that supports transactional operations. */
trait TransactionalConsultationRepository:

  /** Create a consultation within a transaction. */
  def createTransactional(request: CreateConsultationRequest, price: BigDecimal): ConnectionIO[Consultation]

  /** Find a consultation by ID within a transaction. */
  def findByIdTransactional(id: ConsultationId): ConnectionIO[Option[Consultation]]

  /** Update a consultation within a transaction. */
  def updateTransactional(consultation: Consultation): ConnectionIO[Consultation]

  /** Update consultation status within a transaction. */
  def updateStatusTransactional(id: ConsultationId, status: String): ConnectionIO[Int]

  /** Add review within a transaction. */
  def addReviewTransactional(id: ConsultationId, rating: Int, review: String): ConnectionIO[Int]

end TransactionalConsultationRepository
