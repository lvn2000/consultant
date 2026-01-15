package com.consultant.data.repository

import cats.effect.IO
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import com.consultant.core.domain.*
import com.consultant.core.domain.security.UserRole
import com.consultant.core.ports.UserRepository
import java.util.UUID
import java.time.Instant
import org.mindrot.jbcrypt.BCrypt

class PostgresUserRepository(xa: Transactor[IO]) extends UserRepository:

  // Doobie Meta instance for UserRole enum
  given userRoleMeta: Meta[UserRole] =
    Meta[String].timap(str => UserRole.valueOf(str))(_.toString)

  override def create(request: CreateUserRequest): IO[User] =
    val id   = UUID.randomUUID()
    val now  = Instant.now()
    val user = User(id, request.login, request.email, request.name, request.phone, request.role, now, now)

    sql"""
      INSERT INTO users (id, login, email, name, phone, role, created_at, updated_at)
      VALUES (${user.id}, ${user.login}, ${user.email}, ${user.name}, ${user.phone}, ${user.role}, ${user.createdAt}, ${user.updatedAt})
    """.update.run
      .transact(xa)
      .map(_ => user)

  override def findById(id: UserId): IO[Option[User]] =
    sql"""
      SELECT id, email, name, phone, role, created_at, updated_at
      FROM users
      WHERE id = $id
    """.query[User].option.transact(xa)

  override def findByEmail(email: String): IO[Option[User]] =
    sql"""
      SELECT id, email, name, phone, role, created_at, updated_at
      FROM users
      WHERE email = $email
    """.query[User].option.transact(xa)

  override def update(user: User): IO[User] =
    val updatedUser = user.copy(updatedAt = Instant.now())
    sql"""
      UPDATE users
      SET email = ${updatedUser.email},
          name = ${updatedUser.name},
          phone = ${updatedUser.phone},
          role = ${updatedUser.role},
          updated_at = ${updatedUser.updatedAt}
      WHERE id = ${updatedUser.id}
    """.update.run
      .transact(xa)
      .map(_ => updatedUser)

  override def delete(id: UserId): IO[Unit] =
    sql"DELETE FROM users WHERE id = $id".update.run
      .transact(xa)
      .void

  override def list(offset: Int, limit: Int): IO[List[User]] =
    sql"""
      SELECT id, email, name, phone, role, created_at, updated_at
      FROM users
      ORDER BY created_at DESC
      LIMIT $limit OFFSET $offset
    """.query[User].to[List].transact(xa)

  override def login(login: String, password: String): IO[Option[User]] =
    sql"""
      SELECT u.id, u.login, u.email, u.name, u.phone, u.role, u.created_at, u.updated_at, c.password_hash
      FROM users u
      JOIN credentials c ON u.id = c.user_id
      WHERE u.login = $login AND c.is_active = true
    """.query[(User, String)].option.transact(xa).map {
      case Some((user, hash)) if BCrypt.checkpw(password, hash) => Some(user)
      case _                                                    => None
    }
