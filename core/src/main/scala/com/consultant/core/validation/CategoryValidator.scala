package com.consultant.core.validation

import com.consultant.core.domain.{ Category, CreateCategoryRequest, DomainError }
import Validator.*
import ValidationResult.*

/** Validator for Category-related requests. */
object CategoryValidator:

  /**
   * Validates a CreateCategoryRequest.
   *
   * @param request
   *   The request to validate
   * @return
   *   ValidationResult indicating success or failure
   */
  def validateCreate(request: CreateCategoryRequest): ValidationResult =
    all(
      // Validate name
      nonEmpty(request.name, "Name"),
      minLength(request.name, 2, "Name"),
      maxLength(request.name, 100, "Name"),

      // Validate description (can be empty but has max length)
      maxLength(request.description, 500, "Description")
    )

  /**
   * Validates a Category update.
   *
   * @param category
   *   The category to validate
   * @return
   *   ValidationResult indicating success or failure
   */
  def validateUpdate(category: Category): ValidationResult =
    all(
      // Validate name
      nonEmpty(category.name, "Name"),
      minLength(category.name, 2, "Name"),
      maxLength(category.name, 100, "Name"),

      // Validate description (can be empty but has max length)
      maxLength(category.description, 500, "Description")
    )

end CategoryValidator
