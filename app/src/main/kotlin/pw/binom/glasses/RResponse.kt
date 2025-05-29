package pw.binom.glasses

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@SuppressLint("UnsafeOptInUsageError")
@Serializable
sealed interface RResponse {
    @Serializable
    data object OK : RResponse

    @Serializable
    data class DeviceInfo(
        val batteryLevel: Int,
    ) : RResponse

    @Serializable
    data class State(
        val file: String?,
        val totalDuration: Duration,
        val currentTime: Duration,
        val isPlaying: Boolean,
    ) : RResponse
}