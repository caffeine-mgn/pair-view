package pw.binom.glasses

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pw.binom.glasses.dto.GlassesResponse
import kotlin.time.Duration

@SuppressLint("UnsafeOptInUsageError")
@Serializable
sealed interface REvent {
    @Serializable
    data class Play(val time: Duration) : REvent

    @Serializable
    data class Pause(val time: Duration) : REvent

    @Serializable
    data class Seek(val time: Duration) : REvent

    @Serializable
    data class Open(val file: String) : REvent

    @Serializable
    data object Finished : REvent

    @Serializable
    data class IntentionPause(val state: GlassesResponse.State) : REvent

    @Serializable
    data class IntentionPlay(val state: GlassesResponse.State) : REvent

    @Serializable
    data class IntentionSeekNext(val state: GlassesResponse.State) : REvent

    @Serializable
    data class IntentionSeekBack(val state: GlassesResponse.State) : REvent
}