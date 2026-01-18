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
        case None    => IO.pure(Left(DomainError.ValidationError("Connection type not found")))
        case Some(_) =>
          // Check if specialist already has this connection type
          for
            existingOpt <- connectionRepo.findBySpecialistAndType(specialistId, request.connectionTypeId)
            result <- existingOpt match
              case Some(_) => IO.pure(Left(DomainError.ValidationError("Specialist already has this connection type")))
              case None    => connectionRepo.create(specialistId, request).map(Right(_))
          yield result
    yield result

  // Get all connections for a specialist
  def getSpecialistConnections(specialistId: SpecialistId): IO[List[SpecialistConnection]] =
    connectionRepo.findBySpecialist(specialistId)

  // Get a specific connection
  def getConnection(id: UUID): IO[Either[DomainError, SpecialistConnection]] =
    connectionRepo.findById(id).map {
      case Some(connection) => Right(connection)
      case None             => Left(DomainError.ValidationError(s"Connection not found: $id"))
    }

  // Update a connection
  def updateConnection(
    id: UUID,
    newValue: String
  ): IO[Either[DomainError, SpecialistConnection]] =
    for
      connectionOpt <- connectionRepo.findById(id)
      result <- connectionOpt match
        case None => IO.pure(Left(DomainError.ValidationError(s"Connection not found: $id")))
        case Some(connection) =>
          val updated = connection.copy(connectionValue = newValue)
          connectionRepo.update(updated).map(Right(_))
    yield result

  // Remove a connection
  def removeConnection(id: UUID): IO[Either[DomainError, Unit]] =
    connectionRepo.findById(id).flatMap {
      case None    => IO.pure(Left(DomainError.ValidationError(s"Connection not found: $id")))
      case Some(_) => connectionRepo.delete(id).map(Right(_))
    }

  // Get all connection types
  def getAllConnectionTypes(): IO[List[ConnectionType]] =
    connectionTypeRepo.listAll()

  // Get a specific connection type
  def getConnectionType(id: ConnectionTypeId): IO[Either[DomainError, ConnectionType]] =
    connectionTypeRepo.findById(id).map {
      case Some(connType) => Right(connType)
      case None           => Left(DomainError.ValidationError(s"Connection type not found: $id"))
    }

  // Create a connection type
  def createConnectionType(
    request: CreateConnectionTypeRequest
  ): IO[Either[DomainError, ConnectionType]] =
    connectionTypeRepo.findByName(request.name).flatMap {
      case Some(_) =>
        IO.pure(Left(DomainError.ValidationError("Connection type already exists")))
      case None =>
        connectionTypeRepo.create(request).map(Right(_))
    }

  // Update a connection type
  def updateConnectionType(
    id: ConnectionTypeId,
    request: UpdateConnectionTypeRequest
  ): IO[Either[DomainError, ConnectionType]] =
    connectionTypeRepo.findById(id).flatMap {
      case None => IO.pure(Left(DomainError.ValidationError(s"Connection type not found: $id")))
      case Some(existing) =>
        connectionTypeRepo.findByName(request.name).flatMap {
          case Some(duplicate) if duplicate.id != id =>
            IO.pure(Left(DomainError.ValidationError("Connection type name already exists")))
          case _ =>
            val updated = existing.copy(name = request.name, description = request.description)
            connectionTypeRepo.update(updated).map(Right(_))
        }
    }

  // Delete a connection type
  def deleteConnectionType(id: ConnectionTypeId): IO[Either[DomainError, Unit]] =
    connectionTypeRepo.findById(id).flatMap {
      case None    => IO.pure(Left(DomainError.ValidationError(s"Connection type not found: $id")))
      case Some(_) => connectionTypeRepo.delete(id).map(Right(_))
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
      case None             => Left(DomainError.ValidationError(s"Connection not found: $id"))
    }

  def updateClientConnection(
    id: UUID,
    newValue: String
  ): IO[Either[DomainError, ClientConnection]] =
    for
      connectionOpt <- connectionRepo.findClientConnectionById(id)
      result <- connectionOpt match
        case None => IO.pure(Left(DomainError.ValidationError(s"Connection not found: $id")))
        case Some(connection) =>
          val updated = connection.copy(connectionValue = newValue)
          connectionRepo.updateClientConnection(updated).map(Right(_))
    yield result

  def removeClientConnection(id: UUID): IO[Either[DomainError, Unit]] =
    connectionRepo.findClientConnectionById(id).flatMap {
      case None    => IO.pure(Left(DomainError.ValidationError(s"Connection not found: $id")))
      case Some(_) => connectionRepo.deleteClientConnection(id).map(Right(_))
    }
