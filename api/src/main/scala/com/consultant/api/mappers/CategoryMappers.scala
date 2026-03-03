package com.consultant.api.mappers

import com.consultant.core.domain.{ Category, CreateCategoryRequest }
import com.consultant.api.dto.{ CategoryDto, CreateCategoryDto }

/** Mappers for Category domain <-> DTO conversions. */
object CategoryMappers:

  /** Mapper from Category domain to CategoryDto */
  given DtoMapper[Category, CategoryDto] with
    def toDto(category: Category): CategoryDto =
      CategoryDto(category.id, category.name, category.description, category.parentId)

  /** Mapper from CreateCategoryDto to CreateCategoryRequest */
  given RequestDtoMapper[CreateCategoryDto, CreateCategoryRequest] with
    def toRequest(dto: CreateCategoryDto): CreateCategoryRequest =
      CreateCategoryRequest(dto.name, dto.description, dto.parentId)

  // Convenience methods for explicit usage
  def toCategoryDto(category: Category): CategoryDto =
    summon[DtoMapper[Category, CategoryDto]].toDto(category)

  def toCreateCategoryRequest(dto: CreateCategoryDto): CreateCategoryRequest =
    summon[RequestDtoMapper[CreateCategoryDto, CreateCategoryRequest]].toRequest(dto)

end CategoryMappers
