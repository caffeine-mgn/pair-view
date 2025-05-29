package pw.binom

import android.widget.Toast
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import pw.binom.http.client.HttpClientRunnable
import pw.binom.http.client.factory.Https11ConnectionFactory
import pw.binom.http.client.factory.NativeNetChannelFactory
import pw.binom.logger.Logger
import pw.binom.logger.info
import pw.binom.network.MultiFixedSizeThreadNetworkDispatcher
import pw.binom.url.toURL
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.seconds

abstract class AbstractNetworkService<REQUEST, RESPONSE, EVENT> : BService() {
    protected val logger by Logger.ofThisOrGlobal
    protected val config by define {
        DeviceConfig.open(this)
    }
    protected abstract val requestSerializer: KSerializer<REQUEST>
    protected abstract val responseSerializer: KSerializer<RESPONSE>
    protected abstract val eventSerializer: KSerializer<EVENT>

    suspend fun sendEvent(event: EVENT) {
        val device = device ?: return
        val json = json.encodeToString(eventSerializer, event)
        device.sendEvent(json.encodeToByteArray())
    }

    protected val fileManager by define {
        FileManager(it)
    }

    protected val networkManager by define {
        MultiFixedSizeThreadNetworkDispatcher(Runtime.getRuntime().availableProcessors())
    }.destroyWith {
        it.closeAnyway()
    }

    protected val httpClient by define {
        HttpClientRunnable(
            factory = Https11ConnectionFactory(),
            source = NativeNetChannelFactory(networkManager),
            idleCoroutineContext = networkManager,
        )
    }

    protected val json = Json {
        ignoreUnknownKeys = true
    }

    protected val networkJob by define {
        networkManager.launch {
            processing()
        }
    }.destroyWith {
        it.cancel()
    }

    protected abstract suspend fun rpc(param: REQUEST): RESPONSE

    private val header = object : DeviceClient.Handler {
        override suspend fun rpcRequest(data: ByteArray): ByteArray {
            val request = json.decodeFromString(requestSerializer, data.decodeToString())
            val resp = rpc(request)
            return json.encodeToString(responseSerializer, resp).encodeToByteArray()
        }

    }
    private var device: DeviceClient? = null
    protected val isConnected
        get() = device != null

    protected open fun connected() {

    }

    protected open fun disconnected() {

    }

    private suspend fun processing() {
        while (coroutineContext.isActive) {
            val client = try {
                withTimeout(20.seconds) {
                    DeviceClient.create(
                        handler = header,
                        client = httpClient,
                        url = config.serverUrl.toURL(),
                        deviceId = config.id,
                        deviceName = config.name,
                        contentType = "application/json",
                        deviceSecret = config.secret,
                    )
                }
            } catch (e: TimeoutCancellationException) {
                logger.info("Connection timeout")
                delay(10.seconds)
                continue
            } catch (e: Throwable) {
                logger.info("Can't connect")
                delay(10.seconds)
                continue
            }
            runOnUi {
                Toast.makeText(this, "Connected to server!", Toast.LENGTH_SHORT).show()
            }
            device = client
            connected()
            try {
                client.processing()
            } catch (e: Throwable) {
                logger.info("Disconnected :(")
                e.printStackTrace()
                delay(10.seconds)
                continue
            } finally {
                device = null
                disconnected()
                runOnUi {
                    Toast.makeText(this, "Connection lost", Toast.LENGTH_SHORT).show()
                }
                runCatching { client.asyncClose() }
            }
        }
    }
}