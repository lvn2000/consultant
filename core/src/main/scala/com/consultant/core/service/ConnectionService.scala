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
