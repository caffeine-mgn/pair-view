package pw.binom

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pw.binom.dto.DefaultSerialization
import pw.binom.dto.GlassesDto
import pw.binom.dto.JumpDto
import pw.binom.dto.OpenFileDto
import pw.binom.dto.PlaybackState
import pw.binom.dto.Response
import pw.binom.dto.UpdateViewDto
import pw.binom.http.client.HttpClientRunnable
import pw.binom.io.http.HttpContentLength
import pw.binom.io.http.httpContentLength
import pw.binom.io.useAsync
import pw.binom.properties.AppProperties
import pw.binom.strong.inject
import pw.binom.strong.properties.injectProperty
import pw.binom.url.URI
import pw.binom.url.toURI
import pw.binom.url.toURL

@Deprecated(message = "Not use it")
class GlassesServiceClient {
    private val client: HttpClientRunnable by inject()
    private val properties: AppProperties by injectProperty()
    private val json = Json

    suspend fun openFile(id: String, fileName: String) = post(
        url = "/api/clients/$id/actions/openFile".toURI(),
        request = OpenFileDto(
            name = fileName
        ),
        requestSerializer = OpenFileDto.serializer(),
        responseSerializer = Response.serializer(Unit.serializer()),
        method = "POST",
    )

    suspend fun play(id: String) =
        post(
            url = "/api/clients/${id}/actions/play".toURI(),
            request = Unit,
            requestSerializer = Unit.serializer(),
            responseSerializer = Response.serializer(Unit.serializer()),
            method = "POST",
        )

    suspend fun seek(id: String, position: Long) = post(
        url = "/api/clients/$id/actions/seek".toURI(),
        request = JumpDto(
            time = position
        ),
        requestSerializer = JumpDto.serializer(),
        responseSerializer = Response.serializer(Unit.serializer()),
        method = "POST"
    )

    suspend fun getState(id: String) =
        post(
            method = "GET",
            url = "/api/clients/$id/state".toURI(),
            responseSerializer = Response.serializer(PlaybackState.serializer()),
            request = Unit,
            requestSerializer = Unit.serializer(),
        )

    suspend fun pause(id: String) = post(
        url = "/api/clients/$id/actions/pause".toURI(),
        request = Unit,
        requestSerializer = Unit.serializer(),
        responseSerializer = Response.serializer(Unit.serializer()),
        method = "POST",
    )

    suspend fun getClients() = post(
        method = "GET",
        url = "/api/clients".toURI(),
        responseSerializer = Response.serializer(ListSerializer(GlassesDto.serializer())),
        request = Unit,
        requestSerializer = Unit.serializer(),
    )

    suspend fun getFiles(id: String) =
        post(
            method = "GET",
            url = "/api/clients/$id/files".toURI(),
            responseSerializer = Response.serializer(ListSerializer(String.serializer())),
            request = Unit,
            requestSerializer = Unit.serializer(),
        )

    suspend fun updateView(
        id: String,
        padding: Int,
        align: Int,
    ) {
        post(
            url = "/api/clients/$id/actions/openFile".toURI(),
            request = UpdateViewDto(
                padding = padding,
                align = align,
            ),
            requestSerializer = UpdateViewDto.serializer(),
            responseSerializer = Response.serializer(Unit.serializer()),
            method = "POST",
        )
    }

    suspend fun <T : Any, F : Any> post(
        url: URI,
        requestSerializer: KSerializer<F>,
        request: F,
        responseSerializer: KSerializer<T>,
        method: String,
    ): T {
        val req = client.request(
            method = method,
            url = properties.glassesServiceUrl.toURL().appendPath(url.toString())
        )
        if (requestSerializer != Unit.serializer()) {
            req.headers.httpContentLength = HttpContentLength.CHUNKED
            req.headers.contentType = "application/json"
        } else {
            req.headers.httpContentLength = HttpContentLength.NONE
        }
        return req.connect().useAsync { con ->
            if (requestSerializer != Unit.serializer()) {
                con.sendText(json.encodeToString(requestSerializer, request))
            }
            check(con.getResponseCode() == 200)
            if (responseSerializer == Unit.serializer()) {
                Unit as T
            } else {
                json.decodeFromString(responseSerializer, con.readAllText())
            }
        }
    }
}