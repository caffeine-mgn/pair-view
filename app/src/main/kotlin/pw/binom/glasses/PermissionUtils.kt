package pw.binom.glasses

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

fun Activity.requestPermissions(vararg permissions: String) {
    val notGranted = permissions.filter {
        checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
    }
    if (notGranted.isEmpty()) {
        requestPermissions()
    }
    ActivityCompat.requestPermissions(
        this,
        notGranted.toTypedArray(),
        1
    )
}