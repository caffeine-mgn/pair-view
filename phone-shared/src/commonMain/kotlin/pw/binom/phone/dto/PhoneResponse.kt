package pw.binom.phone.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlin.time.Duration

@Serializable
@JsonClassDiscriminator("type")
sealed interface PhoneResponse {

    @Serializable
    @SerialName("local_files")
    data class LocalFiles(val files: List<String>) : PhoneResponse

    @Serializable
    @SerialName("ok")
    data object OK : PhoneResponse

    @Serializable
    @SerialName("state")
    data class State(
        val currentFile: String?,
        val time: Duration,
        val totalTime: Duration,
        val isPlaying: Boolean,
    ) : PhoneResponse
}