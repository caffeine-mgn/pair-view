package pw.binom

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import pw.binom.device.telegram.dto.Content
import pw.binom.device.telegram.dto.MessageFromChatDto
import pw.binom.device.telegram.dto.ParseMode
import pw.binom.device.telegram.dto.SendTextMessageDto
import pw.binom.device.telegram.dto.TelegramEvent
import pw.binom.io.useAsync
import pw.binom.logger.Logger
import pw.binom.logger.warn
import pw.binom.mq.nats.client.NatsMessage
import pw.binom.network.NetworkManager
import pw.binom.properties.AppProperties
import pw.binom.strong.inject
import pw.binom.strong.nats.client.AbstractJetStreamNatsConsumer
import pw.binom.strong.properties.injectProperty
import pw.binom.telegram.dto.SendChatEvent
import kotlin.time.Duration.Companion.seconds

class TelegramIncomeService : AbstractJetStreamNatsConsumer() {

    private val properties: AppProperties by injectProperty()
    private val networkManager: NetworkManager by inject()
    private val telegramChatOutcomeService: TelegramChatOutcomeService by inject()
    private val telegramChatEventService: TelegramChatEventService by inject()
    private val chatService: ChatService by inject()
    private val audioService: AudioService by inject()
    private val storage: StorageService by inject()
    private val logger by Logger.ofThisOrGlobal

    override val config
        get() = properties.telegram.fromChat
    private val json = Json {
        ignoreUnknownKeys = true
    }

    override suspend fun consume(message: NatsMessage) {
        val incomeJson = message.data.decodeToString()
        println("Income: $incomeJson")
        val msg = json.decodeFromString(MessageFromChatDto.serializer(), incomeJson)
        val typingAction = networkManager.launch {
            while (isActive) {
                telegramChatEventService.send(
                    TelegramEvent(
                        chatId = msg.chatId,
                        event = TelegramEvent.Event.Typing
                    )
                )
                delay(5.seconds)
            }
        }
        try {
            messageProcessor(msg)
        } finally {
            typingAction.cancel()
        }
    }

    private suspend fun messageProcessor(msg: MessageFromChatDto) {
        val responseContent = when (val content = msg.content) {
            is Content.Text -> {
                Content.Text(
                    chatService.incomeMessageProcessing(
                        chatId = msg.chatId.toLong(),
                        incomeMessageText = content.value,
                    )
                )
            }

            is Content.Voice -> voiceMessageProcessing(
                chatId = msg.chatId,
                content = content,
            )
        }
        telegramChatOutcomeService.send(
            SendTextMessageDto(
                chatId = msg.chatId,
                content = responseContent,
                parseMode = ParseMode.HTML,
            )
        )
    }

    private suspend fun voiceMessageProcessing(chatId: String, content: Content.Voice): Content {
        val resultText = withRetry(30) {
            val voiceFile = storage.load(content.fileId)
            if (voiceFile == null) {
                return@withRetry null
            }
            voiceFile.useAsync { voice ->
                audioService.speechToText(
                    model = "arminhaberl/faster-whisper-medium",
                    temperature = 0.2f,
                    vadFilter = false,
                    language = "ru",
                    input = voice
                )
            }
        }
        if (resultText == null) {
            logger.warn("Voice file not found")
            return Content.Text("Неудалось найти аудио файл")
        }
        val assistantResponse = chatService.incomeMessageProcessing(
            chatId = chatId.toLong(),
            incomeMessageText = resultText,
        )
        return Content.Text(assistantResponse)
    }
}