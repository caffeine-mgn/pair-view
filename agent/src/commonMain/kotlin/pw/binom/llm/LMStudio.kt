package pw.binom.llm

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import pw.binom.http.client.HttpClientExchange
import pw.binom.http.client.HttpClientRunnable
import pw.binom.io.AsyncAppendable
import pw.binom.io.asAsync
import pw.binom.io.bufferedWriter
import pw.binom.io.http.HttpContentLength
import pw.binom.io.http.httpContentLength
import pw.binom.io.useAsync
import pw.binom.url.URL

class LMStudio(val url: URL, val client: HttpClientRunnable) : AbstractOpenApi() {
    override suspend fun send(
        model: String,
        temperature: Float,
        tools: List<JsonObject>?,
        messages: Flow<LLM.Message>,
    ): LLM.Result {
        val ex = client.request(
            method = "POST",
            url = url.appendPath("v1/chat/completions"),
        ).also {
            it.headers.contentType = "application/json"
            it.headers.httpContentLength = HttpContentLength.CHUNKED
            it.headers.keepAlive = false
        }.connect() as HttpClientExchange

        val sb = StringBuilder()
        makeResponse(
            model = model,
            temperature = temperature,
            tools = tools,
            messages = messages,
            out = sb.asAsync(),
        )

        val text = ex.useAsync { con ->
            con.getOutput().bufferedWriter().useAsync { out ->
                makeResponse(
                    model = model,
                    temperature = temperature,
                    tools = tools,
                    messages = messages,
                    out = out,
                )
                out.flush()
            }
            con.readAllText()
        }
        return parseResult(text)
    }

}