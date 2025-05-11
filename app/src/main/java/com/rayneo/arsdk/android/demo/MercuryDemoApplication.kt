package com.rayneo.arsdk.android.demo

import android.app.Application
import com.rayneo.arsdk.android.MercurySDK
import pw.binom.dto.AndroidLogHandler
import pw.binom.logger.Logger


class MercuryDemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Logger.global.handler = AndroidLogHandler
        MercurySDK.init(this)
    }
}