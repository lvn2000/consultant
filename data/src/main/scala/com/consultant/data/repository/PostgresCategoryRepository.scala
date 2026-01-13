package com.consultant.data.repository

import cats.effect.IO
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import com.consultant.core.domain.*
import com.consultant.core.ports.CategoryRepository
import java.util.UUID

class PostgresCategoryRepository(xa: Transactor[IO]) extends CategoryRepository:

  override def create(request: CreateCategoryRequest): IO[Category] =
    val id       = UUID.randomUUID()
    val category = Category(id, request.name, request.description, request.parentId)

    sql"""
      INSERT INTO categories (id, name, description, parent_id)
      VALUES (${category.id}, ${category.name}, ${category.description}, ${category.parentId})
    """.update.run
      .transact(xa)
      .map(_ => category)

  override def findById(id: CategoryId): IO[Option[Category]] =
    sql"""
      SELECT id, name, description, parent_id
      FROM categories
      WHERE id = $id
    """.query[Category].option.transact(xa)

  override def findByName(name: String): IO[Option[Category]] =
    sql"""
      SELECT id, name, description, parent_id
      FROM categories
      WHERE name = $name
    """.query[Category].option.transact(xa)

  override def listAll(): IO[List[Category]] =
    sql"""
      SELECT id, name, description, parent_id
      FROM categories
      ORDER BY name
    """.query[Category].to[List].transact(xa)

  override def update(category: Category): IO[Category] =
    sql"""
      UPDATE categories
      SET name = ${category.name},
          description = ${category.description},
          parent_id = ${category.parentId}
      WHERE id = ${category.id}
    """.update.run
      .transact(xa)
      .map(_ => category)

  override def delete(id: CategoryId): IO[Unit] =
    sql"DELETE FROM categories WHERE id = $id".update.run
      .transact(xa)
      .void
