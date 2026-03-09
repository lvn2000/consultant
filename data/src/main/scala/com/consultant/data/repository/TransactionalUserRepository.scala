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
import com.consultant.core.domain.{ CreateUserRequest, User, UserId }

/**
 * Trait for UserRepository that supports transactional operations.
 *
 * This trait provides methods that return ConnectionIO instead of IO, allowing user operations to participate in
 * multi-repository transactions.
 */
trait TransactionalUserRepository:

  /**
   * Create a user within a transaction.
   *
   * @param request
   *   The creation request
   * @return
   *   ConnectionIO of the created user
   */
  def createTransactional(request: CreateUserRequest): ConnectionIO[User]

  /**
   * Find a user by ID within a transaction.
   *
   * @param id
   *   The user ID
   * @return
   *   ConnectionIO of optional user
   */
  def findByIdTransactional(id: UserId): ConnectionIO[Option[User]]

  /**
   * Update a user within a transaction.
   *
   * @param user
   *   The user to update
   * @return
   *   ConnectionIO of the updated user
   */
  def updateTransactional(user: User): ConnectionIO[User]

  /**
   * Delete a user within a transaction.
   *
   * @param id
   *   The user ID
   * @return
   *   ConnectionIO of number of rows affected
   */
  def deleteTransactional(id: UserId): ConnectionIO[Int]

end TransactionalUserRepository
