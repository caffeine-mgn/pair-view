package pw.binom

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import org.w3c.xhr.XMLHttpRequest
import pw.binom.dto.DefaultSerialization
import pw.binom.url.URI
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object Network {

    suspend fun <T : Any> get(
        url: URI,
        responseSerializer: KSerializer<T>,
    ): T =
        send(
            method = "GET",
            param = null,
            responseSerializer = responseSerializer,
            url = url,
        )

    suspend fun <T : Any, F : Any> post(
        url: URI,
        requestSerializer: KSerializer<F>,
        request: F,
        responseSerializer: KSerializer<T>,
    ): T =
        send(
            method = "POST",
            param = DefaultSerialization.json.encodeToString(requestSerializer, request),
            responseSerializer = responseSerializer,
            url = url,
        )

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T : Any> send(
        method: String,
        url: URI,
        param: dynamic,
        responseSerializer: KSerializer<T>,
    ): T = suspendCoroutine { con ->
        val rr = XMLHttpRequest()
        rr.withCredentials = true
        rr.onreadystatechange = {
            if (rr.readyState == XMLHttpRequest.DONE) {
                if (rr.status == 200.toShort()) {
                    try {
                        val decodedResult = try {
                            con.resume(rr.decodeResult(responseSerializer))
                        } catch (e: SerializationException) {
                            console.error("Can't decode ${rr.responseText} to $responseSerializer")
                            con.resumeWithException(e)
                        } catch (e: dynamic) {
                            con.resumeWithException(e)
                        }
                    } catch (e: dynamic) {
                        con.resumeWithException(e)
                    }
                } else {
                    if ((rr.status == 202.toShort() || rr.status == 204.toShort()) && responseSerializer == Unit.serializer()) {
                        con.resume(Unit as T)
                    } else {
                        con.resumeWithException(RuntimeException("Unknown CODE=${rr.status}"))
                    }
                }
            }
        }
        rr.open(method = method, url = url.toString(), async = true)
        rr.send(if (param == Unit) null else param)
    }

    private fun <T : Any> XMLHttpRequest.decodeResult(responseSerializer: KSerializer<T>): T {
        fun decodeJson() = DefaultSerialization.json.decodeFromString(
            responseSerializer,
            responseText,
        )

        val type = getResponseHeader("content-type")?.lowercase() ?: return decodeJson()
        if (type == "application/json" || type.startsWith("application/json;")) {
            return decodeJson()
        }
        throw RuntimeException("Illegal contains type \"$type\"")
    }
}