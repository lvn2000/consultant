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
import com.consultant.api.codec.SecurityCodecs.given

class UserRoutes(userService: UserService):

  private val baseEndpoint = endpoint

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

  // Login user
  val loginEndpoint = endpoint.post
    .in(jsonBody[LoginDto].schema(Schema.derived[LoginDto]))
    .out(jsonBody[LoginResponseDto])
    .errorOut(jsonBody[ErrorResponse])

  val login = loginEndpoint.serverLogic { dto =>
    userService.login(dto.login, dto.password).map {
      case Right(user) => Right(LoginResponseDto(user.id.toString, user.login, user.email, user.role.toString))
      case Left(error) => Left(toErrorResponse(error))
    }
  }

  val listUsers = listUsersEndpoint.serverLogic { case (offset, limit) =>
    userService
      .listUsers(offset.getOrElse(0), limit.getOrElse(20))
      .map(users => Right(users.map(toUserDto)))
  }

  // Test endpoint to verify API is working
  val testEndpoint = baseEndpoint.get
    .in("test-api")
    .out(stringBody)

  val test = testEndpoint.serverLogic { _ =>
    IO.pure(Right("API is working!"))
  }

  val endpoints = List(test, createUser, listUsers, getUser)

  val routes: HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(endpoints)
