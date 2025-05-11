package pw.binom.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import pw.binom.io.AsyncInput
import pw.binom.io.AsyncOutput
import pw.binom.io.readBytes

@Serializable
data class ServerRequest(
    val id: Int,
    val request: Boolean,
    val ok: OK? = null,
    val getLocalFiles: GetLocalFiles? = null,
    val openVideoFile: OpenVideoFile? = null,
    val play: Play? = null,
    val pause: Pause? = null,
    val error: Error? = null,
    val seek: Seek? = null,
    val getState: GetState? = null,
    val updateView: UpdateView? = null,
) {
    companion object {
        fun ok(id: Int) = ServerRequest(id = id, request = false, ok = OK)
        suspend fun read(input: AsyncInput): ServerRequest {
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
    object OK

    @Serializable
    object GetLocalFiles

    @Serializable
    data class OpenVideoFile(val fileName: String, val time: Long)

    @Serializable
    data class Play(val time: Long)

    @Serializable
    data class Pause(val time: Long)

    @Serializable
    data class Error(val message: String)

    @Serializable
    data object GetState

    @Serializable
    data class Seek(val time:Long)

    @Serializable
    data class UpdateView(
        val padding: Int,
        val align: Int,
    )
}