package pw.binom.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.json.Json

@Serializable
@SerialName(NatsConst.TYPE_ERROR)
data class NatsErrorDto(val msg: String) {
    companion object {
        fun fromJson(json: String) = Json.decodeFromString(serializer(), json)
    }

    val asJson
        get() = Json.encodeToString(serializer(), this)
}