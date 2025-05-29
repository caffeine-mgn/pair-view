package com.rayneo.arsdk.android.demo

import android.graphics.SurfaceTexture
import android.graphics.SurfaceTexture.OnFrameAvailableListener
import android.os.Handler
import android.os.Looper
import kotlin.coroutines.suspendCoroutine

fun OnFrameAvailableListener(func: (surfaceTexture: SurfaceTexture) -> Unit) =
    object : OnFrameAvailableListener {
        override fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
            func(surfaceTexture)
        }
    }