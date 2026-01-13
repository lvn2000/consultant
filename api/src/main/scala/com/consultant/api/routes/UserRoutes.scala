package com.consultant.api.routes

import cats.effect.IO
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import com.consultant.api.dto.*
import com.consultant.core.service.UserService
import com.consultant.api.DtoMappers.*
import java.util.UUID
import sttp.tapir.server.http4s.Http4sServerInterpreter
import org.http4s.HttpRoutes

class UserRoutes(userService: UserService):

  private val baseEndpoint = endpoint.in("api" / "users")

  // Create user
  val createUserEndpoint = baseEndpoint.post
    .in(jsonBody[CreateUserDto])
    .out(jsonBody[UserDto])
    .errorOut(jsonBody[ErrorResponse])

  val createUser = createUserEndpoint.serverLogic { dto =>
    userService.createUser(toCreateUserRequest(dto)).map {
      case Right(user) => Right(toUserDto(user))
      case Left(error) => Left(toErrorResponse(error))
    }
  }

  // Get user by ID
  val getUserEndpoint = baseEndpoint.get
    .in(path[UUID]("userId"))
    .out(jsonBody[UserDto])
    .errorOut(jsonBody[ErrorResponse])

  val getUser = getUserEndpoint.serverLogic { id =>
    userService.getUser(id).map {
      case Right(user) => Right(toUserDto(user))
      case Left(error) => Left(toErrorResponse(error))
    }
  }

  // List users
  val listUsersEndpoint = baseEndpoint.get
    .in(query[Option[Int]]("offset").default(Some(0)))
    .in(query[Option[Int]]("limit").default(Some(20)))
    .out(jsonBody[List[UserDto]])

  val listUsers = listUsersEndpoint.serverLogic { case (offset, limit) =>
    userService
      .listUsers(offset.getOrElse(0), limit.getOrElse(20))
      .map(users => Right(users.map(toUserDto)))
  }

  val endpoints = List(createUser, getUser, listUsers)

  val routes: HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(endpoints)
