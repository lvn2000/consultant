package com.consultant.api.dto

import io.circe.{ Decoder, Encoder }
import io.circe.Codec

case class ErrorResponse(
  error: String,
  message: String
) derives Codec.AsObject
