package pw.binom

import kotlinx.coroutines.isActive
import kotlinx.serialization.ExperimentalSerializationApi
import pw.binom.dto.ClientRequest
import pw.binom.dto.Headers
import pw.binom.dto.ProcessingChannel
import pw.binom.dto.ServerRequest
import pw.binom.dto.WsMessage
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
import pw.binom.url.URL
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.coroutines.coroutineContext

@OptIn(ExperimentalSerializationApi::class, ExperimentalAtomicApi::class)
class DeviceClient(
    val connection: WebSocketConnection,
    val handler: DeviceEventProcessor,
) : AsyncCloseable {
    interface DeviceEventProcessor {
        suspend fun processing(msg: ByteArray): ByteArray?
    }

    companion object {
        private val logger = Logger.getLogger("NetworkContext")
        suspend fun create(
            handler: DeviceEventProcessor,
            client: HttpClientRunnable,
            url: URL,
            deviceName: String,
            deviceId: String,
        ): DeviceClient {
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
            return DeviceClient(
                connection = connection,
                handler = handler,
            )
        }
    }


    private val sendingChannel = ProcessingChannel<WsMessage> { message ->
        connection.write(MessageType.BINARY).useAsync { output ->
            message.write(output)
        }
    }

    /*
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
    */
    suspend fun processing() {
        sendingChannel.start(coroutineContext)
        try {
            while (coroutineContext.isActive) {
                val req = connection.read().useAsync { msg ->
                    WsMessage.read(msg)
                }
                val resp = handler.processing(req.data)
                if (resp != null) {
                    sendingChannel.push(WsMessage(id = req.id, data = resp))
                }
            }
        } finally {
            sendingChannel.close()
        }
    }

    override suspend fun asyncClose() {
        connection.asyncCloseAnyway()
    }

}