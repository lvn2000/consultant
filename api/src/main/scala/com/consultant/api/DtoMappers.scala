package com.consultant.api

import com.consultant.core.domain.*
import com.consultant.api.dto.*

object DtoMappers:

  // User mappers
  def toUserDto(user: User): UserDto =
    UserDto(user.id, user.email, user.name, user.phone, user.role, user.createdAt, user.updatedAt)

  def toCreateUserRequest(dto: CreateUserDto): CreateUserRequest =
    CreateUserRequest(dto.email, dto.name, dto.phone, dto.role)

  // Specialist mappers
  def toSpecialistDto(specialist: Specialist): SpecialistDto =
    SpecialistDto(
      specialist.id,
      specialist.email,
      specialist.name,
      specialist.phone,
      specialist.bio,
      specialist.categories,
      specialist.hourlyRate,
      specialist.experienceYears,
      specialist.rating,
      specialist.totalConsultations,
      specialist.isAvailable,
      specialist.connections.map(toSpecialistConnectionDto),
      specialist.createdAt,
      specialist.updatedAt
    )

  def toCreateSpecialistRequest(dto: CreateSpecialistDto): CreateSpecialistRequest =
    CreateSpecialistRequest(
      dto.email,
      dto.name,
      dto.phone,
      dto.bio,
      dto.categories,
      dto.hourlyRate,
      dto.experienceYears
    )

  def toSpecialistSearchCriteria(dto: SpecialistSearchDto): SpecialistSearchCriteria =
    SpecialistSearchCriteria(
      dto.categoryId,
      dto.minRating,
      dto.maxHourlyRate,
      dto.minExperience,
      dto.isAvailable
    )

  // Category mappers
  def toCategoryDto(category: Category): CategoryDto =
    CategoryDto(category.id, category.name, category.description, category.parentId)

  def toCreateCategoryRequest(dto: CreateCategoryDto): CreateCategoryRequest =
    CreateCategoryRequest(dto.name, dto.description, dto.parentId)

  // Consultation mappers
  def toConsultationDto(consultation: Consultation): ConsultationDto =
    ConsultationDto(
      consultation.id,
      consultation.userId,
      consultation.specialistId,
      consultation.categoryId,
      consultation.description,
      consultation.status.toString,
      consultation.scheduledAt,
      consultation.duration,
      consultation.price,
      consultation.rating,
      consultation.review,
      consultation.createdAt,
      consultation.updatedAt
    )

  def toCreateConsultationRequest(dto: CreateConsultationDto): CreateConsultationRequest =
    CreateConsultationRequest(
      dto.userId,
      dto.specialistId,
      dto.categoryId,
      dto.description,
      dto.scheduledAt,
      dto.duration
    )

  // Connection mappers
  def toConnectionTypeDto(connType: ConnectionType): ConnectionTypeDto =
    ConnectionTypeDto(
      connType.id,
      connType.name,
      connType.description,
      connType.createdAt,
      connType.updatedAt
    )

  def toSpecialistConnectionDto(connection: SpecialistConnection): SpecialistConnectionDto =
    SpecialistConnectionDto(
      connection.id,
      connection.specialistId,
      connection.connectionTypeId,
      connection.connectionValue,
      connection.isVerified,
      connection.createdAt,
      connection.updatedAt
    )

  def toCreateConnectionRequest(dto: CreateConnectionDto): CreateConnectionRequest =
    CreateConnectionRequest(
      dto.connectionTypeId,
      dto.connectionValue
    )

  // Error mapper
  def toErrorResponse(error: DomainError): ErrorResponse =
    error match
      case DomainError.UserNotFound(id)           => ErrorResponse("NOT_FOUND", s"User not found: $id")
      case DomainError.SpecialistNotFound(id)     => ErrorResponse("NOT_FOUND", s"Specialist not found: $id")
      case DomainError.CategoryNotFound(id)       => ErrorResponse("NOT_FOUND", s"Category not found: $id")
      case DomainError.ConsultationNotFound(id)   => ErrorResponse("NOT_FOUND", s"Consultation not found: $id")
      case DomainError.EmailAlreadyExists(email)  => ErrorResponse("CONFLICT", s"Email already exists: $email")
      case DomainError.InvalidEmail(email)        => ErrorResponse("VALIDATION_ERROR", s"Invalid email: $email")
      case DomainError.InvalidPhoneNumber(phone)  => ErrorResponse("VALIDATION_ERROR", s"Invalid phone: $phone")
      case DomainError.InvalidPrice(price)        => ErrorResponse("VALIDATION_ERROR", s"Invalid price: $price")
      case DomainError.SpecialistNotAvailable(id) => ErrorResponse("UNAVAILABLE", s"Specialist not available: $id")
      case DomainError.ValidationError(msg)       => ErrorResponse("VALIDATION_ERROR", msg)
