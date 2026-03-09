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
