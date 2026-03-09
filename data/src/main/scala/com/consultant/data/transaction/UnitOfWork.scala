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
package com.consultant.data.transaction

import cats.effect.IO
import cats.syntax.all.*
import doobie.ConnectionIO
import doobie.implicits.*
import doobie.util.transactor.Transactor

/**
 * UnitOfWork provides transaction management for database operations.
 *
 * This trait abstracts transaction boundaries, allowing multiple database operations to be executed atomically within a
 * single transaction. It's essential for maintaining data consistency across multiple tables.
 *
 * Example usage: {{ val result: IO[Either[DomainError, (User, Specialist)]] = unitOfWork.transaction { for user <-
 * userRepo.create(userRequest).to[ConnectionIO] specialist <- specialistRepo.create(specialistRequest).to[ConnectionIO]
 * yield (user, specialist) } }}
 */
trait UnitOfWork:

  /**
   * Executes a single ConnectionIO operation within a transaction.
   *
   * @param operation
   *   The database operation to execute
   * @tparam A
   *   The result type
   * @return
   *   The result wrapped in IO
   */
  def transaction[A](operation: ConnectionIO[A]): IO[A]

  /**
   * Executes multiple ConnectionIO operations within a single transaction.
   *
   * All operations succeed or all fail together (atomic).
   *
   * @param operations
   *   List of database operations to execute
   * @tparam A
   *   The result type
   * @return
   *   List of results wrapped in IO
   */
  def transact[A](operations: List[ConnectionIO[A]]): IO[List[A]]

  /**
   * Executes a sequence of ConnectionIO operations within a transaction with error handling.
   *
   * @param operation
   *   The database operation that may fail with a domain error
   * @tparam A
   *   The success result type
   * @tparam E
   *   The error type
   * @return
   *   Either the error or the result wrapped in IO
   */
  def transactionEither[E, A](operation: ConnectionIO[Either[E, A]]): IO[Either[E, A]]

end UnitOfWork

/**
 * Implementation of UnitOfWork using Doobie's Transactor.
 *
 * @param xa
 *   The Doobie transactor for executing transactions
 */
class DoobieUnitOfWork(xa: Transactor[IO]) extends UnitOfWork:

  override def transaction[A](operation: ConnectionIO[A]): IO[A] =
    operation.transact(xa)

  override def transact[A](operations: List[ConnectionIO[A]]): IO[List[A]] =
    operations.sequence.transact(xa)

  override def transactionEither[E, A](operation: ConnectionIO[Either[E, A]]): IO[Either[E, A]] =
    operation.transact(xa)

end DoobieUnitOfWork
