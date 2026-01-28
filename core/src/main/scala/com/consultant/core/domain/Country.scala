package com.consultant.core.domain

import java.util.UUID
import types.*

case class Country(
  id: CountryId,
  code: String, // e.g., "US", "RU"
  name: String  // e.g., "United States", "Russia"
)

object Country {
  def apply(code: String, name: String): Country =
    Country(UUID.randomUUID(), code, name)
}

type CountryId = UUID
