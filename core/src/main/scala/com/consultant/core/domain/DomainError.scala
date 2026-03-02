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
