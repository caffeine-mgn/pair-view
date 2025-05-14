package pw.binom.tools

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import pw.binom.Description
import pw.binom.dto.ControlRequestDto
import pw.binom.dto.ControlResponseDto
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds

class PlayVideoTool : AbstractGlassesTool<PlayVideoTool.Function, ControlResponseDto.OK>() {
    override val function: KSerializer<Function>
        get() = Function.serializer()

    override val validResponse: KClass<ControlResponseDto.OK>
        get() = ControlResponseDto.OK::class

    override suspend fun generateRequest(arguments: Function): ControlRequestDto =
        ControlRequestDto.OpenVideoFile(
            fileName = arguments.fileName,
            time = 0.seconds,
        )

    @Serializable
    @Description("Open and play video file on device")
    @SerialName("open_and_play_video_file")
    class Function(
        @Description("Name of file for play")
        @JsonNames("filename", "file_name", "file_path", "path")
        val fileName: String,
    )
}