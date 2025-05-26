package pw.binom.llm

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import pw.binom.io.AsyncAppendable

abstract class AbstractOpenApi : LLM {
    protected fun String.encodeJson() =
        Json.Default.encodeToString(
            JsonElement.Companion.serializer(), JsonPrimitive(this)
        )

    protected val LLM.Message.role: String
        get() = when (this) {
            is LLM.Message.Assistant,
            is LLM.Message.AssistantToolCall,
                -> "assistant"

            is LLM.Message.System -> "system"
            is LLM.Message.ToolResult -> "tool"
            is LLM.Message.User -> "user"
        }

    protected suspend fun writeMessage(message: LLM.Message, out: AsyncAppendable) {
        when (message) {
            is LLM.Message.ToolResult -> {
                out.append("{")
                    .append("\"role\":\"tool\",")
                    .append("\"tool_call_id\":\"").append(message.id).append("\",")
                    .append("\"content\":").append(message.content.encodeJson())
                    .append("}")
            }

            is LLM.Message.AssistantToolCall -> {
                out.append("{")
                    .append("\"role\": \"assistant\",")
                    .append("\"tool_calls\":[")
                var first = true
                message.calls.forEach {
                    if (!first) {
                        out.append(",")
                    } else {
                        first = false
                    }
                    out.append("{")
                        .append("\"id\": \"").append(it.id).append("\",")
                        .append("\"type\": \"function\",")
                        .append("\"function\": {")
                        .append("\"name\": \"").append(it.name).append("\",")
                        .append("\"arguments\": ").append(it.arguments.encodeJson())
                        .append("}")
                        .append("}")
                }
                out.append("]}")
            }

            is LLM.Message.Assistant -> {
                out.append("{")
                    .append("\"role\": \"").append(message.role).append("\"")
                    .append(",\"content\":")
                val content = if (message.think.isNullOrBlank()) {
                    message.content
                } else {
                    "<think>${message.think}</think>${message.content}"
                }
                out.append(content.encodeJson())
                out.append("}")
            }

            is LLM.Message.TextContent -> {
                out.append("{")
                    .append("\"role\": \"").append(message.role).append("\"")
                    .append(",\"content\":")
                    .append(message.content.encodeJson())
                    .append("}")
            }
        }
    }

    protected suspend fun makeResponse(
        model: String,
        temperature: Float,
        tools: List<JsonObject>?,
        messages: Flow<LLM.Message>,
        out: AsyncAppendable,
    ) {
        out.append("{")
            .append("\"model\":\"")
            .append(model)
            .append("\"")
        out.append(",\"temperature\":").append(temperature)
        out.append(",\"stream\":false")
        if (tools != null) {
            out.append(",\"tools\":")
                .append(
                    Json.Default.encodeToString(
                        JsonArray.Companion.serializer(),
                        JsonArray(tools)
                    )
                )

        }
        out.append(",\"messages\":[")
        var first = true
        messages.collect {
            if (!first) {
                out.append(",")
            }
            first = false
            writeMessage(it, out)
        }
        out.append("]")
        out.append("}")
    }

    protected fun parseResult(json: String): LLM.Result {
        val elements = Json.Default.parseToJsonElement(json).jsonObject
        val choices = elements["choices"]!!.jsonArray
        val choice = choices[0].jsonObject
        val finishReason = choice["finish_reason"]!!.jsonPrimitive.content
        val msg = choice["message"]!!.jsonObject
        return when (finishReason) {
            "stop" -> {
                var content = msg["content"]!!.jsonPrimitive.content
                var thinking: String? = null
                if (content.startsWith("<think>")) {
                    val index = content.indexOf("</think>")
                    if (index > 0) {
                        thinking = content.substring(7, index).trim()
                        content = content.substring(index + 8).trim()
                    }
                }
                LLM.Result.TextResponse(thinking = thinking, content = content)
            }

            "tool_calls" -> {
                val calls = msg["tool_calls"]!!.jsonArray.map {
                    val call = it.jsonObject
                    val id = call["id"]!!.jsonPrimitive!!.content
                    val func = call["function"]!!.jsonObject
                    val name = func["name"]!!.jsonPrimitive.content
                    val arguments = func["arguments"]!!.jsonPrimitive.content
                    LLM.FunctionCall(
                        id = id,
                        name = name,
                        arguments = arguments
                    )

                }
                LLM.Result.ToolCall(list = calls)
            }

            else -> TODO("Unknown finish reason: $finishReason")
        }
    }
}