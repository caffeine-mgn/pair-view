package pw.binom

import kotlinx.coroutines.isActive
import pw.binom.http.client.HttpClientRunnable
import pw.binom.logger.Logger
import pw.binom.logger.info
import pw.binom.properties.AppProperties
import pw.binom.strong.BeanLifeCycle
import pw.binom.strong.inject
import pw.binom.strong.properties.injectProperty
import pw.binom.telegram.TelegramClient
import pw.binom.telegram.dto.Message
import pw.binom.telegram.dto.ParseMode
import pw.binom.telegram.dto.SendChatEvent
import pw.binom.telegram.dto.TextMessage

class TelegramService {
    private val httpClient: HttpClientRunnable by inject()
    private val chatService: ChatService by inject()
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
        val text = message.text ?: return null
        return chatService.incomeMessageProcessing(
            chatId = message.chat.id,
            incomeMessageText = text,
        )
    }

    init {
        BeanLifeCycle.process {
            while (isActive) {
                logger.info("Request updates....")
                try {
                    telegram.getUpdate().forEach {
                        val message = it.message ?: return@forEach
                        telegram.sendChatAction(
                            chatId = message.chat.id.toString(),
                            action = SendChatEvent.Action.TYPING,
                        )
                        val resp = messageProcessing(message) ?: return@forEach

                        telegram.sendMessage(
                            TextMessage(
                                chatId = message.chat.id.toString(),
                                text = resp,
                                parseMode = ParseMode.MARKDOWN,
                            )
                        )
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                    break
                }
            }
        }
    }
}
