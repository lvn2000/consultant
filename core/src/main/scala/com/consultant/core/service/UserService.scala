package com.consultant.core.service

import cats.effect.IO
import cats.syntax.all.*
import com.consultant.core.domain.*
import com.consultant.core.ports.*
import java.util.UUID
import java.time.Instant

class UserService(userRepo: UserRepository):

  def createUser(request: CreateUserRequest): IO[Either[DomainError, User]] =
    for
      existing <- userRepo.findByEmail(request.email)
      result <- existing match
        case Some(_) => IO.pure(Left(DomainError.EmailAlreadyExists(request.email)))
        case None =>
          if !isValidEmail(request.email) then IO.pure(Left(DomainError.InvalidEmail(request.email)))
          else userRepo.create(request).map(Right(_))
    yield result

  def getUser(id: UserId): IO[Either[DomainError, User]] =
    userRepo.findById(id).map {
      case Some(user) => Right(user)
      case None       => Left(DomainError.UserNotFound(id))
    }

  def updateUser(user: User): IO[Either[DomainError, User]] =
    userRepo.update(user).map(Right(_))

  def listUsers(offset: Int, limit: Int): IO[List[User]] =
    userRepo.list(offset, limit)

  private def isValidEmail(email: String): Boolean =
    email.matches("""^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$""")
