package pw.binom.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.time.Duration

@Serializable
sealed interface ControlResponseDto {
    companion object {
        fun fromByteArray(data: ByteArray) =
            ProtoBuf.decodeFromByteArray(ControlResponseDto.serializer(), data)
    }

    fun toByteArray() {
        ProtoBuf.encodeToByteArray(ControlResponseDto.serializer(), this)
    }

    @Serializable
    data object OK : ControlResponseDto

    @Serializable
    data class Error(val message: String) : ControlResponseDto

    @Serializable
    data class Files(val message: List<String>) : ControlResponseDto

    @Serializable
    sealed interface CurrentState : ControlResponseDto {

        @Serializable
        data class Video(
            val fileName: String?,
            val totalDuration: Duration,
            val currentPosition: Duration,
            val isPlaying: Boolean,
        ) : CurrentState

        @Serializable
        data object NoneState : CurrentState
    }

    val isError
        get() = this is Error

    val errorMessage
        get() = (this as? Error)?.message
}