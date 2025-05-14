package pw.binom.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.protobuf.ProtoBuf
import pw.binom.io.AsyncInput
import pw.binom.io.AsyncOutput
import pw.binom.io.readBytes

@Serializable
data class ClientRequest(
    val id: Int,
    val request: Boolean,
    val ok: OK? = null,
    val localFiles: LocalFiles? = null,
    val playbackVideo: PlaybackVideo? = null,
    val error: Error? = null,
    val state: State? = null,
) {
    companion object {
        fun ok(id: Int) = ClientRequest(id = id, request = false, ok = OK)
        suspend fun read(input: AsyncInput): ClientRequest {
            val size = input.readInt()
            return ProtoBuf.decodeFromByteArray(serializer(), input.readBytes(size))
        }
    }

    suspend fun write(output: AsyncOutput) {
        val bytes = ProtoBuf.encodeToByteArray(serializer(), this)
        output.writeInt(bytes.size)
        output.write(bytes)
    }

    @Serializable
    data object OK

    @Serializable
    data class LocalFiles(val list: List<String>)

    @Serializable
    data class PlaybackVideo(val time: Long)

    @Serializable
    data class Error(val msg: String)

    @Serializable
    data class State(
        val videoFile: String?,
        val playing: Boolean,
        val time: Long,
    )
}