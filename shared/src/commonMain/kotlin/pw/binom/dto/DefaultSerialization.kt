package pw.binom.dto

import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf

object DefaultSerialization {
    val json = Json
    val protobuf = ProtoBuf
}