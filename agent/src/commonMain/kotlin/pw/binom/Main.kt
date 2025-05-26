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
import pw.binom.strong.StrongApplication
import pw.binom.strong.bean
import pw.binom.strong.beanAsyncCloseable
import pw.binom.strong.nats.client.NatsClientConfig
import pw.binom.strong.properties.BaseStrongProperties
import pw.binom.strong.properties.StrongProperties
import pw.binom.strong.properties.yaml.addYaml
import pw.binom.strong.web.server.WebConfig

fun main(args: Array<String>) {
    StrongApplication.run(args) {
        +BaseConfig(properties, networkManager)
        +NatsClientConfig.apply(properties)
        +WebConfig.apply(properties)
    }
    /*
        println("Environment.workDirectory=${Environment.workDirectoryFile}")

        val properties = BaseStrongProperties()
            .addEnvironment()
            .addArgs(args)

        Environment.workDirectoryFile.relative("config.yaml").takeIfFile()?.also {
            properties.addYaml(it.readText())
        }
        var strong: Strong? = null
        MultiFixedSizeThreadNetworkDispatcher(Environment.availableProcessors).use { networkManager ->
            runBlocking {
                val s = Strong.create(
                    BaseConfig(properties,networkManager),
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
     */
}