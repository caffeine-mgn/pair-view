package pw.binom.tools

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import pw.binom.Description
import pw.binom.date.DateTime
import pw.binom.date.format.toDatePattern

class CurrentTimeTool : AbstractAITool<CurrentTimeTool.Function>() {
    private val formatter = "yyyy-MM-dd HH:mm:ss".toDatePattern()
    override val function: KSerializer<Function>
        get() = Function.serializer()

    override suspend fun execute(
        chatId: Long,
        name: String,
        arguments: Function,
    ) = JsonObject(
        mapOf(
            "result" to JsonPrimitive(formatter.toString(DateTime.now))
        )
    )

    @Serializable
    @Description(
        "Returns current time in format yyyy-MM-dd HH:mm:ss\n\n" +
                "For example {\"result\": \"2025-02-12 14:37:17\"}"
    )
    object Function
}