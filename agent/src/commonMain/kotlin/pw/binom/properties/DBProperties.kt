package pw.binom.properties

import kotlinx.serialization.Serializable
import pw.binom.properties.serialization.annotations.PropertiesPrefix

@Serializable
@PropertiesPrefix("strong.db")
data class DBProperties(
    val login: String,
    val password: String,
    val host: String,
    val port: Int = 5432,
    val dbName: String,
    val maxConnections: Int,
)