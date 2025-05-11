package com.rayneo.arsdk.android.demo

import android.graphics.SurfaceTexture
import android.graphics.SurfaceTexture.OnFrameAvailableListener
import android.os.Handler
import android.os.Looper
import kotlin.coroutines.suspendCoroutine

fun runOnUi(func: () -> Unit) {
    if (Looper.getMainLooper().isCurrentThread) {
        func()
    } else {
        Handler(Looper.getMainLooper()).postAtFrontOfQueue {
            func()
        }
    }
}

suspend fun <T> runOnUiAsync(func: () -> T): T {
    if (Looper.getMainLooper().isCurrentThread) {
        return func()
    }
    return suspendCoroutine<T> { con ->
        Handler(Looper.getMainLooper()).postAtFrontOfQueue {
            con.resumeWith(runCatching { func() })
        }
    }
}

fun OnFrameAvailableListener(func: (surfaceTexture: SurfaceTexture) -> Unit) =
    object : OnFrameAvailableListener {
        override fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
            func(surfaceTexture)
        }
    }