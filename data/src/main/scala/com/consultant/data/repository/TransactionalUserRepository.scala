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
