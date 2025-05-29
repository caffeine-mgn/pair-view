package pw.binom

import android.app.Application
import pw.binom.logger.Logger

class PhoneApplication : Application() {
    override fun onCreate() {
        Logger.global.handler = AndroidLogHandler
        super.onCreate()
    }
}