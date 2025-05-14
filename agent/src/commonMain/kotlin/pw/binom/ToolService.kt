package pw.binom

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import pw.binom.strong.BeanLifeCycle
import pw.binom.strong.injectServiceList

class ToolService {
    private val tools by AITool::class.injectServiceList()

    private val toolMap = HashMap<String, ToolItem<Any>>()

    init {
        BeanLifeCycle.postConstruct {
            tools.forEach { tool ->
                addTool(tool.function) { chatId, name, arguments ->
                    tool as AITool<Any>
                    val result = tool.execute(chatId, name, arguments as Any)
                    Json.encodeToString(JsonElement.serializer(), result)
                }
            }
        }
    }

    val toolSchemes
        get() = toolMap.map { it.value.schema }

    private class ToolItem<T>(
        val serializer: KSerializer<T>,
        val processing: suspend (chatId: Long, name: String, T) -> String,
        val schema: JsonObject,
    )

    fun <T> addTool(
        serializer: KSerializer<T>,
        func: suspend (chatId: Long, name: String, T) -> String,
    ) {
        val tool = ToolItem(
            serializer = serializer,
            processing = func,
            schema = Tool.toolFunction(serializer.descriptor),
        )
        toolMap[serializer.descriptor.serialName] = tool as ToolItem<Any>
    }


    suspend fun execute(chatId: Long, name: String, arguments: String): String {
        val tool = toolMap[name] ?: return """{"error":"function $name not found"}"""
        try {
            val argObject: Any = Json.decodeFromString(tool.serializer, arguments)
            return tool.processing(chatId, name, argObject)
        } catch (e: Throwable) {
            e.printStackTrace()
            return """{"error":"internal error"}"""
        }
    }
}