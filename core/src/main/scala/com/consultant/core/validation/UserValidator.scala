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
