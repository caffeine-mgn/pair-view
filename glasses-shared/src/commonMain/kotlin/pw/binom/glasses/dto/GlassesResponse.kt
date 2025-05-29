package pw.binom.glasses.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlin.time.Duration

@Serializable
@JsonClassDiscriminator("type")
sealed interface GlassesResponse {
    @Serializable
    @SerialName("local_files")
    data class LocalFiles(val files: List<String>) : GlassesResponse

    @Serializable
    @SerialName("ok")
    data object OK : GlassesResponse

    @Serializable
    @SerialName("state")
    data class State(
        val currentFile: String?,
        val time: Duration,
        val totalTime: Duration,
        val isPlaying: Boolean,
    ) : GlassesResponse
}