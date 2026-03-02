package com.consultant.api.routes

import cats.effect.IO
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import com.consultant.api.dto.*
import com.consultant.core.service.UserService
import com.consultant.infrastructure.security.JwtTokenService
import com.consultant.api.mappers.UserMappers.*
import com.consultant.api.mappers.ErrorMappers.*
import java.util.UUID
import sttp.tapir.server.http4s.Http4sServerInterpreter
import org.http4s.HttpRoutes
import com.consultant.api.codec.SecurityCodecs.given

class UserRoutes(userService: UserService, jwtService: Option[JwtTokenService] = None):

  // Create user
  val createUserEndpoint = ApiEndpoints
    .publicEndpoint("createUser", "Register a new user")
    .post
    .in("register")
    .in(jsonBody[CreateUserDto])
    .out(jsonBody[UserDto])

  val createUser = createUserEndpoint.serverLogic { dto =>
    userService.createUser(toCreateUserRequest(dto)).map {
      case Right(user) => Right(toUserDto(user))
      case Left(error) => Left(toErrorResponse(error))
    }
  }

  // Get user by ID
  val getUserEndpoint = ApiEndpoints
    .securedEndpoint("getUser", "Get user by ID")
    .get
    .in(path[UUID]("userId"))
    .out(jsonBody[UserDto])

  val getUser = getUserEndpoint.serverLogic { id =>
    userService.getUser(id).map {
      case Right(user) => Right(toUserDto(user))
      case Left(error) => Left(toErrorResponse(error))
    }
  }

  // Update user
  val updateUserEndpoint = ApiEndpoints
    .securedEndpoint("updateUser", "Update user")
    .put
    .in(path[UUID]("userId"))
    .in(jsonBody[UpdateUserDto])
    .out(jsonBody[UserDto])

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
  val listUsersEndpoint = ApiEndpoints
    .securedEndpoint("listUsers", "List users")
    .get
    .in(query[Option[Int]]("offset").default(Some(0)))
    .in(query[Option[Int]]("limit").default(Some(20)))
    .out(jsonBody[List[UserDto]])

  // Login user
  val loginEndpoint = ApiEndpoints
    .publicEndpoint("login", "Login with credentials")
    .post
    .in("login")
    .in(jsonBody[LoginDto])
    .out(jsonBody[LoginResponseDto])

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
  val logoutEndpoint = ApiEndpoints
    .securedEndpoint("logout", "Logout user")
    .post
    .in("logout")
    .in(jsonBody[LogoutDto])
    .out(stringBody)

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
  val testEndpoint = ApiEndpoints
    .publicEndpoint("testApi", "Test API endpoint")
    .get
    .in("test-api")
    .out(stringBody)

  val test = testEndpoint.serverLogic { _ =>
    IO.pure(Right("API is working!"))
  }

  // Get admin count (separate path to avoid conflicts)
  val getAdminCountEndpoint = ApiEndpoints
    .adminEndpoint("getAdminCount", "Get admin user count")
    .get
    .in("")
    .out(jsonBody[AdminCountDto])

  val getAdminCount = getAdminCountEndpoint.serverLogic { _ =>
    userService.getAdminCount().map { count =>
      Right(AdminCountDto(count))
    }
  }

  // Delete user
  val deleteUserEndpoint = ApiEndpoints
    .adminEndpoint("deleteUser", "Delete user")
    .delete
    .in(path[UUID]("userId"))
    .out(stringBody)

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
