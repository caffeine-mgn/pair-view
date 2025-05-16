package pw.binom.tools

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import pw.binom.Description
import pw.binom.dto.ControlRequestDto
import pw.binom.dto.ControlResponseDto
import pw.binom.dto.Response
import pw.binom.logger.Logger
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class DeviceInfoTool :
    AbstractGlassesTool<DeviceInfoTool.Function, ControlResponseDto.DeviceInfo>() {
    override val function: KSerializer<Function>
        get() = Function.serializer()
    override val validResponse
        get() = ControlResponseDto.DeviceInfo::class

    override suspend fun generateRequest(arguments: Function): ControlRequestDto =
        ControlRequestDto.GetDeviceInfo

    override suspend fun toLLMResult(resp: ControlResponseDto.DeviceInfo): JsonElement =
        ok(JsonObject(mapOf("battery_level_in_percent" to JsonPrimitive(resp.batteryLevel))))

    @Description(
        "Returns device info\n" +
                "Info:\n" +
                "- battery level in percent"
    )
    @Serializable
    @SerialName("get_device_info")
    data object Function
}