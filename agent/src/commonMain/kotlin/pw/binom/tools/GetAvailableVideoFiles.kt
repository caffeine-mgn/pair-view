package pw.binom.tools

import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import pw.binom.Description
import pw.binom.dto.ControlRequestDto
import pw.binom.dto.ControlResponseDto
import kotlin.reflect.KClass

class GetAvailableVideoFiles :
    AbstractGlassesTool<GetAvailableVideoFiles.Function, ControlResponseDto.Files>() {
    override val function: KSerializer<Function>
        get() = Function.serializer()
    override val validResponse: KClass<ControlResponseDto.Files>
        get() = ControlResponseDto.Files::class

    override suspend fun generateRequest(arguments: Function): ControlRequestDto =
        ControlRequestDto.GetAvailableVideoFiles

    override suspend fun toLLMResult(resp: ControlResponseDto.Files): JsonElement =
        ok(
            JsonObject(
                mapOf(
                    "files" to JsonArray(resp.message.map { JsonPrimitive(it) })
                )
            )
        )

    @Description("Returns array of available video files")
    @Serializable
    @SerialName("get_available_video_files")
    data object Function
}