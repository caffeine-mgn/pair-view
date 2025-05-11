package pw.binom.dto

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalAtomicApi::class)
class ProcessingChannel<T>(private val processing: suspend (T) -> Unit) : AutoCloseable {
    private val internalStarted = AtomicBoolean(false)
    private var job: Job? = null
    private val channel = Channel<T>()

    suspend fun push(value: T) {
        channel.send(value)
    }

    fun start(context: CoroutineContext) {
        check(internalStarted.compareAndSet(false, true)) {
            "Already started"
        }

        job = GlobalScope.launch(context) {
            while (isActive) {
                processing(channel.receive())
            }
        }
    }

    val started
        get() = internalStarted.load()

    override fun close() {
        if (!internalStarted.compareAndExchange(true, false)) {
            return
        }
        job?.cancel()
    }
}