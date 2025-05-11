package com.rayneo.arsdk.android.demo.ui

import android.view.SurfaceHolder

fun SurfaceHolder.onCreated(func: (SurfaceHolder) -> Unit) {
    addCallback(object : SurfaceHolder.Callback {
        override fun surfaceCreated(p0: SurfaceHolder) {
            func(p0)
        }

        override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
        }

        override fun surfaceDestroyed(p0: SurfaceHolder) {
        }
    })
}