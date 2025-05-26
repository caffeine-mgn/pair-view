package pw.binom

import kotlinx.serialization.json.Json
import pw.binom.properties.AppProperties
import pw.binom.strong.nats.client.AbstractNatsProducer
import pw.binom.strong.properties.injectProperty
import pw.binom.device.telegram.dto.SendTextMessageDto
import pw.binom.mq.MapHeaders

class TelegramChatOutcomeService : AbstractNatsProducer() {
    private val properties: AppProperties by injectProperty()
    override val topicName: String
        get() = properties.telegram.toChat.topic

    suspend fun send(message: SendTextMessageDto) {
        val jsonText = Json.encodeToString(SendTextMessageDto.serializer(), message)
        this.producer.send(
            headers = MapHeaders(mapOf("content-type" to listOf("application/json"))),
            data = jsonText.encodeToByteArray(),
        )
    }
}