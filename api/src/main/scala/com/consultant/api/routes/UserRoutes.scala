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
    .in("register")
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
  val loginEndpoint = baseEndpoint.post
    .in("login")
    .in(jsonBody[LoginDto])
    .out(jsonBody[LoginResponseDto])
    .errorOut(jsonBody[ErrorResponse])

  val login = loginEndpoint.serverLogic { dto =>
    userService.login(dto.login, dto.password, "0.0.0.0", "unknown").map {
      case Right(result) =>
        Right(
          LoginResponseDto(
            result.user.id.toString,
            result.user.login,
            result.user.email,
            result.user.role.toString,
            result.session.sessionId
          )
        )
      case Left(error) => Left(toErrorResponse(error))
    }
  }

  // Logout user session
  val logoutEndpoint = baseEndpoint.post
    .in("logout")
    .in(jsonBody[LogoutDto])
    .out(stringBody)
    .errorOut(jsonBody[ErrorResponse])

  val logout = logoutEndpoint.serverLogic { dto =>
    userService.logout(dto.sessionId).map { success =>
      if success then Right("Logged out successfully")
      else Left(ErrorResponse("LOGOUT_ERROR", "Session not found"))
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

  val endpoints = List(test, createUser, listUsers, getUser, login, logout)

  val routes: HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(endpoints)
