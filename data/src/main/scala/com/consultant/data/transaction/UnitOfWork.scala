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
