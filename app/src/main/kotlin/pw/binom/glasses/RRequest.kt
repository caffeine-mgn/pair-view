package pw.binom.glasses

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
@SuppressLint("UnsafeOptInUsageError")
sealed interface RRequest {

    @Serializable
    data class OpenVideoFile(
        val fileName: String,
        val time: Duration? = null,
    ) : RRequest

    @Serializable
    data class Seek(val time: Duration) : RRequest

    @Serializable
    data class SeekDelta(val time: Duration) : RRequest

    @Serializable
    data class Play(val time: Duration?) : RRequest

    @Serializable
    data class Pause(val time: Duration?) : RRequest

    @Serializable
    data object GetState : RRequest

    @Serializable
    data object GetDeviceInfo : RRequest
}