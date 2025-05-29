package pw.binom.glasses.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlin.time.Duration

@Serializable
@JsonClassDiscriminator("type")
sealed interface GlassesEvent {

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

    @Serializable
    @SerialName("intention_pause")
    data class IntentionPause(val state: GlassesResponse.State) : GlassesEvent

    @Serializable
    @SerialName("intention_play")
    data class IntentionPlay(val state: GlassesResponse.State) : GlassesEvent

    @Serializable
    @SerialName("intention_seek_next")
    data class IntentionSeekNext(val state: GlassesResponse.State) : GlassesEvent

    @Serializable
    @SerialName("intention_seek_back")
    data class IntentionSeekBack(val state: GlassesResponse.State) : GlassesEvent
}