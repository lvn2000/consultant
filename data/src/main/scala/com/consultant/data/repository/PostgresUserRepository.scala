package com.consultant.data.repository

import cats.data.NonEmptyList
import cats.effect.Async
import doobie.free.connection
import cats.syntax.all._

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
  // Doobie Meta instance for UserRole enum
  given userRoleMeta: Meta[UserRole] =
    Meta[String].timap(str => UserRole.valueOf(str))(_.toString)

  override def login(login: String, password: String): IO[Option[User]] = {
    val userQ = sql"""
      SELECT u.id, u.login, u.email, u.name, u.phone, u.role, u.country_id, c.password_hash, u.created_at, u.updated_at
      FROM users u
      JOIN credentials c ON u.id = c.user_id
      WHERE (LOWER(u.login) = LOWER($login) OR LOWER(u.email) = LOWER($login))
        AND c.is_active = true
    """.query[(UUID, String, String, String, Option[String], UserRole, Option[UUID], String, Instant, Instant)].option
    val action: ConnectionIO[Option[User]] = for {
      userOpt <- userQ
      langs <- userOpt match {
        case Some((id, _, _, _, _, _, _, _, _, _)) =>
          sql"SELECT language_id FROM user_languages WHERE user_id = $id".query[UUID].to[List]
        case None => connection.pure(List.empty[UUID])
      }
    } yield userOpt.flatMap { case (id, login, email, name, phone, role, countryId, passwordHash, created, updated) =>
      if (BCrypt.checkpw(password, passwordHash))
        Some(User(id, login, email, name, phone, role, countryId, langs.toSet, created, updated))
      else None
    }
    action.transact(xa)
  }

  override def create(request: CreateUserRequest): IO[User] = {
    val id  = UUID.randomUUID()
    val now = Instant.now()
    val user = User(
      id,
      request.login,
      request.email,
      request.name,
      request.phone,
      request.role,
      request.countryId,
      request.languages,
      now,
      now
    )
    val action: ConnectionIO[User] = for {
      _ <- sql"""
        INSERT INTO users (id, login, email, name, phone, role, country_id, created_at, updated_at)
        VALUES (${user.id}, ${user.login}, ${user.email}, ${user.name}, ${user.phone}, ${user.role}, ${user.countryId}, ${user.createdAt}, ${user.updatedAt})
      """.update.run
      _ <- insertUserLanguages(user.id, request.languages)
    } yield user
    action.transact(xa)
  }

  override def findById(id: UserId): IO[Option[User]] = {
    val userQ = sql"""
      SELECT id, login, email, name, phone, role, country_id, created_at, updated_at
      FROM users
      WHERE id = $id
    """.query[(UUID, String, String, String, Option[String], UserRole, Option[UUID], Instant, Instant)].option
    val languagesQ = sql"SELECT language_id FROM user_languages WHERE user_id = $id".query[UUID].to[List]
    val action: ConnectionIO[Option[User]] = for {
      userOpt <- userQ
      langs   <- languagesQ
    } yield userOpt.map { case (id, login, email, name, phone, role, countryId, created, updated) =>
      User(id, login, email, name, phone, role, countryId, langs.toSet, created, updated)
    }
    action.transact(xa)
  }

  override def findByEmail(email: String): IO[Option[User]] = {
    val userQ = sql"""
      SELECT id, login, email, name, phone, role, country_id, created_at, updated_at
      FROM users
      WHERE email = $email
    """.query[(UUID, String, String, String, Option[String], UserRole, Option[UUID], Instant, Instant)].option
    val action: ConnectionIO[Option[User]] = for {
      userOpt <- userQ
      langs <- userOpt match {
        case Some((id, _, _, _, _, _, _, _, _)) =>
          sql"SELECT language_id FROM user_languages WHERE user_id = $id".query[UUID].to[List]
        case None => connection.pure(List.empty[UUID])
      }
    } yield userOpt.map { case (id, login, email, name, phone, role, countryId, created, updated) =>
      User(id, login, email, name, phone, role, countryId, langs.toSet, created, updated)
    }
    action.transact(xa)
  }

  override def findByLogin(login: String): IO[Option[User]] = {
    val userQ = sql"""
      SELECT id, login, email, name, phone, role, country_id, created_at, updated_at
      FROM users
      WHERE login = $login
    """.query[(UUID, String, String, String, Option[String], UserRole, Option[UUID], Instant, Instant)].option
    val action: ConnectionIO[Option[User]] = for {
      userOpt <- userQ
      langs <- userOpt match {
        case Some((id, _, _, _, _, _, _, _, _)) =>
          sql"SELECT language_id FROM user_languages WHERE user_id = $id".query[UUID].to[List]
        case None => connection.pure(List.empty[UUID])
      }
    } yield userOpt.map { case (id, login, email, name, phone, role, countryId, created, updated) =>
      User(id, login, email, name, phone, role, countryId, langs.toSet, created, updated)
    }
    action.transact(xa)
  }

  override def update(user: User): IO[User] = {
    val updatedUser = user.copy(updatedAt = Instant.now())
    val action: ConnectionIO[User] = for {
      _ <- sql"""
        UPDATE users
        SET email = ${updatedUser.email},
            name = ${updatedUser.name},
            phone = ${updatedUser.phone},
            country_id = ${updatedUser.countryId},
            updated_at = ${updatedUser.updatedAt}
        WHERE id = ${updatedUser.id}
      """.update.run
      _ <- deleteUserLanguages(updatedUser.id)
      _ <- insertUserLanguages(updatedUser.id, updatedUser.languages)
    } yield updatedUser
    action.transact(xa)
  }

  override def list(offset: Int, limit: Int): IO[List[User]] = {
    val usersQ = sql"""
      SELECT id, login, email, name, phone, role, country_id, created_at, updated_at
      FROM users
      ORDER BY created_at DESC
      LIMIT $limit OFFSET $offset
    """.query[(UUID, String, String, String, Option[String], UserRole, Option[UUID], Instant, Instant)].to[List]
    def languagesQ(ids: List[UUID]): ConnectionIO[List[(UUID, UUID)]] =
      NonEmptyList.fromList(ids) match {
        case Some(nel) =>
          (fr"SELECT user_id, language_id FROM user_languages WHERE " ++ doobie.util.fragments.in(fr"user_id", nel))
            .query[(UUID, UUID)]
            .to[List]
        case None => connection.pure(List.empty[(UUID, UUID)])
      }
    val action: ConnectionIO[List[User]] = for {
      users     <- usersQ
      langPairs <- languagesQ(users.map(_._1))
      langMap = langPairs.groupMap(_._1)(_._2)
    } yield users.map { case (id, login, email, name, phone, role, countryId, created, updated) =>
      User(
        id,
        login,
        email,
        name,
        phone,
        role,
        countryId,
        langMap.getOrElse(id, List.empty).toSet,
        created,
        updated
      )
    }
    action.transact(xa)
  }

  // --- User Languages helpers ---
  private def insertUserLanguages(userId: UUID, languages: Set[UUID]): ConnectionIO[Unit] =
    languages.toList.traverse_ { langId =>
      sql"INSERT INTO user_languages (user_id, language_id) VALUES ($userId, $langId)".update.run.void
    }

  private def deleteUserLanguages(userId: UUID): ConnectionIO[Unit] =
    sql"DELETE FROM user_languages WHERE user_id = $userId".update.run.void

  // (Removed duplicate/broken findById)
  // (Removed duplicate/broken findByEmail)
  // (Removed duplicate/broken update)
  override def delete(id: UserId): IO[Unit] =
    sql"DELETE FROM users WHERE id = $id".update.run
      .transact(xa)
      .void

  // (Removed duplicate/broken list)
  // TODO: Reimplement login method to match new User mapping if needed.
// End of file
