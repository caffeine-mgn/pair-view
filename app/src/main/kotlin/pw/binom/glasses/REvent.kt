package pw.binom.glasses

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable
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
}