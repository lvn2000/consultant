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

  private val baseEndpoint            = endpoint
  private val connectionTypesEndpoint = endpoint
  private val specialistConnectionsEndpoint = endpoint
    .in(path[UUID]("specialistId"))
    .in("connections")

  // List connection types
  val listConnectionTypesEndpoint = connectionTypesEndpoint.get
    .out(jsonBody[List[ConnectionTypeDto]])

  val listConnectionTypes = listConnectionTypesEndpoint.serverLogic { _ =>
    connectionService.getAllConnectionTypes().map { types =>
      Right(types.map(toConnectionTypeDto))
    }
  }

  // Get connection type by ID
  val getConnectionTypeEndpoint = connectionTypesEndpoint.get
    .in(path[UUID]("connectionTypeId"))
    .out(jsonBody[ConnectionTypeDto])
    .errorOut(jsonBody[ErrorResponse])

  val getConnectionType = getConnectionTypeEndpoint.serverLogic { id =>
    connectionService.getConnectionType(id).map {
      case Right(connType) => Right(toConnectionTypeDto(connType))
      case Left(error)     => Left(toErrorResponse(error))
    }
  }

  // Create connection type
  val createConnectionTypeEndpoint = connectionTypesEndpoint.post
    .in(jsonBody[CreateConnectionTypeDto])
    .out(jsonBody[ConnectionTypeDto])
    .errorOut(jsonBody[ErrorResponse])

  val createConnectionType = createConnectionTypeEndpoint.serverLogic { dto =>
    connectionService.createConnectionType(toCreateConnectionTypeRequest(dto)).map {
      case Right(connType) => Right(toConnectionTypeDto(connType))
      case Left(error)     => Left(toErrorResponse(error))
    }
  }

  // Update connection type
  val updateConnectionTypeEndpoint = connectionTypesEndpoint.put
    .in(path[UUID]("connectionTypeId"))
    .in(jsonBody[UpdateConnectionTypeDto])
    .out(jsonBody[ConnectionTypeDto])
    .errorOut(jsonBody[ErrorResponse])

  val updateConnectionType = updateConnectionTypeEndpoint.serverLogic { case (id, dto) =>
    connectionService.updateConnectionType(id, toUpdateConnectionTypeRequest(dto)).map {
      case Right(connType) => Right(toConnectionTypeDto(connType))
      case Left(error)     => Left(toErrorResponse(error))
    }
  }

  // Delete connection type
  val deleteConnectionTypeEndpoint = connectionTypesEndpoint.delete
    .in(path[UUID]("connectionTypeId"))
    .errorOut(jsonBody[ErrorResponse])
    .out(emptyOutput)

  val deleteConnectionType = deleteConnectionTypeEndpoint.serverLogic { id =>
    connectionService.deleteConnectionType(id).map {
      case Right(_)    => Right(())
      case Left(error) => Left(toErrorResponse(error))
    }
  }

  // Add connection for specialist
  val addConnectionEndpoint = specialistConnectionsEndpoint.post
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
  val getSpecialistConnectionsEndpoint = specialistConnectionsEndpoint.get
    .out(jsonBody[List[SpecialistConnectionDto]])

  val getSpecialistConnections = getSpecialistConnectionsEndpoint.serverLogic { specialistId =>
    connectionService.getSpecialistConnections(specialistId).map { connections =>
      Right(connections.map(toSpecialistConnectionDto))
    }
  }

  // Get a specific connection
  val getConnectionEndpoint = specialistConnectionsEndpoint.get
    .in(path[UUID]("connectionId"))
    .out(jsonBody[SpecialistConnectionDto])
    .errorOut(jsonBody[ErrorResponse])

  val getConnection = getConnectionEndpoint.serverLogic { case (_, connectionId) =>
    connectionService.getConnection(connectionId).map {
      case Right(connection) => Right(toSpecialistConnectionDto(connection))
      case Left(error)       => Left(toErrorResponse(error))
    }
  }

  // Update a connection
  val updateConnectionEndpoint = specialistConnectionsEndpoint.put
    .in(path[UUID]("connectionId"))
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
  val deleteConnectionEndpoint = specialistConnectionsEndpoint.delete
    .in(path[UUID]("connectionId"))
    .errorOut(jsonBody[ErrorResponse])
    .out(emptyOutput)

  val deleteConnection = deleteConnectionEndpoint.serverLogic { case (_, connectionId) =>
    connectionService.removeConnection(connectionId).map {
      case Right(_)    => Right(())
      case Left(error) => Left(toErrorResponse(error))
    }
  }

  private val connectionTypeEndpoints = List(
    listConnectionTypes,
    getConnectionType,
    createConnectionType,
    updateConnectionType,
    deleteConnectionType
  )

  private val specialistConnectionEndpoints = List(
    addConnection,
    getSpecialistConnections,
    getConnection,
    updateConnection,
    deleteConnection
  )

  val endpoints = connectionTypeEndpoints ++ specialistConnectionEndpoints

  val connectionTypeRoutes: HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(connectionTypeEndpoints)
  val specialistConnectionRoutes: HttpRoutes[IO] =
    Http4sServerInterpreter[IO]().toRoutes(specialistConnectionEndpoints)
