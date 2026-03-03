package com.consultant.api.routes

import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import sttp.model.StatusCode
import com.consultant.api.dto.ErrorResponse

/**
 * Standardized API endpoint definitions.
 *
 * Provides a consistent base for all API endpoints with proper error handling and documentation.
 *
 * Note: Authentication and authorization are handled at the HTTP middleware level (TokenAuthMiddleware), not at the
 * Tapir endpoint level. The securedEndpoint and adminEndpoint methods only add descriptive text to document the
 * intended security requirements.
 */
object ApiEndpoints:

  /**
   * Creates a base endpoint with standard error handling.
   *
   * Note: The /api prefix is added by the Router in RoutesModule, not here.
   *
   * @param name
   *   The endpoint name for documentation
   * @param description
   *   The endpoint description
   * @return
   *   A configured endpoint builder
   */
  def baseEndpoint(name: String, description: String): Endpoint[Unit, Unit, ErrorResponse, Unit, Any] =
    endpoint
      .name(name)
      .description(description)
      .errorOut(
        oneOf[ErrorResponse](
          oneOfVariant(statusCode(StatusCode.BadRequest).and(jsonBody[ErrorResponse])),
          oneOfVariant(statusCode(StatusCode.Unauthorized).and(jsonBody[ErrorResponse])),
          oneOfVariant(statusCode(StatusCode.Forbidden).and(jsonBody[ErrorResponse])),
          oneOfVariant(statusCode(StatusCode.NotFound).and(jsonBody[ErrorResponse])),
          oneOfVariant(statusCode(StatusCode.Conflict).and(jsonBody[ErrorResponse])),
          oneOfVariant(statusCode(StatusCode.InternalServerError).and(jsonBody[ErrorResponse]))
        )
      )

  /**
   * Creates a public endpoint (no authentication required).
   */
  def publicEndpoint(name: String, description: String): Endpoint[Unit, Unit, ErrorResponse, Unit, Any] =
    baseEndpoint(name, description)

  /**
   * Creates a secured endpoint (authentication required).
   *
   * NOTE: Actual authentication enforcement happens at the middleware level (TokenAuthMiddleware). This method only
   * adds descriptive text to the endpoint documentation.
   */
  def securedEndpoint(name: String, description: String): Endpoint[Unit, Unit, ErrorResponse, Unit, Any] =
    baseEndpoint(name, description)
      .description(s"$description (Requires authentication)")

  /**
   * Creates an admin endpoint (admin role required).
   *
   * NOTE: Actual authorization enforcement happens at the middleware level (TokenAuthMiddleware). This method only adds
   * descriptive text to the endpoint documentation.
   */
  def adminEndpoint(name: String, description: String): Endpoint[Unit, Unit, ErrorResponse, Unit, Any] =
    baseEndpoint(name, description)
      .description(s"$description (Requires admin role)")

end ApiEndpoints
