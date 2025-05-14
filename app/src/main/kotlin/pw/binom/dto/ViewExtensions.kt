package pw.binom.dto

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.View.MeasureSpec
import com.rayneo.arsdk.android.demo.ui.activity.VideoPlayerActivity

fun Context.sendEvent(channel:String,func:Intent.()->Unit){
    val intent = Intent(channel)
    func(intent)
    sendBroadcast(intent)
}

fun Context.isServiceRunning(serviceClass: Class<*>): Boolean {
    val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    for (service in manager.getRunningServices(Int.Companion.MAX_VALUE)) {
        if (serviceClass.getName() == service.service.getClassName()) {
            return true
        }
    }
    return false
}

fun View.redraw() {
    post {
        measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        )
        layout(left, top, right, bottom)
    }
}