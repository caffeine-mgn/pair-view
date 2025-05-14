package pw.binom

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement

interface AITool<T> {
    val function: KSerializer<T>
    suspend fun execute(chatId: Long, name: String, arguments: T): JsonElement
}