package pw.binom

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import pw.binom.concurrency.ReentrantLock
import pw.binom.concurrency.synchronize
import pw.binom.dto.ClientRequest
import pw.binom.dto.ProcessingChannel
import pw.binom.dto.ServerRequest
import pw.binom.io.http.websocket.MessageType
import pw.binom.io.http.websocket.WebSocketConnection
import pw.binom.io.useAsync
import pw.binom.logger.Logger
import pw.binom.logger.warn
import pw.binom.network.NetworkManager
import pw.binom.strong.inject
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.coroutines.coroutineContext

@OptIn(ExperimentalAtomicApi::class)
class Glasses(
    val id: String,
    val name: String,
    private val connection: WebSocketConnection,
    private val networkManager: NetworkManager,
) {

    private val waters = HashMap<Int, CancellableContinuation<ClientRequest>>()
    private val waterLock = ReentrantLock()
    private val logger by Logger.ofThisOrGlobal

    private val sender = ProcessingChannel<ServerRequest> { input ->
        connection.write(MessageType.BINARY).useAsync { output ->
            input.write(output)
        }
    }

    private val requestCounter = AtomicInt(0)

    private suspend fun sendAndReceive(request: ServerRequest): ClientRequest {
        require(request.request)
        sender.push(request)
        return suspendCancellableCoroutine {
            it.invokeOnCancellation {
                waterLock.synchronize {
                    waters.remove(request.id)
                }
            }
            waterLock.synchronize {
                waters[request.id] = it
            }
        }
    }

    suspend fun seek(time: Long) {
        sendAndReceive(
            ServerRequest(
                id = requestCounter.incrementAndFetch(),
                request = true,
                seek = ServerRequest.Seek(
                    time = time
                ),
            )
        )
    }

    suspend fun play(file: String, time: Long) {
        sendAndReceive(
            ServerRequest(
                id = requestCounter.incrementAndFetch(),
                request = true,
                openVideoFile = ServerRequest.OpenVideoFile(
                    fileName = file,
                    time = time
                ),
            )
        )
    }

    suspend fun play(time: Long) {
        sendAndReceive(
            ServerRequest(
                id = requestCounter.incrementAndFetch(),
                request = true,
                play = ServerRequest.Play(
                    time = time,
                ),
            )
        )
    }

    suspend fun pause(time: Long) {
        sendAndReceive(
            ServerRequest(
                id = requestCounter.incrementAndFetch(),
                request = true,
                pause = ServerRequest.Pause(
                    time = time,
                ),
            )
        )
    }

    suspend fun updateView(
        padding: Int,
        align: Int,
    ) {
        sendAndReceive(
            ServerRequest(
                id = requestCounter.incrementAndFetch(),
                request = true,
                updateView = ServerRequest.UpdateView(
                    padding = padding,
                    align = align,
                ),
            )
        )
    }

    suspend fun getState(): ClientRequest.State {
        val resp = sendAndReceive(
            ServerRequest(
                id = requestCounter.incrementAndFetch(),
                request = true,
                getState = ServerRequest.GetState,
            )
        )
        val localFiles = resp.state ?: TODO("Local files is null")
        return localFiles
    }

    suspend fun getFiles(): List<String> {
        val resp = sendAndReceive(
            ServerRequest(
                id = requestCounter.incrementAndFetch(),
                request = true,
                getLocalFiles = ServerRequest.GetLocalFiles,
            )
        )
        val localFiles = resp.localFiles ?: TODO("Local files is null")
        return localFiles.list
    }

    private suspend fun request(client: ClientRequest): ServerRequest {
        when {
            client.playbackVideo != null -> return ServerRequest.ok(client.id)
        }
        return ServerRequest(id = client.id, request = false, ok = ServerRequest.OK)
    }

    suspend fun processing() {
        sender.start(networkManager)
        try {
            while (coroutineContext.isActive) {
                connection.read().useAsync { input ->
                    val clientRequest = ClientRequest.read(input)
                    if (!clientRequest.request) {
                        val water = waterLock.synchronize {
                            waters.remove(clientRequest.id)
                        } ?: return@useAsync
                        if (!water.isCancelled) {
                            if (clientRequest.error != null) {
                                water.resumeWith(Result.failure(ClientException(clientRequest.error!!.msg)))
                            } else {
                                water.resumeWith(Result.success(clientRequest))
                            }
                        }
                    } else {
                        val resp = request(clientRequest)
                        if (resp.id != clientRequest.id) {
                            logger.warn("Invalid response: response id doesn't match with request id")
                            return@useAsync
                        }
                        if (resp.request) {
                            logger.warn("Invalid response: response not marked as response")
                            return@useAsync
                        }
                        sender.push(resp)
                    }
                }
            }
        } finally {
            sender.close()
        }
    }
}

