package pw.binom

import pw.binom.http.client.HttpClientRunnable
import pw.binom.http.client.factory.Https11ConnectionFactory
import pw.binom.http.client.factory.NativeNetChannelFactory
import pw.binom.llm.LMStudio
import pw.binom.network.NetworkManager
import pw.binom.properties.AppProperties
import pw.binom.strong.Strong
import pw.binom.strong.bean
import pw.binom.strong.beanAsyncCloseable
import pw.binom.strong.inject
import pw.binom.strong.properties.StrongProperties
import pw.binom.strong.properties.injectProperty
import pw.binom.tools.ClearHistory
import pw.binom.tools.CurrentTimeTool
import pw.binom.tools.DeviceInfoTool
import pw.binom.tools.GetAvailableVideoFiles
import pw.binom.tools.GetGlassesState
import pw.binom.tools.PauseTool
import pw.binom.tools.PlayTool
import pw.binom.tools.PlayVideoTool
import pw.binom.tools.SeekDeltaTool
import pw.binom.tools.SeekAbsoluteTool
import pw.binom.url.toURL

fun BaseConfig(properties: StrongProperties, networkManager: NetworkManager) = Strong.config {
//    it.bean { TelegramService() }
    it.bean { TelegramIncomeService() }
    it.bean { TelegramChatOutcomeService() }
    it.bean { StrongS3Client() }
    it.bean { StorageService() }
    it.bean { TelegramChatEventService() }
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
    it.bean { DeviceInfoTool() }
    it.bean { AudioService() }
    it.bean { CurrentTimeTool() }
    it.beanAsyncCloseable {
        HttpClientRunnable(
            source = NativeNetChannelFactory(
                manager = networkManager
            ),
            factory = Https11ConnectionFactory(),
        )
    }
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