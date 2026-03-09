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
package com.consultant.core.validation

import com.consultant.core.domain.DomainError
import ValidationResult.*

/**
 * Core validation utilities and DSL.
 *
 * Provides a fluent API for validating data with composable validation rules.
 */
object Validator:

  /** Validates a value and returns a ValidationResult */
  def validate[A](value: A): ValidationBuilder[A] = ValidationBuilder(value)

  /** Validates a condition and returns a ValidationResult */
  def check(condition: Boolean, error: => DomainError): ValidationResult =
    if condition then Valid else Invalid(List(error))

  /** Validates that a string is not empty */
  def nonEmpty(value: String, fieldName: String): ValidationResult =
    check(value.trim.nonEmpty, DomainError.ValidationError(s"$fieldName cannot be empty"))

  /** Validates that a string matches an email pattern */
  def isValidEmail(email: String): Boolean =
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".r
    emailRegex.matches(email)

  /** Validates that a string is a valid phone number */
  def isValidPhone(phone: String): Boolean =
    val digitsOnly = phone.replaceAll("\\D", "")
    digitsOnly.length >= 10 && digitsOnly.length <= 15

  /** Validates that a value is positive */
  def positive(value: BigDecimal, fieldName: String): ValidationResult =
    check(value > 0, DomainError.InvalidPrice(value))

  /** Validates that a value is non-negative */
  def nonNegative(value: Int, fieldName: String): ValidationResult =
    check(value >= 0, DomainError.ValidationError(s"$fieldName must be non-negative"))

  /** Validates that a value is within a range */
  def inRange(value: Int, min: Int, max: Int, fieldName: String): ValidationResult =
    check(
      value >= min && value <= max,
      DomainError.ValidationError(s"$fieldName must be between $min and $max")
    )

  /** Validates that a string has a minimum length */
  def minLength(value: String, min: Int, fieldName: String): ValidationResult =
    check(
      value.length >= min,
      DomainError.ValidationError(s"$fieldName must be at least $min characters")
    )

  /** Validates that a string has a maximum length */
  def maxLength(value: String, max: Int, fieldName: String): ValidationResult =
    check(
      value.length <= max,
      DomainError.ValidationError(s"$fieldName must be at most $max characters")
    )

  /** Combines multiple validation results into one */
  def all(results: ValidationResult*): ValidationResult =
    results.foldLeft[ValidationResult](Valid)(_ ++ _)

  /** Builder class for fluent validation API */
  case class ValidationBuilder[A](value: A):

    /** Validates that the value satisfies a predicate */
    def must(predicate: A => Boolean, error: => DomainError): ValidationResult =
      check(predicate(value), error)

    /** Validates that the value does not satisfy a predicate */
    def mustNot(predicate: A => Boolean, error: => DomainError): ValidationResult =
      check(!predicate(value), error)

    /** Validates a field of the value */
    def field[B](extractor: A => B)(validation: B => ValidationResult): ValidationResult =
      validation(extractor(value))

    /** Validates an optional field */
    def optionalField[B](extractor: A => Option[B])(validation: B => ValidationResult): ValidationResult =
      extractor(value) match
        case Some(b) => validation(b)
        case None    => Valid

  end ValidationBuilder

end Validator
