package com.consultant.core.validation

import com.consultant.core.domain.{ CreateSpecialistRequest, DomainError, Specialist, SpecialistCategoryRate }
import Validator.*
import ValidationResult.*

/** Validator for Specialist-related requests. */
object SpecialistValidator:

  /**
   * Validates a CreateSpecialistRequest.
   *
   * @param request
   *   The request to validate
   * @return
   *   ValidationResult indicating success or failure
   */
  def validateCreate(request: CreateSpecialistRequest): ValidationResult =
    all(
      // Validate email
      validate(request.email)
        .must(isValidEmail, DomainError.InvalidEmail(request.email)),

      // Validate name
      nonEmpty(request.name, "Name"),
      minLength(request.name, 2, "Name"),
      maxLength(request.name, 100, "Name"),

      // Validate phone (must be valid if not empty)
      validate(request.phone)
        .must(
          phone => phone.isEmpty || isValidPhone(phone),
          DomainError.InvalidPhoneNumber(request.phone)
        ),

      // Validate category rates
      check(
        request.categoryRates.nonEmpty,
        DomainError.ValidationError("At least one category rate is required")
      ),

      // Validate each category rate
      validateCategoryRates(request.categoryRates)
    )

  /**
   * Validates a Specialist update.
   *
   * @param specialist
   *   The specialist to validate
   * @return
   *   ValidationResult indicating success or failure
   */
  def validateUpdate(specialist: Specialist): ValidationResult =
    all(
      // Validate name
      nonEmpty(specialist.name, "Name"),
      minLength(specialist.name, 2, "Name"),
      maxLength(specialist.name, 100, "Name"),

      // Validate category rates
      validateCategoryRates(specialist.categoryRates),

      // Validate email
      validate(specialist.email)
        .must(isValidEmail, DomainError.InvalidEmail(specialist.email))
    )

  /** Validates category rates. */
  private def validateCategoryRates(rates: List[SpecialistCategoryRate]): ValidationResult =
    val validations = rates.map { rate =>
      all(
        // Hourly rate must be positive
        positive(rate.hourlyRate, "Hourly rate"),

        // Experience years must be non-negative
        nonNegative(rate.experienceYears, "Experience years")
      )
    }

    validations.foldLeft[ValidationResult](Valid)(_ ++ _)

end SpecialistValidator
