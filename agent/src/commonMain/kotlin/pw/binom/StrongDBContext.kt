package pw.binom

import pw.binom.db.async.pool.AsyncConnectionPool
import pw.binom.db.serialization.AbstractDBContext
import pw.binom.db.serialization.SQLSerialization
import pw.binom.strong.inject

class StrongDBContext : AbstractDBContext() {
    override val pool: AsyncConnectionPool by inject()
    override val sql: SQLSerialization = SQLSerialization()
}