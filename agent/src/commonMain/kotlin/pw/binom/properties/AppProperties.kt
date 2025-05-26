package pw.binom.properties

import kotlinx.serialization.Serializable
import pw.binom.properties.serialization.annotations.PropertiesPrefix
import pw.binom.strong.nats.client.NatsJetStreamConsumerProperties
import pw.binom.strong.nats.client.NatsProducerProperties
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Serializable
@PropertiesPrefix("app")
data class AppProperties(
    val telegramToken: String,
    val llmModel: String,
    val llmTemperature: Float = 0.1f,
    val lmStudioUrl: String,
    val glassesServiceUrl: String,
    val deviceCommandTimeout: Duration = 10.seconds,
    val voiceServiceUrl: String,
    val telegram: Telegram,
    val storage: Storage,
) {
    @Serializable
    data class Telegram(
        val fromChat: NatsJetStreamConsumerProperties,
        val toChat: NatsProducerProperties,
        val events: NatsProducerProperties,
    )

    @Serializable
    data class Storage(
        val accessKey: String,
        val secretKey: String,
        val url: String,
        val bucketName: String,
        val regin: String,
    )
}