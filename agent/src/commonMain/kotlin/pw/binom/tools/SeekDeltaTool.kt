package pw.binom.tools

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNames
import pw.binom.Description
import pw.binom.dto.ControlRequestDto
import pw.binom.dto.ControlResponseDto
import pw.binom.dto.Response
import pw.binom.logger.Logger
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class SeekDeltaTool : AbstractGlassesTool<SeekDeltaTool.Function, ControlResponseDto.OK>() {
    override val function: KSerializer<Function>
        get() = Function.serializer()
    override val validResponse: KClass<ControlResponseDto.OK>
        get() = ControlResponseDto.OK::class

    override suspend fun generateRequest(arguments: Function): ControlRequestDto =
        ControlRequestDto.SeekDelta(arguments.position.seconds)

    @Description(
        "Changes the playback time of the current playback.\'" +
                "Changes the playback time relative to the absolute."
    )
    @Serializable
    @SerialName("change_relative_time_for_current_playback")
    data class Function(
        @Description("Playback position. In seconds")
        @JsonNames("time_in_seconds", "position", "seconds")
        @SerialName("time")
        val position: Long,
    )
}