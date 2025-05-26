package pw.binom

import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

suspend fun <T> withRetry(
    count: Int = 5,
    delay: Duration = 10.seconds,
    func: suspend () -> T,
): T {
    var count = count
    var ex: Throwable? = null
    while (count > 0) {
        try {
            return func()
        } catch (_: Throwable) {
            count--
            delay(delay)
        }
    }
    checkNotNull(ex) { "Exception is null" }
    throw ex
}