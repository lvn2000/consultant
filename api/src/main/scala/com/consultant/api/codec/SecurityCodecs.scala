package com.consultant.api.codec

import io.circe.{ Codec, Decoder, Encoder }
import com.consultant.core.domain.security.UserRole

object SecurityCodecs:

  given Codec[UserRole] = Codec.from(
    Decoder[String].emap(s => UserRole.values.find(_.toString == s).toRight(s"Invalid UserRole: $s")),
    Encoder[String].contramap(_.toString)
  )
