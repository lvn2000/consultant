package com.consultant.core.error

import com.consultant.core.domain.DomainError

/**
 * Utility for parsing PostgreSQL exceptions into structured domain errors.
 *
 * This centralized component handles the reflection-based logic for extracting SQL state codes from PostgreSQL
 * exceptions, ensuring consistent error handling across all service layers.
 */
object PostgresErrorParser:

  /**
   * Parses a throwable into a structured domain error.
   *
   * @param error
   *   the throwable to parse
   * @return
   *   a DomainError representing the structured error
   */
  def parseError(error: Throwable): DomainError =
    error match
      case ex if isPostgresException(ex) =>
        val sqlState = getSqlState(ex)
        val message  = ex.getMessage

        sqlState match
          case Some("23505") => // unique_violation
            DomainError.DuplicateEntry(s"Database entry already exists: $message")
          case Some("23503") => // foreign_key_violation
            DomainError.ReferencedRecordNotFound(s"Referenced record not found: $message")
          case Some("23514") => // check_violation
            DomainError.ValidationError(s"Check constraint violation: $message")
          case Some("23502") => // not_null_violation
            DomainError.ValidationError(s"Not-null constraint violation: $message")
          case Some(code) =>
            DomainError.DatabaseError(s"Database error [$code]: $message")
          case None =>
            DomainError.DatabaseError(s"Database error: $message")

      case ex =>
        DomainError.UnexpectedError(ex.getMessage)

  /**
   * Checks if an exception is a PostgreSQL PSQLException.
   *
   * @param ex
   *   the exception to check
   * @return
   *   true if the exception is a PostgreSQL exception
   */
  private def isPostgresException(ex: Throwable): Boolean =
    ex.getClass.getName == "org.postgresql.util.PSQLException"

  /**
   * Gets the SQL state from a PostgreSQL exception using reflection.
   *
   * @param ex
   *   the PostgreSQL exception
   * @return
   *   the SQL state code, or None if unavailable
   */
  private def getSqlState(ex: Throwable): Option[String] =
    try
      val method = ex.getClass.getMethod("getSQLState")
      Option(method.invoke(ex).asInstanceOf[String])
    catch case _: Exception => None
