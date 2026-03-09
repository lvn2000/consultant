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
package com.consultant.core.domain

import types.*

// Domain errors
enum DomainError:
  // Not found errors
  case UserNotFound(id: UserId)
  case SpecialistNotFound(id: SpecialistId)
  case CategoryNotFound(id: CategoryId)
  case ConsultationNotFound(id: ConsultationId)
  case ConnectionTypeNotFound(id: ConnectionTypeId)
  case ConnectionNotFound(id: String)

  // Conflict errors
  case EmailAlreadyExists(email: String)
  case LoginAlreadyExists(login: String)
  case DuplicateCategoryRate(categoryId: String)
  case DuplicateEntry(message: String)
  case ReferencedRecordNotFound(message: String)

  // Validation errors
  case InvalidEmail(email: String)
  case InvalidPhoneNumber(phone: String)
  case InvalidPrice(price: BigDecimal)
  case ValidationError(message: String)
  case InvalidCredentials

  // State errors
  case SpecialistNotAvailable(id: SpecialistId)
  case Forbidden(message: String)
  case Unauthorized(message: String = "Authentication required")

  // Database errors
  case DatabaseError(message: String)
  case ConstraintViolation(constraintName: String, message: String)

  // System errors
  case UnexpectedError(message: String)
  case ExternalServiceError(service: String, message: String)
