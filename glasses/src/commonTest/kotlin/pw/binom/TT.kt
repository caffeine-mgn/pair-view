package pw.binom

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.test.Test

class TT {
    @Serializable
    sealed interface SSS {
        @Serializable
        object User : SSS

        @Serializable
        data class Agent(val data: String) : SSS
    }

    @Test
    fun aa() {
        val ss: SSS = SSS.Agent("value")
        val serializedJson = Json.encodeToString(SSS.serializer(), ss)
        val serializedProto = ProtoBuf.encodeToByteArray(SSS.serializer(), ss)
        val deserializedProto = ProtoBuf.decodeFromByteArray(SSS.serializer(), serializedProto)
        println("->${deserializedProto}")
        println("serializedJson=$serializedJson")

    }
}