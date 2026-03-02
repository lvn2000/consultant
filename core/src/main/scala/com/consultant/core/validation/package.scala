package com.consultant.core

/**
 * Validation layer for domain objects.
 *
 * This package provides a DSL for validating domain requests and entities with composable validation rules.
 *
 * Example usage: {{ import com.consultant.core.validation.* import com.consultant.core.validation.Validator.*
 *
 * val result = UserValidator.validateCreate(request) result.toEither match case Left(error) => // handle error case
 * Right(_) => // validation passed }}
 */
package object validation
