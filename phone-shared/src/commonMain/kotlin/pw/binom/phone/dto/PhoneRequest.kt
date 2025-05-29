package pw.binom.phone.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlin.time.Duration

@Serializable
@JsonClassDiscriminator("type")
sealed interface PhoneRequest {
    @Serializable
    @SerialName("get_local_files")
    data object GetLocalFiles : PhoneRequest

    @Serializable
    @SerialName("get_state")
    data object GetState : PhoneRequest

    @Serializable
    @SerialName("open")
    data class Open(val fileName: String) : PhoneRequest

    @Serializable
    @SerialName("play")
    data class Play(val time: Duration? = null) : PhoneRequest

    @Serializable
    @SerialName("pause")
    data class Pause(val time: Duration? = null) : PhoneRequest

    @Serializable
    @SerialName("seek")
    data class Seek(val time: Duration) : PhoneRequest

    @Serializable
    @SerialName("seek_delta")
    data class SeekDelta(val time: Duration) : PhoneRequest
}