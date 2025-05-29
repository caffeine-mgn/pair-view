package pw.binom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.protobuf.ProtoBuf

@OptIn(ExperimentalSerializationApi::class)
object EventBroadcast {
    private const val DATA = "data"
    private val proto = ProtoBuf

    fun <T> listener(
        context: Context,
        channel: String,
        serializer: KSerializer<T>,
        handler: (T) -> Unit,
    ): Listener = ListenerImpl(
        context = context,
        channel = channel,
        serializer = serializer,
        handler = handler,
    )

    fun <T> publisher(
        context: Context,
        channel: String,
        serializer: KSerializer<T>,
    ): Publisher<T> = PublisherImpl(
        context = context,
        channel = channel,
        serializer = serializer,
    )

    interface Publisher<T> {
        fun publish(value: T)
    }

    interface Listener {
        fun reg()
        fun unreg()
    }


    private class PublisherImpl<T>(
        val context: Context,
        val channel: String,
        val serializer: KSerializer<T>,
    ) : Publisher<T> {
        override fun publish(value: T) {
            val msg = proto.encodeToByteArray(serializer, value)
            context.sendEvent(channel) {
                putExtra(DATA, msg)
            }
        }

    }

    private class ListenerImpl<T>(
        val context: Context,
        val channel: String,
        val serializer: KSerializer<T>,
        val handler: (T) -> Unit,
    ) : BroadcastReceiver(), Listener {
        override fun onReceive(context: Context?, intent: Intent) {
            val bundle = intent.extras ?: return
            val data = bundle.getByteArray(DATA) ?: return
            val obj = proto.decodeFromByteArray(serializer, data)
            handler(obj)
        }

        override fun reg() {
            context.registerReceiver(this, IntentFilter(channel))
        }

        override fun unreg() {
            context.unregisterReceiver(this)
        }
    }
}