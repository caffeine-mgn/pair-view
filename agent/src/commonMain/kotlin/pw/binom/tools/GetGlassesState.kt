package pw.binom.tools

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import pw.binom.Description
import pw.binom.JsonValue
import pw.binom.dto.ControlRequestDto
import pw.binom.dto.ControlResponseDto
import pw.binom.dto.PlaybackState
import pw.binom.dto.Response
import kotlin.reflect.KClass
import kotlin.time.Duration

class GetGlassesState :
    AbstractGlassesTool<GetGlassesState.Function, ControlResponseDto.CurrentState>() {
    override val function: KSerializer<Function>
        get() = Function.serializer()

    override val validResponse: KClass<ControlResponseDto.CurrentState>
        get() = ControlResponseDto.CurrentState::class

    override suspend fun generateRequest(arguments: Function): ControlRequestDto =
        ControlRequestDto.GetState

    @Serializable
    private data class LLMVideoStatus(
        @SerialName("file_name")
        val fileName: String?,
        @SerialName("total_duration_in_seconds")
        val totalDurationInSeconds: Long,
        @SerialName("current_time_in_Seconds")
        val currentTimeInSeconds: Long,
        @SerialName("is_playing")
        val isPlaying: Boolean,
    )

    override suspend fun toLLMResult(resp: ControlResponseDto.CurrentState): JsonElement =
        when (resp) {
            is ControlResponseDto.CurrentState.Video -> {
                ok(
                    JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("video"),
                            "data" to Json.encodeToJsonElement(
                                LLMVideoStatus.serializer(), LLMVideoStatus(
                                    fileName = resp.fileName,
                                    totalDurationInSeconds = resp.totalDuration.inWholeSeconds,
                                    currentTimeInSeconds = resp.currentPosition.inWholeSeconds,
                                    isPlaying = resp.isPlaying,
                                )
                            )
                        )
                    )
                )
            }

            ControlResponseDto.CurrentState.NoneState -> ok(
                JsonObject(
                    mapOf(
                        "type" to JsonPrimitive("none")
                    )
                )
            )
        }


    @Description(
        "Returns current state of device:\n" +
                "State with type \"video\":\n" +
                "device playing some video file\n" +
                "\n" +
                "Available data:\n" +
                "- name of current video file\n" +
                "- current position in seconds\n" +
                "- total duration of current file in seconds\n" +
                "- playing state\n" +
                "\n\n" +
                "State with type \"none\":\n" +
                "Device do nothing. Await any command"
    )
    @Serializable
    @SerialName("get_current_glasses_state")
    data object Function
}