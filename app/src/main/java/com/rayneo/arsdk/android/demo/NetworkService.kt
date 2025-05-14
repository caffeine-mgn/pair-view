package com.rayneo.arsdk.android.demo
/*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pw.binom.VideoClientImpl
import pw.binom.network.MultiFixedSizeThreadNetworkDispatcher
import kotlin.coroutines.coroutineContext

object NetworkService {
    private var networkManager: MultiFixedSizeThreadNetworkDispatcher? = null

    val current: VideoClientImpl?
        get() = null

    private val threadFunc = Runnable {
        MultiFixedSizeThreadNetworkDispatcher(
            Runtime.getRuntime().availableProcessors()
        )
    }

    private var job: Job? = null

    private suspend fun processing() {
        while (coroutineContext.isActive) {

        }
    }

    private fun checkRun() {
        if (networkManager == null) {
            networkManager = MultiFixedSizeThreadNetworkDispatcher(
                Runtime.getRuntime().availableProcessors()
            )
            job = GlobalScope.launch(networkManager!!) { processing() }
        }
    }

    fun stop() {
        runBlocking { job?.cancel() }
        networkManager?.close()
        job = null
        networkManager = null
    }
}
*/