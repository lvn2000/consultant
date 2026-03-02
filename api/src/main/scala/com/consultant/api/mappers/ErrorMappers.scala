package com.consultant.api.mappers

import com.consultant.core.domain.DomainError
import com.consultant.api.dto.ErrorResponse

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

      // Database errors
      case DomainError.DatabaseError(msg) =>
        System.err.println(s"Database error: $msg")
        ErrorResponse("DATABASE_ERROR", "A database error occurred. Please try again later.")
      case DomainError.ConstraintViolation(constraint, msg) =>
        System.err.println(s"Constraint violation [$constraint]: $msg")
        ErrorResponse("CONFLICT", s"Constraint violation: $msg")

      // System errors
      case DomainError.UnexpectedError(msg) =>
        System.err.println(s"Unexpected error: $msg")
        ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred. Please try again later.")
      case DomainError.ExternalServiceError(service, msg) =>
        System.err.println(s"External service error [$service]: $msg")
        ErrorResponse("SERVICE_UNAVAILABLE", s"External service error: $service")

end ErrorMappers
