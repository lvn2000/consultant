package com.consultant.core.domain

import java.util.UUID
import types.*

// Category/Specialization domain model
case class Category(
  id: CategoryId,
  name: String,
  description: String,
  parentId: Option[CategoryId] // For hierarchical categories
)

case class CreateCategoryRequest(
  name: String,
  description: String,
  parentId: Option[CategoryId]
)
