package pw.binom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.protobuf.ProtoBuf
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

@OptIn(ExperimentalAtomicApi::class)
class ExchangeService(
    val context: Context,
    broadcastChannel: String,
    server: Boolean,
) : BroadcastReceiver() {

    private class MethodImplement<REQUEST, RESPONSE>(
        val method: Method<REQUEST, RESPONSE>,
        val function: suspend (REQUEST) -> RESPONSE,
    )

    private val methodImplements = HashMap<String, MethodImplement<Any?, Any?>>()

    fun <REQUEST, RESPONSE> implements(
        method: Method<REQUEST, RESPONSE>,
        invoke: suspend (REQUEST) -> RESPONSE,
    ) {
        val impl = MethodImplement(method = method, function = invoke)
        methodImplements[method.name] = impl as MethodImplement<Any?, Any?>
    }

    companion object {
        private const val ID = "id"
        private const val DATA = "data"
        private const val SUCCESS = "success"
        private const val METHOD = "method"
    }

    private val counter = AtomicInt(0)
    private val waters = ConcurrentHashMap<Int, CancellableContinuation<ByteArray>>()
    private val proto = ProtoBuf

    private val selfChannel =
        broadcastChannel + if (server) ".server" else ".client"

    private val otherChannel =
        broadcastChannel + if (!server) ".server" else ".client"

    fun reg() {
        context.registerReceiver(this, IntentFilter(selfChannel))
    }

    fun unreg() {
        context.unregisterReceiver(this)
    }

    suspend fun <REQUEST, RESPONSE> call(
        method: Method<REQUEST, RESPONSE>,
        params: REQUEST,
    ): RESPONSE {
        val msg = proto.encodeToByteArray(method.request, params)
        val responseBytes = suspendCancellableCoroutine<ByteArray> { continuation ->
            val msgId = counter.incrementAndFetch()
            continuation.invokeOnCancellation {
                waters.remove(msgId)
            }
            waters[msgId] = continuation
            context.sendEvent(otherChannel) {
                putExtra(ID, msgId)
                putExtra(DATA, msg)
                putExtra(METHOD, method.name)
            }
        }
        return proto.decodeFromByteArray(method.response, responseBytes)
    }

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "CANNOT_OVERRIDE_INVISIBLE_MEMBER")
    private suspend fun callProcessing(method: String, data: ByteArray): ByteArray {
        val method = methodImplements[method]
            ?: throw IllegalStateException("method $method not found")
        val param = proto.decodeFromByteArray(method.method.request, data)
        val response: Any? = method.function(param)
        return proto.encodeToByteArray(method.method.response, response)
    }


    override fun onReceive(context: Context, intent: Intent) {
        val bundle = intent.extras ?: return
        val method = bundle.getString(METHOD)
        val id = bundle.getInt(ID)

        if (method != null) {
            val data = bundle.getByteArray(DATA) ?: return
            GlobalScope.launch {
                val response = runCatching { callProcessing(method, data) }
                if (response.isSuccess) {
                    context.sendEvent(otherChannel) {
                        putExtra(ID, id)
                        putExtra(DATA, response.getOrThrow())
                        putExtra(SUCCESS, true)
                    }
                } else {
                    val ex = response.exceptionOrNull()!!
                    context.sendEvent(otherChannel) {
                        putExtra(ID, id)
                        putExtra(DATA, ex.message ?: ex.toString())
                        putExtra(SUCCESS, false)
                    }
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