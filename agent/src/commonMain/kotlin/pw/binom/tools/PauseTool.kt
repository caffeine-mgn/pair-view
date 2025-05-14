package pw.binom.tools

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import pw.binom.Description
import pw.binom.dto.ControlRequestDto
import pw.binom.dto.ControlResponseDto
import kotlin.reflect.KClass

class PauseTool : AbstractGlassesTool<PauseTool.Function, ControlResponseDto.OK>() {
    override val function: KSerializer<Function>
        get() = Function.serializer()
    override val validResponse: KClass<ControlResponseDto.OK>
        get() = ControlResponseDto.OK::class

    override suspend fun generateRequest(arguments: Function): ControlRequestDto =
        ControlRequestDto.Pause(
            time = null,
        )

    @Description("Pause current video")
    @Serializable
    @SerialName("pause")
    class Function
}