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
