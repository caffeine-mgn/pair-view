package pw.binom
/*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import pw.binom.http.client.HttpClientRunnable
import pw.binom.http.client.factory.NativeNetChannelFactory
import pw.binom.io.use
import pw.binom.io.useAsync
import pw.binom.llm.LLM
import pw.binom.llm.LMStudio
import pw.binom.network.MultiFixedSizeThreadNetworkDispatcher
import pw.binom.tools.GetAvailableVideoFiles
import pw.binom.tools.GetGlassesState
import pw.binom.tools.PlayVideoTool
import pw.binom.url.toURL
import kotlin.test.Test

class ToolTest {

    @Test
    fun aa() {
        val result = Tool.toolFunction(PlayVideoTool.serializer().descriptor)
        val txt = Json {
            prettyPrint = true
        }.encodeToString(JsonObject.serializer(), result)
        println("-->$txt")
    }

    @Test
    fun aaa() {
        runBlocking {
            val tools =
                listOf(
                    Tool.toolFunction(GetAvailableVideoFiles.serializer()),
                    Tool.toolFunction(GetGlassesState.serializer()),
                )
            MultiFixedSizeThreadNetworkDispatcher(4).use { nm ->
                HttpClientRunnable(source = NativeNetChannelFactory(nm)).useAsync { client ->
                    val lm = LMStudio(
                        url = "http://127.0.0.1:11434/".toURL(),
                        client = client
                    )
                    val result = lm.send(
                        model = "qwen3-30b-a3b",
                        temperature = 0.1f,
                        tools = tools,
                        messages = flowOf(
                            LLM.Message.User("Привет! Что там есть посмотреть и что сейчас играет?"),
                            LLM.Message.AssistantToolCall(
                                listOf(
                                    LLM.FunctionCall(
                                        id = "111",
                                        name = "get_available_video_files",
                                        arguments = "{}"
                                    ),
                                    LLM.FunctionCall(
                                        id = "222",
                                        name = "get_current_glasses_state",
                                        arguments = "{}"
                                    ),
                                )
                            ),
                            LLM.Message.ToolResult(id = "111", content = """["1.mp4","Индиана Джонс 1.mp4"]"""),
                            LLM.Message.ToolResult(id = "222", content = """{"pause": true, "currentOpenFile": "Индиана Джонс 1.mp4"}"""),
                        )
                    )
                    println("Result:\n$result")
                }
            }
        }
    }
}
*/