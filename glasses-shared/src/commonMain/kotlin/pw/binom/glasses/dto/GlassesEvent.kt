package pw.binom.glasses.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlin.time.Duration

@Serializable
@JsonClassDiscriminator("type")
sealed interface GlassesEvent {
    @Serializable
    @SerialName("completed")
    object Completed : GlassesEvent

    @Serializable
    @SerialName("play")
    data class Play(val time: Duration) : GlassesEvent

    @Serializable
    @SerialName("pause")
    data class Pause(val time: Duration) : GlassesEvent

    @Serializable
    @SerialName("seek")
    data class Seek(val time: Duration) : GlassesEvent

    @Serializable
    @SerialName("finished")
    data object Finished : GlassesEvent

    @Serializable
    @SerialName("open")
    data class Open(val file: String) : GlassesEvent
}