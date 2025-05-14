package pw.binom.dto

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

@OptIn(ExperimentalAtomicApi::class)
class ExchangeService(
    val context: Context,
    val broadcastChannel: String,
    val selfChannel: String,
    val commandProcessor: (ByteArray) -> ByteArray,
) : BroadcastReceiver() {

    fun reg() {
        context.registerReceiver(this, IntentFilter(selfChannel))
    }

    fun unreg() {
        context.unregisterReceiver(this)
    }

    companion object {
        private const val REQUEST_FLAG = "request"
        private const val ID = "id"
        private const val DATA = "data"
        private const val SUCCESS = "success"
    }

    private val counter = AtomicInt(0)
    private val waters = ConcurrentHashMap<Int, CancellableContinuation<ByteArray>>()

    suspend fun sendAndReceive(msg: ByteArray): ByteArray =
        suspendCancellableCoroutine<ByteArray> { continuation ->
            val msgId = counter.incrementAndFetch()
            waters[msgId] = continuation
            context.sendEvent(broadcastChannel) {
                this.putExtra(ID, msgId)
                this.putExtra(DATA, msg)
                this.putExtra(REQUEST_FLAG, true)
            }
        }

    override fun onReceive(context: Context, intent: Intent) {
        val bundle = intent.extras ?: return
        val isRequest = bundle.getBoolean(REQUEST_FLAG)
        val id = bundle.getInt(ID)
        if (isRequest) {
            val data = bundle.getByteArray(DATA) ?: return
            val runResult = runCatching { commandProcessor(data) }
            if (runResult.isSuccess) {
                context.sendEvent(broadcastChannel) {
                    putExtra(ID, id)
                    putExtra(DATA, runResult.getOrThrow())
                    putExtra(REQUEST_FLAG, false)
                    putExtra(SUCCESS, true)
                }
            } else {
                val ex = runResult.exceptionOrNull()!!
                context.sendEvent(broadcastChannel) {
                    putExtra(ID, id)
                    putExtra(DATA, ex.message ?: ex.toString())
                    putExtra(REQUEST_FLAG, false)
                    putExtra(SUCCESS, false)
                }
            }
        } else {
            val water = waters.remove(id) ?: return
            if (bundle.getBoolean(SUCCESS)) {
                val data = bundle.getByteArray(DATA) ?: return
                water.resumeWith(Result.success(data))
            } else {
                val data = bundle.getString(DATA)
                water.resumeWith(Result.failure(IllegalStateException(data)))
            }
        }
    }
}