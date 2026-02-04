package com.consultant.core.domain

import types.*

// Domain errors
enum DomainError:
  case UserNotFound(id: UserId)
  case SpecialistNotFound(id: SpecialistId)
  case CategoryNotFound(id: CategoryId)
  case ConsultationNotFound(id: ConsultationId)
  case EmailAlreadyExists(email: String)
  case InvalidEmail(email: String)
  case InvalidPhoneNumber(phone: String)
  case InvalidPrice(price: BigDecimal)
  case SpecialistNotAvailable(id: SpecialistId)
  case ValidationError(message: String)
  case InvalidCredentials
  case DuplicateCategoryRate(categoryId: String)
  case DatabaseError(message: String)
