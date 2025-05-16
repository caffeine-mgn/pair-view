package pw.binom.properties

import kotlinx.serialization.Serializable
import pw.binom.properties.serialization.annotations.PropertiesPrefix
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
)