package com.consultant.core.domain

import java.util.UUID
import types.*

case class Country(
  id: CountryId,
  code: String,     // e.g., "+001", "+380"
  name: String,     // e.g., "United States", "Ukraine"
  shortname: String // e.g., "USA", "Uk"
)

object Country {
  def apply(code: String, name: String, shortname: String): Country =
    Country(UUID.randomUUID(), code, name, shortname)
}

type CountryId = UUID
