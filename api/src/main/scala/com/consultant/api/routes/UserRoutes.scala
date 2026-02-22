package com.consultant.api.routes

import cats.effect.IO
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import com.consultant.api.dto.*
import com.consultant.core.service.UserService
import com.consultant.infrastructure.security.JwtTokenService
import com.consultant.api.DtoMappers.*
import java.util.UUID
import sttp.tapir.server.http4s.Http4sServerInterpreter
import org.http4s.HttpRoutes
import com.consultant.api.codec.SecurityCodecs.given

class UserRoutes(userService: UserService, jwtService: Option[JwtTokenService] = None):

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

  // Update user
  val updateUserEndpoint = baseEndpoint.put
    .in(path[UUID]("userId"))
    .in(jsonBody[UpdateUserDto])
    .out(jsonBody[UserDto])
    .errorOut(jsonBody[ErrorResponse])

  val updateUser = updateUserEndpoint.serverLogic { case (id, dto) =>
    userService.getUser(id).flatMap {
      case Left(error) => IO.pure(Left(toErrorResponse(error)))
      case Right(user) =>
        val updated = user.copy(
          name = dto.name,
          email = dto.email,
          phone = dto.phone
        )
        userService.updateUser(updated).map {
          case Right(updatedUser) => Right(toUserDto(updatedUser))
          case Left(error)        => Left(toErrorResponse(error))
        }
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
    userService.login(dto.login, dto.password, "0.0.0.0", "unknown").flatMap {
      case Right(result) =>
        // Generate JWT access token if jwtService is available
        jwtService match
          case Some(service) =>
            service.generateAccessToken(result.user.id, result.user.role, result.user.email).map { authToken =>
              Right(
                LoginResponseDto(
                  result.user.id.toString,
                  result.user.login,
                  result.user.email,
                  result.user.role.toString,
                  result.session.sessionId,
                  Some(authToken.token)
                )
              )
            }
          case None =>
            IO.pure(
              Right(
                LoginResponseDto(
                  result.user.id.toString,
                  result.user.login,
                  result.user.email,
                  result.user.role.toString,
                  result.session.sessionId,
                  None
                )
              )
            )

      case Left(error) => IO.pure(Left(toErrorResponse(error)))
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

  // Get admin count (separate path to avoid conflicts)
  // Using empty string for path since Router provides the full path
  val getAdminCountEndpoint = endpoint.get
    .in("")
    .out(jsonBody[AdminCountDto])
    .errorOut(jsonBody[ErrorResponse])

  val getAdminCount = getAdminCountEndpoint.serverLogic { _ =>
    userService.getAdminCount().flatMap { count =>
      IO.println(s"[ADMIN-COUNT] Count from DB: $count").as(Right(AdminCountDto(count)))
    }
  }

  // Delete user
  val deleteUserEndpoint = baseEndpoint.delete
    .in(path[UUID]("userId"))
    .out(stringBody)
    .errorOut(jsonBody[ErrorResponse])

  val deleteUser = deleteUserEndpoint.serverLogic { userId =>
    userService.deleteUser(userId, None, None).map {
      case Right(_)    => Right("User deleted successfully")
      case Left(error) => Left(toErrorResponse(error))
    }
  }

  val endpoints = List(
    test,
    createUser,
    getUser,
    updateUser,
    listUsers,
    login,
    logout,
    getAdminCount,
    deleteUser
  )

  val routes: HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(endpoints)

  // Exposed for separate routing
  val getAdminCountRoute = Http4sServerInterpreter[IO]().toRoutes(List(getAdminCount))
