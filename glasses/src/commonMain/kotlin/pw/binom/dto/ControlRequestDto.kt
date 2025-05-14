package pw.binom.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.time.Duration

@Serializable
sealed interface ControlRequestDto {

    companion object {
        fun fromByteArray(data: ByteArray) =
            ProtoBuf.decodeFromByteArray(ControlRequestDto.serializer(), data)
    }

    fun toByteArray() {
        ProtoBuf.encodeToByteArray(ControlRequestDto.serializer(), this)
    }

    @Serializable
    data class OpenVideoFile(
        val fileName: String,
        val time: Duration,
    ) : ControlRequestDto

    @Serializable
    data object CloseCurrentView : ControlRequestDto

    @Serializable
    data object GetState : ControlRequestDto

    @Serializable
    data class Pause(val time: Duration?) : ControlRequestDto

    @Serializable
    data class Play(val time: Duration?) : ControlRequestDto

    @Serializable
    data class Seek(val time: Duration) : ControlRequestDto

    @Serializable
    data class SeekDelta(val time: Duration) : ControlRequestDto

    @Serializable
    data object GetAvailableVideoFiles : ControlRequestDto
}