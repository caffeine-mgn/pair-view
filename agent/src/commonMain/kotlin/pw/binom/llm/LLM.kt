package pw.binom.llm

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject

interface LLM {
    sealed interface Result {
        data class TextResponse(val thinking: String?, val content: String) : Result
        data class ToolCall(val list: List<FunctionCall>) : Result
    }

    sealed interface Message {
        sealed interface TextContent : Message {
            val content: String
        }

        data class System(override val content: String) : Message, TextContent
        data class User(override val content: String) : Message, TextContent
        data class Assistant(override val content: String) : Message, TextContent
        data class ToolResult(val id: String, val content: String) : Message
        data class AssistantToolCall(val calls: List<FunctionCall>) : Message
    }

    data class FunctionCall(val id: String, val name: String, val arguments: String)

    suspend fun send(
        model: String,
        temperature: Float,
        tools: List<JsonObject>?,
        messages: Flow<Message>,
    ): Result
}