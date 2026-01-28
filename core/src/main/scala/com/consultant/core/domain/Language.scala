package com.consultant.core.domain

import java.util.UUID
import types.*

case class Language(
  id: LanguageId,
  code: String, // e.g., "en", "ru"
  name: String  // e.g., "English", "Russian"
)

object Language {
  def apply(code: String, name: String): Language =
    Language(UUID.randomUUID(), code, name)
}

type LanguageId = UUID
