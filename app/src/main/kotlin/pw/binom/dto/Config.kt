package pw.binom.dto

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Config(
    val serverUrl: String,
    val id: String,
    val name: String,
) {

    companion object {
        fun open(context: Context): Config {
            val configFile = context.obbDir.resolve("config.json")
            Log.i("Config", "config file: $configFile")
            return if (configFile.isFile) {
                Json.decodeFromString(serializer(), configFile.readText())
            } else {
                val config = Config(
                    serverUrl = "http://localhost:8080",
                    id = "1",
                    name = "glasses",
                )
                configFile.parentFile?.mkdirs()
                configFile.writeText(Json.encodeToString(serializer(), config))
                config
            }
        }
    }
}