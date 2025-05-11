package pw.binom.services

import pw.binom.Glasses
import pw.binom.concurrency.ReentrantLock
import pw.binom.concurrency.synchronize
import pw.binom.io.http.websocket.WebSocketConnection
import pw.binom.logger.Logger
import pw.binom.logger.info
import pw.binom.network.NetworkManager
import pw.binom.strong.inject

class GlassesService {

    private val connections = HashMap<WebSocketConnection, Glasses>()
    private val connectionsById = HashMap<String, Glasses>()
    private val lock = ReentrantLock()
    private val logger by Logger.ofThisOrGlobal
    private val networkManager: NetworkManager by inject()

    val glasses: List<Glasses>
        get() = lock.synchronize {
            ArrayList(connections.values)
        }

    fun findById(id: String) = lock.synchronize {
        connectionsById[id]
    }

    suspend fun processing(
        deviceId: String,
        deviceName: String,
        connection: WebSocketConnection,
    ) {
        logger.info("Connected $deviceId:$deviceName")
        val glasses = Glasses(
            id = deviceId,
            name = deviceName,
            connection = connection,
            networkManager = networkManager,
        )
        lock.synchronize {
            connections[connection] = glasses
            connectionsById[deviceId] = glasses
        }
        try {
            glasses.processing()
        } finally {
            logger.info("Disconnected $deviceId:$deviceName")
            lock.synchronize {
                connections.remove(connection)
                connectionsById.remove(deviceId)
            }
        }
    }
}