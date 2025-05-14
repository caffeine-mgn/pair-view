package pw.binom.dto

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
@SuppressLint("UnsafeOptInUsageError")
sealed interface RRequest {

    @Serializable
    data class OpenVideoFile(
        val fileName: String,
        val time: Duration,
    ) : RRequest

    @Serializable
    data class Seek(val time: Duration) : RRequest

    @Serializable
    data class SeekDelta(val time: Duration) : RRequest

    @Serializable
    data class Play(val time: Duration?) : RRequest

    @Serializable
    data class Pause(val time: Duration?) : RRequest
}