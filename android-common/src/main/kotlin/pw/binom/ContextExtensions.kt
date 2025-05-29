package pw.binom

import android.content.Context
import android.content.Intent
import kotlin.jvm.java
import kotlin.reflect.KClass

fun Context.startService(serviceClass: KClass<*>, start: Boolean) {
    Intent(this, serviceClass.java).also {
        it.action = if (start) "start" else "stop"
        this.startService(it)
        return
    }
}