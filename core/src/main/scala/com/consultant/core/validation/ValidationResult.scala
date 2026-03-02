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

  extension (result: ValidationResult)
    /** Converts the validation result to an Either */
    def toEither: Either[DomainError, Unit] = result match
      case Valid           => Right(())
      case Invalid(errors) => Left(errors.head)

    /** Gets the first error if any */
    def firstError: Option[DomainError] = result match
      case Valid           => None
      case Invalid(errors) => errors.headOption

end ValidationResult
