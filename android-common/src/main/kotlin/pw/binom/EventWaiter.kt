package pw.binom

import pw.binom.io.Closeable
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
class EventWaiter {
    private val finished = AtomicBoolean(false)
    private val listeners = ArrayList<Closeable>()

    private fun finish() {
        listeners.forEach {
            it.close()
        }
    }

    fun <T> wait(listener: OneShortListeners<T>, func: (T) -> Unit) {
        listeners += listener.wait {
            if (finished.compareAndSet(false, true)) {
                func(it)
            }
            finish()
        }
    }

    fun cancel() {
        finish()
    }
}