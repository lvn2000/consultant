package com.consultant.data.repository

import cats.effect.IO
import doobie.ConnectionIO

/**
 * Trait for repositories that support transactional operations.
 *
 * This trait provides methods that return ConnectionIO instead of IO, allowing multiple operations to be composed into
 * a single transaction.
 *
 * @tparam A
 *   The entity type
 * @tparam ID
 *   The identifier type
 * @tparam CreateRequest
 *   The create request type
 */
trait TransactionalRepository[A, ID, CreateRequest]:

  /**
   * Create an entity within a transaction.
   *
   * @param request
   *   The creation request
   * @return
   *   ConnectionIO of the created entity
   */
  def createTransactional(request: CreateRequest): ConnectionIO[A]

  /**
   * Find an entity by ID within a transaction.
   *
   * @param id
   *   The entity ID
   * @return
   *   ConnectionIO of optional entity
   */
  def findByIdTransactional(id: ID): ConnectionIO[Option[A]]

  /**
   * Update an entity within a transaction.
   *
   * @param entity
   *   The entity to update
   * @return
   *   ConnectionIO of the updated entity
   */
  def updateTransactional(entity: A): ConnectionIO[A]

  /**
   * Delete an entity within a transaction.
   *
   * @param id
   *   The entity ID
   * @return
   *   ConnectionIO of number of rows affected
   */
  def deleteTransactional(id: ID): ConnectionIO[Int]

end TransactionalRepository

/** Helper object for transactional operations. */
object TransactionalRepository:

  /**
   * Lifts a pure value into ConnectionIO.
   *
   * @param a
   *   The pure value
   * @tparam A
   *   The value type
   * @return
   *   ConnectionIO containing the value
   */
  def pure[A](a: A): ConnectionIO[A] =
    doobie.free.connection.pure(a)

  /**
   * Creates a failed ConnectionIO.
   *
   * @param e
   *   The throwable
   * @tparam A
   *   The value type
   * @return
   *   Failed ConnectionIO
   */
  def raiseError[A](e: Throwable): ConnectionIO[A] =
    doobie.free.connection.raiseError(e)

end TransactionalRepository
