package pw.binom.tools

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import pw.binom.AITool
import pw.binom.ChatHistoryService
import pw.binom.Description
import pw.binom.dto.Response
import pw.binom.strong.inject

class ClearHistory : AbstractAITool<ClearHistory.Function>() {
    private val chatHistoryService: ChatHistoryService by inject()
    override val function: KSerializer<Function>
        get() = Function.serializer()

    override suspend fun execute(
        chatId: Long,
        name: String,
        arguments: Function,
    ): JsonElement {
        chatHistoryService.clearForChar(chatId)
        return ok()
    }

    @Serializable
    @Description("Remove all conversation history")
    @SerialName("clear_conversation_history")
    object Function
}