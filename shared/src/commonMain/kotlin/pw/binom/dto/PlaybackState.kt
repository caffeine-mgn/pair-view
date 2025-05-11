package pw.binom.dto

import kotlinx.serialization.Serializable

@Serializable
data class PlaybackState(
    val videoFile: String?,
    val playing: Boolean,
    val time: Long,
)