package pw.binom

import kotlinx.serialization.json.Json
import pw.binom.properties.AppProperties
import pw.binom.strong.nats.client.AbstractNatsProducer
import pw.binom.strong.properties.injectProperty
import pw.binom.device.telegram.dto.SendTextMessageDto
import pw.binom.device.telegram.dto.TelegramEvent
import pw.binom.mq.MapHeaders

class TelegramChatEventService : AbstractNatsProducer() {
    private val properties: AppProperties by injectProperty()
    override val topicName: String
        get() = properties.telegram.events.topic

    suspend fun send(message: TelegramEvent) {
        val jsonText = Json.encodeToString(TelegramEvent.serializer(), message)
        this.producer.send(
            headers = MapHeaders(mapOf("content-type" to listOf("application/json"))),
            data = jsonText.encodeToByteArray(),
        )
    }
}