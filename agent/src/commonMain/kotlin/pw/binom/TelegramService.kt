package pw.binom

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import pw.binom.http.client.HttpClientRunnable
import pw.binom.io.file.File
import pw.binom.io.file.openWrite
import pw.binom.io.use
import pw.binom.io.useAsync
import pw.binom.logger.Logger
import pw.binom.logger.info
import pw.binom.network.NetworkManager
import pw.binom.network.exceptions.ConnectionRefusedException
import pw.binom.properties.AppProperties
import pw.binom.strong.BeanLifeCycle
import pw.binom.strong.inject
import pw.binom.strong.properties.injectProperty
import pw.binom.telegram.TelegramClient
import pw.binom.telegram.dto.Message
import pw.binom.telegram.dto.ParseMode
import pw.binom.telegram.dto.SendChatEvent
import pw.binom.telegram.dto.TextMessage
import pw.binom.telegram.dto.Update
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TelegramService {
    private val networkManager: NetworkManager by inject()
    private val httpClient: HttpClientRunnable by inject()
    private val chatService: ChatService by inject()
    private val audioService: AudioService by inject()
    private val appProperties: AppProperties by injectProperty()
    private val logger by Logger.ofThisOrGlobal

    private val telegram by BeanLifeCycle.afterInit {
        TelegramClient.open(
            httpClient = httpClient,
            token = appProperties.telegramToken,
        )
    }

    private suspend fun messageProcessing(message: Message): String? {
        logger.info("Income $message")
        return when {
            message.text != null -> chatService.incomeMessageProcessing(
                chatId = message.chat!!.id,
                incomeMessageText = message.text!!,
            )

            message.voice != null -> {
                println("Income voice message... getting path")
                val filePath = withRetry {
                    telegram.getFile(message.voice!!.fileId).filePath!!
                }
                println("Path got! $filePath. Try to download...")
                val resultText = withRetry {
                    try {
                        telegram.downloadFile(filePath).useAsync { telegramVoiceStream ->
                            audioService.speechToText(
                                model = "arminhaberl/faster-whisper-medium",
                                temperature = 0.2f,
                                vadFilter = false,
                                language = "ru",
                                input = telegramVoiceStream
                            )
                        }
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        throw e
                    }
                }
                println("File was read: $resultText")
                chatService.incomeMessageProcessing(
                    chatId = message.chat!!.id,
                    incomeMessageText = resultText,
                )
            }

            else -> null
        }
//        val text = message.text ?: return null
//        return chatService.incomeMessageProcessing(
//            chatId = message.chat.id,
//            incomeMessageText = text,
//        )
    }

    init {
        BeanLifeCycle.process {
            withRetry(30) {
                telegram.deleteWebhook()
            }
            while (isActive) {
                try {

                    val incomes = withRetry(count = 30) {
                        val timeout = 1.minutes
                        withTimeoutOrNull(timeout) {
                            telegram.getUpdate(timeout = timeout.inWholeSeconds)
                        } ?: emptyList()
                    }
                    incomes.forEach {
                        val message = it.message ?: return@forEach

                        val typingAction = networkManager.launch {
                            while (isActive) {
                                withRetry(count = 30) {
                                    telegram.sendChatAction(
                                        chatId = message.chat!!.id.toString(),
                                        action = SendChatEvent.Action.TYPING,
                                    )
                                }
                                delay(5.seconds)
                            }
                        }

                        val resp = try {
                            messageProcessing(message) ?: return@forEach
                        } catch (e: Throwable) {
                            e.printStackTrace()
                            throw e
                        } finally {
                            typingAction.cancel()
                        }
                        withRetry(count = 30) {
                            /*
                            try {
                                logger.info("Try convert \"$resp\" to text...")
                                audioService.textToSpeech(
                                    data = AudioService.TextToSpeechDto(
                                        input = resp,
                                        responseFormat = AudioService.ResponseFormat.MP3,
                                        model = "rhasspy/piper-voices",
//                                        language = "ru-RU",
                                        voice = "ru_RU-irina-medium",
                                    )
                                ).useAsync { input ->
                                    logger.info("Output done. Sending to TG")
                                    var size = 0L
                                    telegram.sendVoice(
                                        chatId = message.chat!!.id.toString(),
                                        caption = resp,
                                    ) { output ->
                                        size = input.copyTo(output)
                                    }
                                    logger.info("Voice message done! filesize: $size")
                                }
                            } catch (e: Throwable) {
                                e.printStackTrace()
                                throw e
                            }
                            */
                            telegram.sendMessage(
                                TextMessage(
                                    chatId = message.chat!!.id.toString(),
                                    text = resp,
                                    parseMode = ParseMode.HTML,
                                )
                            )
                        }
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                    break
                }
            }
        }
    }
}
