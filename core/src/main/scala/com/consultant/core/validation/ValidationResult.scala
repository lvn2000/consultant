package com.consultant.core.validation

import com.consultant.core.domain.DomainError

/**
 * Represents the result of a validation operation.
 *
 * This is a simplified validation result that accumulates errors.
 */
sealed trait ValidationResult:
  def isValid: Boolean
  def errors: List[DomainError]
  def ++(other: ValidationResult): ValidationResult

object ValidationResult:

  case object Valid extends ValidationResult:
    def isValid: Boolean                              = true
    def errors: List[DomainError]                     = Nil
    def ++(other: ValidationResult): ValidationResult = other

  case class Invalid(errors: List[DomainError]) extends ValidationResult:
    def isValid: Boolean = false
    def ++(other: ValidationResult): ValidationResult = other match
      case Valid              => this
      case Invalid(otherErrs) => Invalid(errors ++ otherErrs)

  def valid: ValidationResult                              = Valid
  def invalid(error: DomainError): ValidationResult        = Invalid(List(error))
  def invalid(errors: List[DomainError]): ValidationResult = Invalid(errors)

  private def getErrorMessage(error: DomainError): String = error match
    case DomainError.ValidationError(msg)                     => msg
    case DomainError.DuplicateEntry(msg)                      => s"Duplicate entry: $msg"
    case DomainError.ReferencedRecordNotFound(msg)            => s"Referenced record not found: $msg"
    case DomainError.Forbidden(msg)                           => s"Forbidden: $msg"
    case DomainError.Unauthorized(msg)                        => s"Unauthorized: $msg"
    case DomainError.DatabaseError(msg)                       => s"Database error: $msg"
    case DomainError.ConstraintViolation(constraintName, msg) => s"Constraint violation ($constraintName): $msg"
    case DomainError.UnexpectedError(msg)                     => s"Unexpected error: $msg"
    case DomainError.ExternalServiceError(service, msg)       => s"External service error ($service): $msg"
    case DomainError.InvalidEmail(email)                      => s"Invalid email: $email"
    case DomainError.InvalidPhoneNumber(phone)                => s"Invalid phone: $phone"
    case DomainError.InvalidPrice(price)                      => s"Invalid price: $price"
    case DomainError.EmailAlreadyExists(email)                => s"Email already exists: $email"
    case DomainError.LoginAlreadyExists(login)                => s"Login already exists: $login"
    case DomainError.DuplicateCategoryRate(categoryId)        => s"Duplicate category rate: $categoryId"
    case DomainError.UserNotFound(id)                         => s"User not found: $id"
    case DomainError.SpecialistNotFound(id)                   => s"Specialist not found: $id"
    case DomainError.CategoryNotFound(id)                     => s"Category not found: $id"
    case DomainError.ConsultationNotFound(id)                 => s"Consultation not found: $id"
    case DomainError.ConnectionTypeNotFound(id)               => s"Connection type not found: $id"
    case DomainError.ConnectionNotFound(id)                   => s"Connection not found: $id"
    case DomainError.SpecialistNotAvailable(id)               => s"Specialist not available: $id"
    case DomainError.InvalidCredentials                       => "Invalid credentials"

  extension (result: ValidationResult)
    /** Converts the validation result to an Either */
    def toEither: Either[DomainError, Unit] = result match
      case Valid => Right(())
      case Invalid(errors) =>
        if (errors.length == 1) Left(errors.head)
        else
          Left(
            DomainError.ValidationError(
              s"Multiple validation errors: ${errors.map(getErrorMessage).mkString(", ")}"
            )
          )

    /** Gets all errors */
    def getAllErrors: List[DomainError] = result.errors

    /** Gets the first error if any */
    def firstError: Option[DomainError] = result match
      case Valid           => None
      case Invalid(errors) => errors.headOption

end ValidationResult
