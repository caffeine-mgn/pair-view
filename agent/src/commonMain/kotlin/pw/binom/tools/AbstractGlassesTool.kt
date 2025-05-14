package pw.binom.tools

import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import pw.binom.DeviceClient
import pw.binom.dto.ControlRequestDto
import pw.binom.dto.ControlResponseDto
import pw.binom.properties.AppProperties
import pw.binom.strong.inject
import pw.binom.strong.properties.injectProperty
import kotlin.reflect.KClass

abstract class AbstractGlassesTool<T, R : ControlResponseDto> : AbstractAITool<T>() {
    protected val deviceClient: DeviceClient by inject()
    protected val config: AppProperties by injectProperty()
    protected abstract val validResponse: KClass<R>
    protected abstract suspend fun generateRequest(arguments: T): ControlRequestDto

    override suspend fun execute(chatId: Long, name: String, arguments: T): JsonElement {
        val result = withTimeoutOrNull(config.deviceCommandTimeout) {
            deviceClient.send(deviceId = "1", request = generateRequest(arguments))
        }
        if (result == null) {
            return error("timeout of executing")
        }
        if (result is ControlResponseDto.Error) {
            return error(result.message)
        }
        if (result == ControlResponseDto.OK) {
            return ok()
        }
        if (!validResponse.isInstance(result)) {
            return error("unexpected response")
        }
        return toLLMResult(result as R)
    }

    protected open suspend fun toLLMResult(resp: R): JsonElement =
        ok(Json.encodeToJsonElement(ControlResponseDto.serializer(), resp))
}