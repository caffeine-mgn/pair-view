package pw.binom

import kotlinx.coroutines.isActive
import kotlinx.serialization.protobuf.ProtoBuf
import pw.binom.device.ws.dto.DeviceMessage
import pw.binom.device.ws.dto.ServerMessage
import pw.binom.device.ws.dto.WsDevice
import pw.binom.http.client.HttpClientRunnable
import pw.binom.http.client.wsRequest
import pw.binom.io.AsyncCloseable
import pw.binom.io.http.headersOf
import pw.binom.io.http.websocket.MessageType
import pw.binom.io.http.websocket.WebSocketConnection
import pw.binom.io.readBytes
import pw.binom.io.useAsync
import pw.binom.logger.Logger
import pw.binom.logger.info
import pw.binom.logger.warn
import pw.binom.url.URL
import kotlin.coroutines.coroutineContext

class DeviceClient(
    val connection: WebSocketConnection,
    val handler: Handler,
) : AsyncCloseable {
    companion object {
        private val logger = Logger.getLogger("DeviceClient")
        suspend fun create(
            handler: Handler,
            client: HttpClientRunnable,
            url: URL,
            deviceName: String,
            deviceId: String,
            contentType: String,
            deviceSecret: String,
        ): DeviceClient {
            val resultUrl=url.appendPath(WsDevice.BASE_CONTROL_URI)
            logger.info("Connect to $url, resultUrl: $resultUrl")
            val connection = try {
                client.wsRequest(
                    url = resultUrl,
                    headers = headersOf(
                        WsDevice.DEVICE_ID to deviceId,
                        WsDevice.DEVICE_NAME to deviceName,
                        WsDevice.DEVICE_MESSAGING_CONTENT_TYPE to contentType,
                        WsDevice.DEVICE_SECRET to deviceSecret,
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

    private val sendingChannel = ProcessingChannel<DeviceMessage> { message ->
        connection.write(MessageType.BINARY).useAsync { output ->
            val bytes = ProtoBuf.encodeToByteArray(DeviceMessage.serializer(), message)
            output.writeInt(bytes.size)
            output.writeFully(bytes)
        }
    }

    private suspend fun processingServerMessage(message: ServerMessage) {
        when (message) {
            is ServerMessage.Ping -> {
                sendingChannel.push(DeviceMessage.Pong(message.id))
            }

            is ServerMessage.RPCRequest -> {
                val result = runCatching { handler.rpcRequest(message.data) }
                val resp = if (result.isSuccess) {
                    DeviceMessage.RPCResponse(id = message.id, data = result.getOrThrow())
                } else {
                    val ex = result.exceptionOrNull()!!
                    DeviceMessage.RPCResponseError(
                        id = message.id,
                        message = ex.message ?: ex.toString()
                    )
                }
                sendingChannel.push(resp)
            }
        }
    }

    suspend fun processing() {
        sendingChannel.start(coroutineContext)
        try {
            while (coroutineContext.isActive) {
                val req = connection.read().useAsync { msg ->
                    val size = msg.readInt()
                    val bytes = msg.readBytes(size)
                    ProtoBuf.decodeFromByteArray(ServerMessage.serializer(), bytes)
                }
                processingServerMessage(req)
            }
        } finally {
            sendingChannel.close()
        }
    }

    suspend fun sendEvent(data: ByteArray) {
        sendingChannel.push(DeviceMessage.Event(data))
    }

    override suspend fun asyncClose() {
        connection.asyncCloseAnyway()
    }

    fun interface Handler {
        suspend fun rpcRequest(data: ByteArray): ByteArray
    }
}