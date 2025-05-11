package pw.binom.dto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pw.binom.VideoClient
import pw.binom.VideoClientImpl
import pw.binom.http.client.HttpClientRunnable
import pw.binom.http.client.factory.NativeNetChannelFactory
import pw.binom.io.Closeable
import pw.binom.io.httpClient.HttpClient
import pw.binom.io.httpClient.create
import pw.binom.io.socket.InetSocketAddress
import pw.binom.logger.Logger
import pw.binom.logger.info
import pw.binom.logger.infoSync
import pw.binom.logger.warn
import pw.binom.logger.warnSync
import pw.binom.network.MultiFixedSizeThreadNetworkDispatcher
import pw.binom.network.tcpConnect
import pw.binom.url.URL
import kotlin.time.Duration.Companion.seconds

class AndroidVideoClient(
    val url: URL,
    val handler: VideoClient.Handler,
    val deviceId: String,
    val deviceName: String,
) : VideoClient, Closeable {
    private val logger by Logger.ofThisOrGlobal
    private val network = MultiFixedSizeThreadNetworkDispatcher(threadSize = 2, selectTimeout = 5.seconds)
    private val client = HttpClientRunnable(
        source = NativeNetChannelFactory(network),
    )
    private var activeClient: VideoClientImpl? = null
    val process = GlobalScope.launch(network) {
        logger.info("Connect to google.com")
        network.tcpConnect(InetSocketAddress.resolve(host = "google.com", port = 443))
        logger.info("Successes connected to google.com")
        while (isActive) {
            activeClient?.asyncCloseAnyway()
            val client = try {
                VideoClientImpl.create(
                    handler = handler,
                    client = client,
                    url = url,
                    deviceId = deviceId,
                    deviceName = deviceName,
                )
            } catch (e: Throwable) {
                logger.info(text = "Can't connect to $url", exception = e)
                delay(10.seconds)
                continue
            }
            activeClient = client
            try {
                client.processing()
            } catch (e: Throwable) {
                activeClient = null
                logger.warn(text = "Error on client processing", exception = e)
                continue
            }
        }
    }

    override fun close() {
        logger.infoSync("Closing")
        GlobalScope.launch(network) {
            runBlocking {
                logger.infoSync("Stopping process...")
                process.cancelAndJoin()
                logger.infoSync("Stopping client...")
                client.asyncClose()
                logger.infoSync("Closing network manager...")
                network.close()
                logger.infoSync("Closed success!")
            }
        }
    }

}