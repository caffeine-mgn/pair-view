@file:JvmName("JvmMain")
package pw.binom

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pw.binom.config.DefaultConfig
import pw.binom.io.file.readText
import pw.binom.io.file.takeIfFile
import pw.binom.io.file.workDirectoryFile
import pw.binom.io.use
import pw.binom.network.MultiFixedSizeThreadNetworkDispatcher
import pw.binom.properties.ini.addIni
import pw.binom.strong.nats.client.NatsClientConfig
import pw.binom.signal.Signal
import pw.binom.strong.Strong
import pw.binom.strong.bean
import pw.binom.strong.properties.StrongProperties
import pw.binom.strong.properties.yaml.addYaml
import pw.binom.strong.web.server.WebConfig
import pw.binom.strong.web.server.properties.WebServerProperties

fun main(args: Array<String>) {
    val properties = StrongProperties()
        .addEnvironment()
        .addArgs(args)
println("Environment.workDirectoryFile=${Environment.workDirectoryFile}")
    Environment.workDirectoryFile.relative("config.yaml").takeIfFile()?.also {
        properties.addYaml(it.readText())
    }

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
                NatsClientConfig.apply(properties),
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