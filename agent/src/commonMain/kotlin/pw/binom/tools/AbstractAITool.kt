package pw.binom.tools

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import pw.binom.AITool

abstract class AbstractAITool<T> : AITool<T> {
    protected fun ok(element: JsonElement) = JsonObject(
        mapOf(
            "status" to JsonPrimitive("success"),
            "data" to element
        )
    )

    protected fun error(message: String) = JsonObject(
        mapOf(
            "status" to JsonPrimitive("failure"),
            "message" to JsonPrimitive(message)
        )
    )

    protected fun ok() = JsonObject(
        mapOf(
            "status" to JsonPrimitive("success")
        )
    )
}