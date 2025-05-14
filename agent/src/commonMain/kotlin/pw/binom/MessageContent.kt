package pw.binom

import kotlinx.serialization.Serializable
import pw.binom.llm.LLM.Message
import pw.binom.llm.LLM.Message.TextContent

@Serializable
data class MessageContent(
    val system: TextMessage? = null,
    val user: TextMessage? = null,
    val assistant: TextMessage? = null,
    val assistantToolCall: AssistantToolCall? = null,
    val toolResult: ToolResult? = null,

    ) {
    @Serializable
    data class TextMessage(val content: String)

    @Serializable
    data class ToolResult(val id: String, val content: String)

    @Serializable
    data class AssistantToolCall(val calls: List<FunctionCall>)

    @Serializable
    data class FunctionCall(val id: String, val name: String, val arguments: String)
}