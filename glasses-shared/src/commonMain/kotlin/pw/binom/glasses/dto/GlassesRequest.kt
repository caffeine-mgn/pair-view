package pw.binom.glasses.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlin.time.Duration

@Serializable
@JsonClassDiscriminator("type")
sealed interface GlassesRequest {
    @Serializable
    @SerialName("get_local_files")
    data object GetLocalFiles : GlassesRequest

    @Serializable
    @SerialName("get_state")
    data object GetState : GlassesRequest

    @Serializable
    @SerialName("open")
    data class Open(val fileName: String) : GlassesRequest

    @Serializable
    @SerialName("play")
    data class Play(val time: Duration? = null) : GlassesRequest

    @Serializable
    @SerialName("pause")
    data class Pause(val time: Duration? = null) : GlassesRequest

    @Serializable
    @SerialName("seek")
    data class Seek(val time: Duration) : GlassesRequest

    @Serializable
    @SerialName("seek_delta")
    data class SeekDelta(val time: Duration) : GlassesRequest
}