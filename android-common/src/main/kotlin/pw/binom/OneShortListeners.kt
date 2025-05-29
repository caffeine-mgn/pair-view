package pw.binom

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import pw.binom.concurrency.SpinLock
import pw.binom.concurrency.synchronize
import pw.binom.io.Closeable

class OneShortListeners<T> {
    companion object {
    }

    private val waters2 = HashSet<(T) -> Unit>()
    private val lock = SpinLock()

    fun wait(func: (T) -> Unit): Closeable {
        lock.lock()
        try {
            waters2 += func
        } finally {
            lock.unlock()
        }
        return Closeable {
            lock.lock()
            try {
                waters2 -= func
            } finally {
                lock.unlock()
            }
        }
    }

    suspend fun await(): T =
        suspendCancellableCoroutine<T> { continuation ->
            val closable = wait {
                continuation.resumeWith(Result.success(it))
            }
            continuation.invokeOnCancellation {
                closable.close()
            }
        }

    fun fire(value: T) {
        lock.lock()
        val waters2 = try {
            if (waters2.isEmpty) {
                return
            }
            val w2 = ArrayList(waters2)
            waters2.clear()
            w2
        } finally {
            lock.unlock()
        }
        waters2.forEach {
            it(value)
        }
    }
}