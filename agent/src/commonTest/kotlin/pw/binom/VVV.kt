package pw.binom

import kotlinx.coroutines.runBlocking
import pw.binom.http.client.HttpClientRunnable
import pw.binom.http.client.factory.Https11ConnectionFactory
import pw.binom.http.client.factory.NativeNetChannelFactory
import pw.binom.io.use
import pw.binom.io.useAsync
import pw.binom.network.MultiFixedSizeThreadNetworkDispatcher
import pw.binom.rutracker.Rutracker
import kotlin.test.Test

class VVV {
    private val cookie =
        "bb_guid=dooEhOz28hZ1; bb_ssl=1; bb_session=0-7906186-wMojw4k81dF7vUuSDscp; bb_t=a%3A8%3A%7Bi%3A6692541%3Bi%3A1747222466%3Bi%3A6692433%3Bi%3A1747167543%3Bi%3A6682121%3Bi%3A1745345289%3Bi%3A6500155%3Bi%3A1743688601%3Bi%3A6670377%3Bi%3A1743618083%3Bi%3A6668672%3Bi%3A1743142598%3Bi%3A6666061%3Bi%3A1742983291%3Bi%3A6612608%3Bi%3A1738425668%3B%7D; cf_clearance=Onp84NOxVzP7Q7rLlMai1nMyprd0vsITeUHR5ilN2uA-1747847012-1.2.1.1-gTqxiVlTCNutgn5qqdgDkyUmWCbmGmDtlODCRxK.483ReEFd3Bst4_d6s6wUQW9N0k2p_9naVytZm4w7DRsUQDykkAE7ZM_6UYxAu0CAu7mBfpSzG2c84xCOjpWWEv0sQFTO7IS0KJIvQ90qK.u6cqoZc.DzzIN9MxYKQjkwjf2VooaDkOSwMVwnI0KiYZXdNX04dIxvDMXKq6O7PSFZ6PKOiJWwNZQxcedqhJolp7uyP8G7vp.qNonvHVYDPIKIjswu9GOoYJwYId3kutMsMByL3U7vys93DXeBwjSFgrPOi0YJGkoY__4puBRfYSo5JJLiQi3T3V7Gyq5zUKBtMEhEernLlCuYQJoI78QbP2w"

    @Test
    fun test() {
        MultiFixedSizeThreadNetworkDispatcher(4).use { nm ->
//            val factory = HttpProxyNetSocketFactory(
//                fallback = NativeNetChannelFactory(nm),
//                proxySelector = HttpProxyNetSocketFactory.ProxySelector { host, port ->
//                    HttpProxyNetSocketFactory.ProxyConfig(
//                        address = DomainSocketAddress(host = "5.187.7.38", port = 8828),
//                        auth = BasicAuth(login = "subochev", password = "Drovosek319")
//                    )
//                }
//            )
            val factory=NativeNetChannelFactory(nm)
            runBlocking(nm) {
                HttpClientRunnable(
                    source = factory,
                    factory = Https11ConnectionFactory()
                ).useAsync { client ->
                    val r = Rutracker(client = client, cookie = cookie)
                    r.search(text = "tool")
                }
            }
//            MultiFixedSizeThreadNetworkDispatcher
        }
    }
}