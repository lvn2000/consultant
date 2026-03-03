package com.consultant.api.mappers

import com.consultant.core.domain.DomainError
import com.consultant.api.dto.ErrorResponse
import cats.effect.IO
import org.typelevel.log4cats.SelfAwareStructuredLogger

/** Mappers for DomainError <-> ErrorResponse conversions. */
object ErrorMappers:

  /** Maps DomainError to ErrorResponse for API responses. */
  def toErrorResponse(error: DomainError): ErrorResponse =
    error match
      // Not found errors
      case DomainError.UserNotFound(id) =>
        ErrorResponse("NOT_FOUND", s"User not found: $id")
      case DomainError.SpecialistNotFound(id) =>
        ErrorResponse("NOT_FOUND", s"Specialist not found: $id")
      case DomainError.CategoryNotFound(id) =>
        ErrorResponse("NOT_FOUND", s"Category not found: $id")
      case DomainError.ConsultationNotFound(id) =>
        ErrorResponse("NOT_FOUND", s"Consultation not found: $id")
      case DomainError.ConnectionTypeNotFound(id) =>
        ErrorResponse("NOT_FOUND", s"Connection type not found: $id")
      case DomainError.ConnectionNotFound(id) =>
        ErrorResponse("NOT_FOUND", s"Connection not found: $id")

      // Conflict errors
      case DomainError.EmailAlreadyExists(email) =>
        ErrorResponse("CONFLICT", s"Email already exists: $email")
      case DomainError.LoginAlreadyExists(login) =>
        ErrorResponse("CONFLICT", s"Login already exists: $login")
      case DomainError.DuplicateCategoryRate(catId) =>
        ErrorResponse(
          "CONFLICT",
          s"This category has already been added for this specialist (Category: $catId). Each specialist can only have one rate per category."
        )
      case DomainError.DuplicateEntry(msg) =>
        ErrorResponse("CONFLICT", msg)
      case DomainError.ReferencedRecordNotFound(msg) =>
        ErrorResponse("CONFLICT", s"Referenced record not found: $msg")

      // Validation errors
      case DomainError.InvalidEmail(email) =>
        ErrorResponse("VALIDATION_ERROR", s"Invalid email: $email")
      case DomainError.InvalidPhoneNumber(phone) =>
        ErrorResponse("VALIDATION_ERROR", s"Invalid phone: $phone")
      case DomainError.InvalidPrice(price) =>
        ErrorResponse("VALIDATION_ERROR", s"Invalid price: $price")
      case DomainError.ValidationError(msg) =>
        ErrorResponse("VALIDATION_ERROR", msg)
      case DomainError.InvalidCredentials =>
        ErrorResponse("UNAUTHORIZED", "Invalid credentials")

      // State errors
      case DomainError.SpecialistNotAvailable(id) =>
        ErrorResponse("UNAVAILABLE", s"Specialist not available: $id")
      case DomainError.Forbidden(msg) =>
        ErrorResponse("FORBIDDEN", msg)
      case DomainError.Unauthorized(msg) =>
        ErrorResponse("UNAUTHORIZED", msg)

      // Database errors - return error response without side effects
      case DomainError.DatabaseError(msg) =>
        ErrorResponse("DATABASE_ERROR", "A database error occurred. Please try again later.")
      case DomainError.ConstraintViolation(constraint, msg) =>
        ErrorResponse("CONFLICT", s"Constraint violation: $msg")

      // System errors - return error response without side effects
      case DomainError.UnexpectedError(msg) =>
        ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred. Please try again later.")
      case DomainError.ExternalServiceError(service, msg) =>
        ErrorResponse("SERVICE_UNAVAILABLE", s"External service error: $service")

  /** Maps DomainError to ErrorResponse for API responses with logging. */
  def toErrorResponseWithLogging(error: DomainError)(using logger: SelfAwareStructuredLogger[IO]): IO[ErrorResponse] =
    error match
      // Not found errors - no logging needed for expected not-found cases
      case DomainError.UserNotFound(id) =>
        IO.pure(ErrorResponse("NOT_FOUND", s"User not found: $id"))
      case DomainError.SpecialistNotFound(id) =>
        IO.pure(ErrorResponse("NOT_FOUND", s"Specialist not found: $id"))
      case DomainError.CategoryNotFound(id) =>
        IO.pure(ErrorResponse("NOT_FOUND", s"Category not found: $id"))
      case DomainError.ConsultationNotFound(id) =>
        IO.pure(ErrorResponse("NOT_FOUND", s"Consultation not found: $id"))
      case DomainError.ConnectionTypeNotFound(id) =>
        IO.pure(ErrorResponse("NOT_FOUND", s"Connection type not found: $id"))
      case DomainError.ConnectionNotFound(id) =>
        IO.pure(ErrorResponse("NOT_FOUND", s"Connection not found: $id"))

      // Conflict errors - no logging needed for expected conflicts
      case DomainError.EmailAlreadyExists(email) =>
        IO.pure(ErrorResponse("CONFLICT", s"Email already exists: $email"))
      case DomainError.LoginAlreadyExists(login) =>
        IO.pure(ErrorResponse("CONFLICT", s"Login already exists: $login"))
      case DomainError.DuplicateCategoryRate(catId) =>
        IO.pure(
          ErrorResponse(
            "CONFLICT",
            s"This category has already been added for this specialist (Category: $catId). Each specialist can only have one rate per category."
          )
        )
      case DomainError.DuplicateEntry(msg) =>
        IO.pure(ErrorResponse("CONFLICT", msg))
      case DomainError.ReferencedRecordNotFound(msg) =>
        IO.pure(ErrorResponse("CONFLICT", s"Referenced record not found: $msg"))

      // Validation errors - no logging needed for expected validation errors
      case DomainError.InvalidEmail(email) =>
        IO.pure(ErrorResponse("VALIDATION_ERROR", s"Invalid email: $email"))
      case DomainError.InvalidPhoneNumber(phone) =>
        IO.pure(ErrorResponse("VALIDATION_ERROR", s"Invalid phone: $phone"))
      case DomainError.InvalidPrice(price) =>
        IO.pure(ErrorResponse("VALIDATION_ERROR", s"Invalid price: $price"))
      case DomainError.ValidationError(msg) =>
        IO.pure(ErrorResponse("VALIDATION_ERROR", msg))
      case DomainError.InvalidCredentials =>
        IO.pure(ErrorResponse("UNAUTHORIZED", "Invalid credentials"))

      // State errors - no logging needed for expected state errors
      case DomainError.SpecialistNotAvailable(id) =>
        IO.pure(ErrorResponse("UNAVAILABLE", s"Specialist not available: $id"))
      case DomainError.Forbidden(msg) =>
        IO.pure(ErrorResponse("FORBIDDEN", msg))
      case DomainError.Unauthorized(msg) =>
        IO.pure(ErrorResponse("UNAUTHORIZED", msg))

      // Database errors - log the actual error details
      case DomainError.DatabaseError(msg) =>
        logger
          .error(s"Database error: $msg")
          .as(ErrorResponse("DATABASE_ERROR", "A database error occurred. Please try again later."))
      case DomainError.ConstraintViolation(constraint, msg) =>
        logger
          .error(s"Constraint violation [$constraint]: $msg")
          .as(ErrorResponse("CONFLICT", s"Constraint violation: $msg"))

      // System errors - log the actual error details
      case DomainError.UnexpectedError(msg) =>
        logger
          .error(s"Unexpected error: $msg")
          .as(ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred. Please try again later."))
      case DomainError.ExternalServiceError(service, msg) =>
        logger
          .error(s"External service error [$service]: $msg")
          .as(ErrorResponse("SERVICE_UNAVAILABLE", s"External service error: $service"))

end ErrorMappers
