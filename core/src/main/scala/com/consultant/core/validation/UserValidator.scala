package com.consultant.core.validation

import com.consultant.core.domain.{ CreateUserRequest, DomainError, User }
import Validator.*
import ValidationResult.*

/** Validator for User-related requests. */
object UserValidator:

  /**
   * Validates a CreateUserRequest.
   *
   * @param request
   *   The request to validate
   * @return
   *   ValidationResult indicating success or failure
   */
  def validateCreate(request: CreateUserRequest): ValidationResult =
    all(
      // Validate login
      nonEmpty(request.login, "Login"),
      minLength(request.login, 3, "Login"),
      maxLength(request.login, 50, "Login"),

      // Validate email
      validate(request.email)
        .must(isValidEmail, DomainError.InvalidEmail(request.email)),

      // Validate name
      nonEmpty(request.name, "Name"),
      minLength(request.name, 2, "Name"),
      maxLength(request.name, 100, "Name"),

      // Validate phone if provided
      request.phone match
        case Some(phone) =>
          check(
            isValidPhone(phone),
            DomainError.InvalidPhoneNumber(phone)
          )
        case None => Valid
    )

  /**
   * Validates a User update.
   *
   * @param user
   *   The user to validate
   * @return
   *   ValidationResult indicating success or failure
   */
  def validateUpdate(user: User): ValidationResult =
    all(
      // Validate name
      nonEmpty(user.name, "Name"),
      minLength(user.name, 2, "Name"),
      maxLength(user.name, 100, "Name"),

      // Validate email
      validate(user.email)
        .must(isValidEmail, DomainError.InvalidEmail(user.email)),

      // Validate phone if provided
      user.phone match
        case Some(phone) =>
          check(
            isValidPhone(phone),
            DomainError.InvalidPhoneNumber(phone)
          )
        case None => Valid
    )

end UserValidator
