package pw.binom.phone.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlin.time.Duration

@Serializable
@JsonClassDiscriminator("type")
sealed interface PhoneEvent {
    @Serializable
    @SerialName("completed")
    object Completed : PhoneEvent

    @Serializable
    @SerialName("play")
    data class Play(val time: Duration) : PhoneEvent

    @Serializable
    @SerialName("pause")
    data class Pause(val time: Duration) : PhoneEvent

    @Serializable
    @SerialName("seek")
    data class Seek(val time: Duration) : PhoneEvent

    @Serializable
    @SerialName("open")
    data class Open(val file: String) : PhoneEvent
}