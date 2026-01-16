package com.consultant.api.dto

import java.util.UUID
import io.circe.{ Decoder, Encoder }
import io.circe.Codec

// Category DTOs
case class CreateCategoryDto(
  name: String,
  description: String,
  parentId: Option[UUID]
) derives Codec.AsObject

case class CategoryDto(
  id: UUID,
  name: String,
  description: String,
  parentId: Option[UUID]
) derives Codec.AsObject

case class UpdateCategoryDto(
  name: String,
  description: String,
  parentId: Option[UUID]
) derives Codec.AsObject
