package com.consultant.api.routes

import cats.effect.IO
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import com.consultant.api.dto.ErrorResponse
import com.consultant.infrastructure.security.AuthenticationService
import com.consultant.core.domain.security.UserRole
import io.circe.Codec
import java.time.Instant
import java.util.UUID

/** Authentication and authorization endpoints */
class AuthRoutes(authService: AuthenticationService, legacyAuthEnabled: Boolean):

  // DTOs
  case class RegisterDto(
    email: String,
    password: String,
    name: String,
    phone: Option[String],
    role: String // "client" or "specialist"
  ) derives Codec.AsObject

  case class LoginDto(
    login: String,
    password: String
  ) derives Codec.AsObject

  case class RefreshTokenDto(
    refreshToken: String
  ) derives Codec.AsObject

  case class AuthResponseDto(
    accessToken: String,
    refreshToken: String,
    expiresAt: Instant,
    userId: UUID,
    login: String,
    email: String,
    name: String,
    role: String
  ) derives Codec.AsObject

  case class LogoutDto(
    refreshToken: String
  ) derives Codec.AsObject

  private val baseEndpoint = endpoint
    .in("auth")
    .errorOut(
      oneOf[ErrorResponse](
        oneOfVariant(statusCode(sttp.model.StatusCode.BadRequest).and(jsonBody[ErrorResponse])),
        oneOfVariant(statusCode(sttp.model.StatusCode.Unauthorized).and(jsonBody[ErrorResponse])),
        oneOfVariant(statusCode(sttp.model.StatusCode.InternalServerError).and(jsonBody[ErrorResponse]))
      )
    )

  // POST /auth/register - Registration
  val registerEndpoint = baseEndpoint.post
    .in("register")
    .in(jsonBody[RegisterDto])
    .out(jsonBody[AuthResponseDto])
    .description("Register a new user")

  private def legacyGuard[A](action: => IO[Either[ErrorResponse, A]]): IO[Either[ErrorResponse, A]] =
    if legacyAuthEnabled then action
    else IO.pure(Left(ErrorResponse("LEGACY_AUTH_DISABLED", "Legacy auth endpoints are disabled")))

  def registerRoute = registerEndpoint.serverLogic { dto =>
    legacyGuard {
    val role = dto.role.toLowerCase match
      case "specialist" => UserRole.Specialist
      case "admin"      => UserRole.Admin
      case _            => UserRole.Client

    val request = AuthenticationService.RegistrationRequest(
      email = dto.email,
      password = dto.password,
      name = dto.name,
      phone = dto.phone,
      role = role
    )

    authService.register(request).flatMap {
      case Right(user) =>
        // Auto login after registration
        val loginRequest = AuthenticationService.LoginRequest(
          login = user.login,
          password = dto.password,
          ipAddress = "0.0.0.0", // TODO: extract from request
          userAgent = "unknown"
        )

        authService.login(loginRequest).map {
          case Right(loginResponse) =>
            Right(
              AuthResponseDto(
                accessToken = loginResponse.accessToken,
                refreshToken = loginResponse.refreshToken,
                expiresAt = loginResponse.expiresAt,
                userId = loginResponse.user.id,
                login = loginResponse.user.login,
                email = loginResponse.user.email,
                name = loginResponse.user.name,
                role = loginResponse.user.role.toString
              )
            )
          case Left(error) =>
            Left(ErrorResponse("REGISTRATION_ERROR", error))
        }

      case Left(error) =>
        IO.pure(Left(ErrorResponse("REGISTRATION_ERROR", error)))
    }
    }
  }

  // POST /auth/login - Login
  val loginEndpoint = baseEndpoint.post
    .in("login")
    .in(jsonBody[LoginDto])
    .out(jsonBody[AuthResponseDto])
    .description("Login with credentials")

  def loginRoute = loginEndpoint.serverLogic { dto =>
    legacyGuard {
      val request = AuthenticationService.LoginRequest(
        login = dto.login,
        password = dto.password,
        ipAddress = "0.0.0.0", // TODO: extract from request
        userAgent = "unknown"
      )

      authService.login(request).map {
        case Right(response) =>
          Right(
            AuthResponseDto(
              accessToken = response.accessToken,
              refreshToken = response.refreshToken,
              expiresAt = response.expiresAt,
              userId = response.user.id,
              login = response.user.login,
              email = response.user.email,
              name = response.user.name,
              role = response.user.role.toString
            )
          )
        case Left(error) =>
          Left(ErrorResponse("LOGIN_ERROR", error))
      }
    }
  }

  // POST /auth/refresh - Token refresh
  val refreshEndpoint = baseEndpoint.post
    .in("refresh")
    .in(jsonBody[RefreshTokenDto])
    .out(jsonBody[AuthResponseDto])
    .description("Refresh access token")

  def refreshRoute = refreshEndpoint.serverLogic { dto =>
    legacyGuard {
      authService.refreshAccessToken(dto.refreshToken).map {
        case Right(response) =>
          Right(
            AuthResponseDto(
              accessToken = response.accessToken,
              refreshToken = response.refreshToken,
              expiresAt = response.expiresAt,
              userId = response.user.id,
              login = response.user.login,
              email = response.user.email,
              name = response.user.name,
              role = response.user.role.toString
            )
          )
        case Left(error) =>
          Left(ErrorResponse("REFRESH_ERROR", error))
      }
    }
  }

  // POST /auth/logout - Logout
  val logoutEndpoint = baseEndpoint.post
    .in("logout")
    .in(jsonBody[LogoutDto])
    .out(stringBody)
    .description("Logout (invalidate refresh token)")

  def logoutRoute = logoutEndpoint.serverLogic { dto =>
    legacyGuard {
      // TODO: extract userId from JWT access token
      val userId = UUID.randomUUID() // Placeholder

      authService.logout(dto.refreshToken, userId, "0.0.0.0", "unknown").map { success =>
        if success then Right("Logged out successfully")
        else Left(ErrorResponse("LOGOUT_ERROR", "Failed to logout"))
      }
    }
  }

  val routes = List(
    registerRoute,
    loginRoute,
    refreshRoute,
    logoutRoute
  )
