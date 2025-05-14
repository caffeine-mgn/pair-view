@file:JvmName("MainKt")

package pw.binom

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pw.binom.http.client.HttpClientRunnable
import pw.binom.http.client.factory.Https11ConnectionFactory
import pw.binom.http.client.factory.NativeNetChannelFactory
import pw.binom.io.file.readText
import pw.binom.io.file.takeIfFile
import pw.binom.io.file.workDirectoryFile
import pw.binom.io.use
import pw.binom.network.MultiFixedSizeThreadNetworkDispatcher
import pw.binom.signal.Signal
import pw.binom.strong.Strong
import pw.binom.strong.bean
import pw.binom.strong.beanAsyncCloseable
import pw.binom.strong.nats.client.NatsClientConfig
import pw.binom.strong.properties.StrongProperties
import pw.binom.strong.properties.yaml.addYaml
import pw.binom.strong.web.server.WebConfig

fun main(args: Array<String>) {
    println("Environment.workDirectory=${Environment.workDirectoryFile}")

    val properties = StrongProperties()
        .addEnvironment()
        .addArgs(args)

    Environment.workDirectoryFile.relative("config.yaml").takeIfFile()?.also {
        properties.addYaml(it.readText())
    }
    var strong: Strong? = null
    MultiFixedSizeThreadNetworkDispatcher(Environment.availableProcessors).use { networkManager ->
        runBlocking {
            val s = Strong.create(
                BaseConfig(properties),
                Strong.config {
                    it.bean { networkManager }
                    it.bean { properties }
                    it.beanAsyncCloseable {
                        HttpClientRunnable(
                            source = NativeNetChannelFactory(
                                manager = networkManager
                            ),
                            factory = Https11ConnectionFactory(),
                        )
                    }
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