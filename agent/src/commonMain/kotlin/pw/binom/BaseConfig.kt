package pw.binom

import pw.binom.http.client.HttpClientRunnable
import pw.binom.llm.LMStudio
import pw.binom.properties.AppProperties
import pw.binom.strong.Strong
import pw.binom.strong.bean
import pw.binom.strong.inject
import pw.binom.strong.properties.StrongProperties
import pw.binom.strong.properties.injectProperty
import pw.binom.tools.ClearHistory
import pw.binom.tools.GetAvailableVideoFiles
import pw.binom.tools.GetGlassesState
import pw.binom.tools.PauseTool
import pw.binom.tools.PlayTool
import pw.binom.tools.PlayVideoTool
import pw.binom.tools.SeekDeltaTool
import pw.binom.tools.SeekAbsoluteTool
import pw.binom.url.toURL

fun BaseConfig(properties: StrongProperties) = Strong.config {
    it.bean { TelegramService() }
    it.bean { ChatService() }
    it.bean { StrongAsyncConnectionPool() }
    it.bean { StrongDBContext() }
    it.bean { ChatHistoryService() }
    it.bean { GlassesServiceClient() }
    it.bean { ToolService() }
    it.bean { GetAvailableVideoFiles() }
    it.bean { GetGlassesState() }
    it.bean { PauseTool() }
    it.bean { PlayTool() }
    it.bean { PlayVideoTool() }
    it.bean { SeekAbsoluteTool() }
    it.bean { SeekDeltaTool() }
    it.bean { ClearHistory() }
    it.bean { DeviceClient() }
    it.bean { d ->
        StrongLLM(lazy {

            val client by d.inject<HttpClientRunnable>()
            val properties by d.injectProperty<AppProperties>()
            LMStudio(
                client = client,
                url = properties.lmStudioUrl.toURL(),
            )
        })
    }
}