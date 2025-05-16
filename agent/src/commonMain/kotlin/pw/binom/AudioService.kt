package pw.binom

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import pw.binom.http.client.Http11ClientExchange
import pw.binom.http.client.HttpClientRunnable
import pw.binom.io.AsyncInput
import pw.binom.io.http.AsyncMultipartOutput
import pw.binom.io.http.HttpContentLength
import pw.binom.io.http.httpContentLength
import pw.binom.io.useAsync
import pw.binom.io.writeByteArray
import pw.binom.properties.AppProperties
import pw.binom.strong.inject
import pw.binom.strong.properties.injectProperty
import pw.binom.url.toURL

/**
 * https://speaches.ai/api/
 */
class AudioService {
    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
        }
    }

    @Serializable
    data class VoiceModel(
        @SerialName("model_id")
        val modelId: String,
        @SerialName("voice_id")
        val voiceId: String,
        val created: Int,
        @SerialName("owned_by")
        val ownedBy: String,
        @SerialName("sample_rate")
        val sampleRate: Int,
        @SerialName("model_path")
        val modelPath: String,
        @SerialName("object")
        val type: String,
        val id: String,
    )

    @Serializable
    data class TextToSpeechDto(
        val input: String,
        @SerialName("response_format")
        val responseFormat: ResponseFormat? = null,
        val model: String? = null,
        val language: String? = null,
        val speed: Int? = null,
        val voice: String? = null,
        @SerialName("sample_rate")
        val sampleRate: Int? = null, // 8000
    )

    enum class ResponseFormat {
        @SerialName("mp3")
        MP3,

        @SerialName("wav")
        WAV,
    }

    private val properties: AppProperties by injectProperty()
    private val httpClient: HttpClientRunnable by inject()

    suspend fun getVoices(modelId: String): List<VoiceModel> {
        val resp = httpClient.request(
            method = "GET",
            url = properties.voiceServiceUrl.toURL()
                .appendPath("/v1/audio/speech/voices")
                .appendQuery("model_id", modelId)
        ).connect().useAsync {
            check(it.getResponseCode() == 200) { "Invalid response code: ${it.getResponseCode()}" }
            it.readAllText()
        }

        return json.decodeFromString(
            deserializer = ListSerializer(VoiceModel.serializer()),
            string = resp,
        )
    }

    @Serializable
    private data class SpeechToTextResponseDto(
        val text: String,
    )

    /**
     * http://192.168.76.125/v1/models
     */
    suspend fun speechToText(
        model: String,
        language: String? = null,
        prompt: String? = null,
        responseFormat: String? = null,
        temperature: Float,
        hotWords: String? = null,
        vadFilter: Boolean,
        input: AsyncInput,
    ): String {
        var url = properties.voiceServiceUrl.toURL()
            .appendPath("/v1/audio/transcriptions")
        val boundary = AsyncMultipartOutput.generateBoundary()
        return httpClient.request(
            method = "POST",
            url = url,
        ).also {
            it.headers.contentType = "multipart/form-data; boundary=$boundary"
            it.headers.httpContentLength = HttpContentLength.CHUNKED
        }.connect().useAsync { connection ->
            connection as Http11ClientExchange
            AsyncMultipartOutput(
                boundary = boundary,
                stream = connection.getOutput()
            ).useAsync { multipart ->
                multipart.formData(formName = "model")
                multipart.write(model.encodeToByteArray())
                multipart.formData(formName = "temperature")
                multipart.write(temperature.toString().encodeToByteArray())
                multipart.formData(formName = "vad_filter")
                multipart.write(vadFilter.toString().encodeToByteArray())
                if (language != null) {
                    multipart.formData(formName = "language")
                    multipart.writeByteArray(language.encodeToByteArray())
                }
                if (prompt != null) {
                    multipart.formData(formName = "prompt")
                    multipart.writeByteArray(prompt.encodeToByteArray())
                }
                if (responseFormat != null) {
                    multipart.formData(formName = "response_format")
                    multipart.writeByteArray(responseFormat.encodeToByteArray())
                }
                if (hotWords != null) {
                    multipart.formData(formName = "hotwords")
                    multipart.writeByteArray(hotWords.encodeToByteArray())
                }
                multipart.formData(formName = "file", fileName = "audio.mp3")
                input.copyTo(multipart)
            }
            check(connection.getResponseCode() == 200) { "Invalid response code: ${connection.getResponseCode()}" }
            val text = connection.readAllText()
            json.decodeFromString(SpeechToTextResponseDto.serializer(), text).text
        }
    }

    /**
     * http://192.168.76.125/v1/audio/speech/voices?model_id=rhasspy%2Fpiper-voices
     */
    suspend fun textToSpeech(data: TextToSpeechDto): AsyncInput {
        val requestText = json.encodeToString(TextToSpeechDto.serializer(), data)
        val request = httpClient.request(
            method = "POST",
            url = properties.voiceServiceUrl.toURL().appendPath("/v1/audio/speech")
        ).also {
            it.headers.contentType = "application/json;charset=utf-8"
            it.headers.httpContentLength = HttpContentLength.CHUNKED
        }.connect() as Http11ClientExchange
        SafeException.async {
            request.sendText(requestText)
        }
        if (request.getResponseCode() != 200) {
            println("error response: ${request.readAllText()}")
            println("Request message: $requestText")
            throw IllegalStateException("Invalid response code: ${request.getResponseCode()}")
        }
        println("request.getResponseCode()=${request.getResponseCode()}")
        return AutoClosableAsyncInput(request.getInput()) { request.asyncClose() }
    }
}