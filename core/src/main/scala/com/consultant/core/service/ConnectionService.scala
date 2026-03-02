package com.consultant.core.service

import cats.effect.IO
import cats.syntax.all.*
import com.consultant.core.domain.*
import com.consultant.core.ports.*
import java.util.UUID

class ConnectionService(
  connectionRepo: ConnectionRepository,
  connectionTypeRepo: ConnectionTypeRepository,
  specialistRepo: SpecialistRepository
):

  // Add a new connection for a specialist
  def addConnection(
    specialistId: SpecialistId,
    request: CreateConnectionRequest
  ): IO[Either[DomainError, SpecialistConnection]] =
    for
      // Verify connection type exists
      connTypeOpt <- connectionTypeRepo.findById(request.connectionTypeId)
      result <- connTypeOpt match
        case None    => IO.pure(Left(DomainError.ConnectionTypeNotFound(request.connectionTypeId)))
        case Some(_) =>
          // Check if specialist already has this connection type
          for
            existingOpt <- connectionRepo.findBySpecialistAndType(specialistId, request.connectionTypeId)
            result <- existingOpt match
              case Some(_) => IO.pure(Left(DomainError.DuplicateEntry("Specialist already has this connection type")))
              case None    => connectionRepo.create(specialistId, request).map(Right(_)).handleError(parseError)
          yield result
    yield result

  // Get all connections for a specialist
  def getSpecialistConnections(specialistId: SpecialistId): IO[List[SpecialistConnection]] =
    connectionRepo.findBySpecialist(specialistId)

  // Get a specific connection
  def getConnection(id: UUID): IO[Either[DomainError, SpecialistConnection]] =
    connectionRepo.findById(id).map {
      case Some(connection) => Right(connection)
      case None             => Left(DomainError.ConnectionNotFound(id.toString))
    }

  // Update a connection
  def updateConnection(
    id: UUID,
    newValue: String
  ): IO[Either[DomainError, SpecialistConnection]] =
    for
      connectionOpt <- connectionRepo.findById(id)
      result <- connectionOpt match
        case None => IO.pure(Left(DomainError.ConnectionNotFound(id.toString)))
        case Some(connection) =>
          val updated = connection.copy(connectionValue = newValue)
          connectionRepo.update(updated).map(Right(_)).handleError(parseError)
    yield result

  // Remove a connection
  def removeConnection(id: UUID): IO[Either[DomainError, Unit]] =
    connectionRepo.findById(id).flatMap {
      case None    => IO.pure(Left(DomainError.ConnectionNotFound(id.toString)))
      case Some(_) => connectionRepo.delete(id).map(Right(_)).handleError(parseError)
    }

  // Get all connection types
  def getAllConnectionTypes(): IO[List[ConnectionType]] =
    connectionTypeRepo.listAll()

  // Get a specific connection type
  def getConnectionType(id: ConnectionTypeId): IO[Either[DomainError, ConnectionType]] =
    connectionTypeRepo.findById(id).map {
      case Some(connType) => Right(connType)
      case None           => Left(DomainError.ConnectionTypeNotFound(id))
    }

  // Create a connection type
  def createConnectionType(
    request: CreateConnectionTypeRequest
  ): IO[Either[DomainError, ConnectionType]] =
    connectionTypeRepo.findByName(request.name).flatMap {
      case Some(_) =>
        IO.pure(Left(DomainError.DuplicateEntry("Connection type already exists")))
      case None =>
        connectionTypeRepo.create(request).map(Right(_)).handleError(parseError)
    }

  // Update a connection type
  def updateConnectionType(
    id: ConnectionTypeId,
    request: UpdateConnectionTypeRequest
  ): IO[Either[DomainError, ConnectionType]] =
    connectionTypeRepo.findById(id).flatMap {
      case None => IO.pure(Left(DomainError.ConnectionTypeNotFound(id)))
      case Some(existing) =>
        connectionTypeRepo.findByName(request.name).flatMap {
          case Some(duplicate) if duplicate.id != id =>
            IO.pure(Left(DomainError.DuplicateEntry("Connection type name already exists")))
          case _ =>
            val updated = existing.copy(name = request.name, description = request.description)
            connectionTypeRepo.update(updated).map(Right(_)).handleError(parseError)
        }
    }

  // Delete a connection type
  def deleteConnectionType(id: ConnectionTypeId): IO[Either[DomainError, Unit]] =
    connectionTypeRepo.findById(id).flatMap {
      case None    => IO.pure(Left(DomainError.ConnectionTypeNotFound(id)))
      case Some(_) => connectionTypeRepo.delete(id).map(Right(_)).handleError(parseError)
    }

  // Client connections methods
  def addClientConnection(
    userId: UserId,
    request: CreateConnectionRequest
  ): IO[Either[DomainError, ClientConnection]] =
    for
      // Verify connection type exists
      connTypeOpt <- connectionTypeRepo.findById(request.connectionTypeId)
      result <- connTypeOpt match
        case None    => IO.pure(Left(DomainError.ValidationError("Connection type not found")))
        case Some(_) =>
          // Check if client already has this connection type
          for
            existingOpt <- connectionRepo.findClientConnectionByUserAndType(userId, request.connectionTypeId)
            result <- existingOpt match
              case Some(_) => IO.pure(Left(DomainError.ValidationError("Client already has this connection type")))
              case None    => connectionRepo.createClientConnection(userId, request).map(Right(_))
          yield result
    yield result

  def getClientConnections(userId: UserId): IO[List[ClientConnection]] =
    connectionRepo.findClientConnectionsByUser(userId)

  def getClientConnection(id: UUID): IO[Either[DomainError, ClientConnection]] =
    connectionRepo.findClientConnectionById(id).map {
      case Some(connection) => Right(connection)
      case None             => Left(DomainError.ConnectionNotFound(id.toString))
    }

  def updateClientConnection(
    id: UUID,
    newValue: String
  ): IO[Either[DomainError, ClientConnection]] =
    for
      connectionOpt <- connectionRepo.findClientConnectionById(id)
      result <- connectionOpt match
        case None => IO.pure(Left(DomainError.ConnectionNotFound(id.toString)))
        case Some(connection) =>
          val updated = connection.copy(connectionValue = newValue)
          connectionRepo.updateClientConnection(updated).map(Right(_)).handleError(parseError)
    yield result

  def removeClientConnection(id: UUID): IO[Either[DomainError, Unit]] =
    connectionRepo.findClientConnectionById(id).flatMap {
      case None    => IO.pure(Left(DomainError.ConnectionNotFound(id.toString)))
      case Some(_) => connectionRepo.deleteClientConnection(id).map(Right(_)).handleError(parseError)
    }

  /** Parses database errors into structured domain errors */
  private def parseError(error: Throwable): Either[DomainError, Nothing] =
    error match
      case ex if isPostgresException(ex) =>
        val sqlState = getSqlState(ex)
        val message  = ex.getMessage

        sqlState match
          case Some("23505") => // unique_violation
            Left(DomainError.DuplicateEntry(s"Connection already exists: $message"))
          case Some("23503") => // foreign_key_violation
            Left(DomainError.ReferencedRecordNotFound("Referenced record not found"))
          case Some(code) =>
            Left(DomainError.DatabaseError(s"Database error [$code]: $message"))
          case None =>
            Left(DomainError.DatabaseError(s"Database error: $message"))

      case ex =>
        Left(DomainError.UnexpectedError(ex.getMessage))

  /** Checks if exception is a PostgreSQL PSQLException using reflection */
  private def isPostgresException(ex: Throwable): Boolean =
    ex.getClass.getName == "org.postgresql.util.PSQLException"

  /** Gets SQLState from PostgreSQL exception using reflection */
  private def getSqlState(ex: Throwable): Option[String] =
    try
      val method = ex.getClass.getMethod("getSQLState")
      Option(method.invoke(ex).asInstanceOf[String])
    catch case _: Exception => None
