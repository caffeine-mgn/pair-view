package pw.binom

import pw.binom.db.async.AsyncConnection
import pw.binom.db.async.pool.AbstractAsyncConnectionPool
import pw.binom.db.postgresql.async.PGConnection
import pw.binom.io.socket.DomainSocketAddress
import pw.binom.network.NetworkManager
import pw.binom.properties.DBProperties
import pw.binom.strong.inject
import pw.binom.strong.properties.injectProperty

class StrongAsyncConnectionPool : AbstractAsyncConnectionPool() {
    private val properties: DBProperties by injectProperty()
    private val networkManager: NetworkManager by inject()

    override val maxConnections: Int
        get() = properties.maxConnections

    override suspend fun createConnection(): AsyncConnection =
        PGConnection.connect(
            address = DomainSocketAddress(host = properties.host, port = properties.port),
            networkDispatcher = networkManager,
            userName = properties.login,
            password = properties.password,
            dataBase = properties.dbName,
        )
}