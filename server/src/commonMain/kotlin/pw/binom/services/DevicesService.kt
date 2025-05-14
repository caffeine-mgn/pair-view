package pw.binom.services

import pw.binom.Device
import pw.binom.concurrency.ReentrantLock
import pw.binom.concurrency.synchronize
import pw.binom.io.http.websocket.WebSocketConnection
import pw.binom.logger.Logger
import pw.binom.logger.info
import pw.binom.mq.nats.NatsMqConnection
import pw.binom.network.NetworkManager
import pw.binom.strong.inject

class DevicesService {
    private val connections = HashMap<WebSocketConnection, Device>()
    private val connectionsById = HashMap<String, Device>()
    private val lock = ReentrantLock()
    private val logger by Logger.ofThisOrGlobal
    private val networkManager: NetworkManager by inject()
    private val nats: NatsMqConnection by inject()

    val glasses: List<Device>
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
        val glasses = Device(
            id = deviceId,
            name = deviceName,
            connection = connection,
            networkManager = networkManager,
            nats = nats,
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