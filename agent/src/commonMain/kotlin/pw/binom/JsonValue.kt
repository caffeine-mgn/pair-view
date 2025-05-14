package pw.binom

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

@JvmInline
@Serializable(with = JsonValue.Serializer::class)
value class JsonValue<T : Any>(val json: String) {

    class Serializer<T : Any>(valueSerializer: KSerializer<T>) : KSerializer<JsonValue<T>> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
            serialName = "JsonValue",
            kind = PrimitiveKind.STRING,
        )

        override fun serialize(
            encoder: Encoder,
            value: JsonValue<T>,
        ) {
            encoder.encodeString(value.json)
        }

        override fun deserialize(decoder: Decoder): JsonValue<T> =
            JsonValue<T>(decoder.decodeString())
    }

    companion object {
        @OptIn(InternalSerializationApi::class)
        inline fun <reified T : Any> create(value: T) =
            JsonValue<T>(Json.encodeToString(T::class.serializer(), value))
    }
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> JsonValue<T>.get() =
    Json.decodeFromString(T::class.serializer(), json)