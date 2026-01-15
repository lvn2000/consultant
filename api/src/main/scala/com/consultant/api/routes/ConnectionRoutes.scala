package com.consultant.api.routes

import cats.effect.IO
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import com.consultant.api.dto.*
import com.consultant.core.service.ConnectionService
import com.consultant.api.DtoMappers.*
import java.util.UUID
import sttp.tapir.server.http4s.Http4sServerInterpreter
import org.http4s.HttpRoutes

class ConnectionRoutes(connectionService: ConnectionService):

  private val baseEndpoint = endpoint

  // List connection types
  val listConnectionTypesEndpoint = endpoint.get
    .out(jsonBody[List[ConnectionTypeDto]])

  val listConnectionTypes = listConnectionTypesEndpoint.serverLogic { _ =>
    connectionService.getAllConnectionTypes().map { types =>
      Right(types.map(toConnectionTypeDto))
    }
  }

  // Get connection type by ID
  val getConnectionTypeEndpoint = endpoint.get
    .in(path[UUID]("connectionTypeId"))
    .out(jsonBody[ConnectionTypeDto])
    .errorOut(jsonBody[ErrorResponse])

  val getConnectionType = getConnectionTypeEndpoint.serverLogic { id =>
    connectionService.getConnectionType(id).map {
      case Right(connType) => Right(toConnectionTypeDto(connType))
      case Left(error)     => Left(toErrorResponse(error))
    }
  }

  // Add connection for specialist
  val addConnectionEndpoint = baseEndpoint
    .in("specialists")
    .post
    .in(path[UUID]("specialistId"))
    .in("connections")
    .in(jsonBody[CreateConnectionDto])
    .out(jsonBody[SpecialistConnectionDto])
    .errorOut(jsonBody[ErrorResponse])

  val addConnection = addConnectionEndpoint.serverLogic { case (specialistId, dto) =>
    connectionService.addConnection(specialistId, toCreateConnectionRequest(dto)).map {
      case Right(connection) => Right(toSpecialistConnectionDto(connection))
      case Left(error)       => Left(toErrorResponse(error))
    }
  }

  // Get specialist's connections
  val getSpecialistConnectionsEndpoint = baseEndpoint
    .in("specialists")
    .get
    .in(path[UUID]("specialistId"))
    .in("connections")
    .out(jsonBody[List[SpecialistConnectionDto]])

  val getSpecialistConnections = getSpecialistConnectionsEndpoint.serverLogic { specialistId =>
    connectionService.getSpecialistConnections(specialistId).map { connections =>
      Right(connections.map(toSpecialistConnectionDto))
    }
  }

  // Get a specific connection
  val getConnectionEndpoint = baseEndpoint
    .in("specialists")
    .get
    .in(path[UUID]("specialistId"))
    .in("connections" / path[UUID]("connectionId"))
    .out(jsonBody[SpecialistConnectionDto])
    .errorOut(jsonBody[ErrorResponse])

  val getConnection = getConnectionEndpoint.serverLogic { case (_, connectionId) =>
    connectionService.getConnection(connectionId).map {
      case Right(connection) => Right(toSpecialistConnectionDto(connection))
      case Left(error)       => Left(toErrorResponse(error))
    }
  }

  // Update a connection
  val updateConnectionEndpoint = baseEndpoint
    .in("specialists")
    .put
    .in(path[UUID]("specialistId"))
    .in("connections" / path[UUID]("connectionId"))
    .in(jsonBody[UpdateConnectionDto])
    .out(jsonBody[SpecialistConnectionDto])
    .errorOut(jsonBody[ErrorResponse])

  val updateConnection = updateConnectionEndpoint.serverLogic { case (_, connectionId, dto) =>
    connectionService.updateConnection(connectionId, dto.connectionValue).map {
      case Right(connection) => Right(toSpecialistConnectionDto(connection))
      case Left(error)       => Left(toErrorResponse(error))
    }
  }

  // Delete a connection
  val deleteConnectionEndpoint = baseEndpoint
    .in("specialists")
    .delete
    .in(path[UUID]("specialistId"))
    .in("connections" / path[UUID]("connectionId"))
    .errorOut(jsonBody[ErrorResponse])
    .out(emptyOutput)

  val deleteConnection = deleteConnectionEndpoint.serverLogic { case (_, connectionId) =>
    connectionService.removeConnection(connectionId).map {
      case Right(_)    => Right(())
      case Left(error) => Left(toErrorResponse(error))
    }
  }

  val endpoints = List(
    listConnectionTypes,
    getConnectionType,
    addConnection,
    getSpecialistConnections,
    getConnection,
    updateConnection,
    deleteConnection
  )

  val routes: HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(endpoints)
