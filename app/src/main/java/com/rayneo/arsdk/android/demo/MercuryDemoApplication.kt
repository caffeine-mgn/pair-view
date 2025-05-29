package com.rayneo.arsdk.android.demo

import android.app.Application
import com.rayneo.arsdk.android.MercurySDK
import pw.binom.AndroidLogHandler
import pw.binom.glasses.NetworkService
import pw.binom.logger.Logger
import pw.binom.logger.info
import pw.binom.logger.infoSync


class MercuryDemoApplication : Application() {
    private val logger by Logger.ofThisOrGlobal
    override fun onCreate() {
        super.onCreate()
        Logger.global.handler = AndroidLogHandler
        logger.infoSync("Start Application")
        MercurySDK.init(this)
//        NetworkService.start(this)
    }



    override fun onTerminate() {
        logger.infoSync("Stop Application")
//        NetworkService.stop(this)
        super.onTerminate()
    }
}