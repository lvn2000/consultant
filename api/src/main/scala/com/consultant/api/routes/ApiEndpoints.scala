/*
 * Copyright (c) 2026 Volodymyr Lubenchenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
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
