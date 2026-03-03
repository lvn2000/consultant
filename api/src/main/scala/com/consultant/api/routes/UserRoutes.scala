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

  /**
   * SECURITY MODEL: The X-Auth-User-Id and X-User-Role headers are set by TokenAuthMiddleware from the verified JWT
   * token. The middleware strips any client-provided values for these headers and replaces them with trusted values
   * extracted from the authenticated token. Routes can safely rely on these headers for authorization.
   */

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
    .in(header[Option[String]]("X-Auth-User-Id"))
    .in(path[UUID]("userId"))
    .out(jsonBody[UserDto])

  val getUser = getUserEndpoint.serverLogic { case (authUserIdOpt, id) =>
    (for
      authUserId <- IO.fromOption(authUserIdOpt)(new RuntimeException("Missing authentication header"))
      _ <-
        if authUserId != id.toString then
          IO.raiseError(new RuntimeException("Unauthorized: Cannot access another user's data"))
        else IO.unit
      result <- userService.getUser(id).map {
        case Right(user) => Right(toUserDto(user))
        case Left(error) => Left(toErrorResponse(error))
      }
    yield result).handleErrorWith { error =>
      IO.pure(Left(ErrorResponse("UNAUTHORIZED", error.getMessage)))
    }
  }

  // Update user
  val updateUserEndpoint = ApiEndpoints
    .securedEndpoint("updateUser", "Update user")
    .put
    .in(header[Option[String]]("X-Auth-User-Id"))
    .in(path[UUID]("userId"))
    .in(jsonBody[UpdateUserDto])
    .out(jsonBody[UserDto])

  val updateUser = updateUserEndpoint.serverLogic { case (authUserIdOpt, id, dto) =>
    (for
      authUserId <- IO.fromOption(authUserIdOpt)(new RuntimeException("Missing authentication header"))
      _ <-
        if authUserId != id.toString then
          IO.raiseError(new RuntimeException("Unauthorized: Cannot update another user's data"))
        else IO.unit
      userOpt <- userService.getUser(id)
      result <- userOpt match
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
        case Left(error) => IO.pure(Left(toErrorResponse(error)))
    yield result).handleErrorWith { error =>
      IO.pure(Left(ErrorResponse("UNAUTHORIZED", error.getMessage)))
    }
  }

  // List users
  val listUsersEndpoint = ApiEndpoints
    .securedEndpoint("listUsers", "List users")
    .get
    .in(query[Option[Int]]("offset").default(Some(0)))
    .in(query[Option[Int]]("limit").default(Some(20)))
    .out(jsonBody[List[UserDto]])

  val listUsers = listUsersEndpoint.serverLogic { case (offset, limit) =>
    userService
      .listUsers(offset.getOrElse(0), limit.getOrElse(20))
      .map(users => Right(users.map(toUserDto)))
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
    .in(header[Option[String]]("X-User-Role"))
    .out(jsonBody[AdminCountDto])

  val getAdminCount = getAdminCountEndpoint.serverLogic { userRoleOpt =>
    userRoleOpt match
      case Some(role) if role.equalsIgnoreCase("Admin") =>
        userService.getAdminCount().map { count =>
          Right(AdminCountDto(count))
        }
      case _ =>
        IO.pure(Left(ErrorResponse("FORBIDDEN", "Admin role required")))
  }

  // Delete user
  val deleteUserEndpoint = ApiEndpoints
    .adminEndpoint("deleteUser", "Delete user")
    .delete
    .in(path[UUID]("userId"))
    .in(header[Option[String]]("X-User-Role"))
    .out(stringBody)

  val deleteUser = deleteUserEndpoint.serverLogic { case (userId, userRoleOpt) =>
    userRoleOpt match
      case Some(role) if role.equalsIgnoreCase("Admin") =>
        userService.deleteUser(userId, None, None).map {
          case Right(_)    => Right("User deleted successfully")
          case Left(error) => Left(toErrorResponse(error))
        }
      case _ =>
        IO.pure(Left(ErrorResponse("FORBIDDEN", "Admin role required")))
  }

  val endpoints = List(
    test,
    createUser,
    getUser,
    updateUser,
    listUsers,
    logout,
    getAdminCount,
    deleteUser
  )

  val routes: HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(endpoints)

  // Exposed for separate routing
  val getAdminCountRoute = Http4sServerInterpreter[IO]().toRoutes(List(getAdminCount))
