@file:JvmName("JvmMain")
package pw.binom

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pw.binom.config.DefaultConfig
import pw.binom.io.use
import pw.binom.network.MultiFixedSizeThreadNetworkDispatcher
import pw.binom.properties.ini.addIni
import pw.binom.signal.Signal
import pw.binom.strong.Strong
import pw.binom.strong.bean
import pw.binom.strong.properties.StrongProperties
import pw.binom.strong.web.server.WebConfig
import pw.binom.strong.web.server.properties.WebServerProperties

fun main(args: Array<String>) {
    val properties = StrongProperties()
        .addEnvironment()
//        .add("strong.server.bind-addresses[0]", "0.0.0.0")
//        .add("strong.server.port", "8080")
    var strong: Strong? = null
    val e = properties.parse(WebServerProperties.serializer())
    println(e)
    MultiFixedSizeThreadNetworkDispatcher(Environment.availableProcessors).use { networkManager ->
        runBlocking {
            val s = Strong.create(
                DefaultConfig(properties),
                Strong.config {
                    it.bean { networkManager }
                    it.bean { properties }
                },
                WebConfig.apply(properties)
            )
            strong = s
            s.awaitDestroy()
        }
        Signal.handler {
            if (it.isInterrupted) {
                GlobalScope.launch(networkManager) {
                    strong?.destroy()
                }
            }
        }
    }
}