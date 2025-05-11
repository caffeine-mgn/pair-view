package pw.binom

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.ExperimentalSerializationApi
import pw.binom.dto.ClientRequest
import pw.binom.dto.Headers
import pw.binom.dto.ProcessingChannel
import pw.binom.dto.ServerRequest
import pw.binom.http.client.HttpClientRunnable
import pw.binom.http.client.wsRequest
import pw.binom.io.AsyncCloseable
import pw.binom.io.http.headersOf
import pw.binom.io.http.websocket.MessageType
import pw.binom.io.http.websocket.WebSocketConnection
import pw.binom.io.useAsync
import pw.binom.logger.Logger
import pw.binom.logger.info
import pw.binom.logger.warn
import pw.binom.logger.warnSync
import pw.binom.url.URL
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.coroutines.coroutineContext

@OptIn(ExperimentalSerializationApi::class, ExperimentalAtomicApi::class)
class VideoClientImpl(
    val connection: WebSocketConnection,
    val handler: VideoClient.Handler,
) : VideoClient, AsyncCloseable {
    companion object {
        private val logger = Logger.getLogger("NetworkContext")
        suspend fun create(
            handler: VideoClient.Handler,
            client: HttpClientRunnable,
            url: URL,
            deviceName: String,
            deviceId: String,
        ): VideoClientImpl {
            logger.info("Connect to $url")
            val connection = try {
                client.wsRequest(
                    url = url,
                    headers = headersOf(
                        Headers.DEVICE_ID to deviceId,
                        Headers.DEVICE_NAME to deviceName
                    )
                ).connect()
            } catch (e: Throwable) {
                logger.warn(text = "Can't connect to $url", exception = e)
                throw e
            }
            logger.info("Success connected to $url")
            return VideoClientImpl(
                connection = connection,
                handler = handler,
            )
        }
    }


    private val sendingChannel = ProcessingChannel<ClientRequest> { message ->
        connection.write(MessageType.BINARY).useAsync { output ->
            message.write(output)
        }
    }

    private var counter = AtomicInt(0)
    private val waters = ConcurrentHashMap<Int, CancellableContinuation<ServerRequest>>()

    suspend fun sendAndWait(request: ClientRequest): ServerRequest {
        check(request.request) { "Is not request" }
        return suspendCancellableCoroutine {
            it.invokeOnCancellation {
                waters.remove(request.id)
            }
            waters[request.id] = it
        }
    }

    suspend fun sendWatchingTime(time: Long) {
        sendAndWait(
            ClientRequest(
                id = counter.incrementAndFetch(),
                request = true,
                playbackVideo = ClientRequest.PlaybackVideo(time = time)
            )
        )
    }

    suspend fun processing(request: ServerRequest): ClientRequest {
        when {
            request.getLocalFiles != null -> {
                return ClientRequest(
                    id = request.id,
                    request = false,
                    localFiles = ClientRequest.LocalFiles(handler.getLocalFiles())
                )
            }

            request.play != null -> {
                handler.play(request.play!!.time)
                return ClientRequest.ok(request.id)
            }

            request.pause != null -> {
                handler.pause(request.pause!!.time)
                return ClientRequest.ok(request.id)
            }

            request.openVideoFile != null -> {
                handler.openFile(request.openVideoFile!!.fileName, request.openVideoFile!!.time)
                return ClientRequest.ok(request.id)
            }

            request.seek != null -> {
                handler.seek(request.seek!!.time)
                return ClientRequest.ok(request.id)
            }

            request.updateView != null -> {
                val updateView = request.updateView!!
                handler.updateView(
                    padding = updateView.padding,
                    align = updateView.align,
                )
                return ClientRequest.ok(request.id)
            }

            request.getState != null -> {
                return ClientRequest(
                    id = request.id,
                    request = false,
                    state = ClientRequest.State(
                        videoFile = handler.getCurrentPlayingFile(),
                        playing = handler.isPlayingStatus(),
                        time = handler.getCurrentPlayingTime(),
                    )
                )
            }
        }
        return ClientRequest.ok(request.id)
    }


    suspend fun processing() {
        sendingChannel.start(coroutineContext)
        try {
            while (coroutineContext.isActive) {
                val resp = connection.read().useAsync { msg ->
                    ServerRequest.read(msg)
                }
                if (!resp.request) {
                    val water = waters.remove(resp.id) ?: continue
                    if (resp.error != null) {
                        water.resumeWith(Result.failure(RemoteException(resp.error!!.message)))
                    } else {
                        water.resumeWith(Result.success(resp))
                    }
                } else {
                    try {
                        val r = processing(resp)
                        if (r.request) {
                            logger.warn("Response marked as request")
                            continue
                        }
                        if (r.id != resp.id) {
                            logger.warn("Response id doesn't match with request id")
                            continue
                        }
                        sendingChannel.push(r)
                    } catch (e: Throwable) {
                        logger.warnSync(text = "Error on response\nRequest: $resp", exception = e)
                        sendingChannel.push(
                            ClientRequest(
                                id = resp.id,
                                request = false,
                                error = ClientRequest.Error(e.message ?: e.toString())
                            )
                        )
                    }
                }
            }
        } finally {
            waters.values.forEach { it.resumeWith(Result.failure(IllegalStateException("Disconnected"))) }
            waters.clear()
            sendingChannel.close()
        }
    }

    override suspend fun asyncClose() {
        connection.asyncCloseAnyway()
    }

}