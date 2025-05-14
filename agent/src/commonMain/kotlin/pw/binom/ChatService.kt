package pw.binom

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import pw.binom.llm.LLM
import pw.binom.properties.AppProperties
import pw.binom.strong.inject
import pw.binom.strong.properties.injectProperty

class ChatService {
    private val llm: LLM by inject()
    private val chatHistoryService: ChatHistoryService by inject()
    private val toolService: ToolService by inject()
    private val appProperties: AppProperties by injectProperty()

    suspend fun incomeMessageProcessing(chatId: Long, incomeMessageText: String): String {
        chatHistoryService.pushMessages(
            charId = chatId,
            listOf(MessageContent(user = MessageContent.TextMessage(incomeMessageText))),
        )
        while (true) {
            val oldMessages = chatHistoryService.getAll(chatId = chatId).map { msg ->

                val r = when {
                    msg.system != null -> LLM.Message.System(msg.system.content)
                    msg.user != null -> LLM.Message.User(msg.user.content)
                    msg.assistant != null -> LLM.Message.Assistant(msg.assistant.content)
                    msg.assistantToolCall != null -> {
                        val calls = msg.assistantToolCall.calls.map { call ->
                            LLM.FunctionCall(
                                id = call.id,
                                name = call.name,
                                arguments = call.arguments,
                            )
                        }

                        LLM.Message.AssistantToolCall(calls)
                    }

                    msg.toolResult != null -> {
                        LLM.Message.ToolResult(
                            id = msg.toolResult.id,
                            content = msg.toolResult.content
                        )
                    }

                    else -> TODO()
                }
                r
            }
            val llmResult = llm.send(
                model = appProperties.llmModel,
                temperature = appProperties.llmTemperature,
                tools = toolService.toolSchemes,
                messages = flow {
                    emitAll(oldMessages)
//                    emit(LLM.Message.User(incomeMessageText))
                }
            )
            when (llmResult) {
                is LLM.Result.ToolCall -> {
                    val calls = llmResult.list.map { call ->
                        call to toolService.execute(
                            chatId = chatId,
                            name = call.name,
                            arguments = call.arguments
                        )
                    }

                    val request = MessageContent(
                        assistantToolCall = MessageContent.AssistantToolCall(
                            calls = calls.map {
                                MessageContent.FunctionCall(
                                    id = it.first.id,
                                    name = it.first.name,
                                    arguments = it.first.arguments
                                )
                            }
                        ))
                    val response = calls.map {
                        MessageContent(
                            toolResult = MessageContent.ToolResult(
                                id = it.first.id,
                                content = it.second,
                            )
                        )
                    }
                    chatHistoryService.pushMessages(
                        charId = chatId,
                        listOf(request) + response
                    )
                }

                is LLM.Result.TextResponse -> {
                    chatHistoryService.pushMessages(
                        charId = chatId,
                        listOf(
                            MessageContent(
                                assistant = MessageContent.TextMessage(
                                    content = llmResult.content,
                                )
                            )
                        )
                    )
                    return llmResult.content
                }
            }
        }
    }
}