package pw.binom.dto

import android.util.Log
import pw.binom.logger.DEBUG
import pw.binom.logger.INFO
import pw.binom.logger.Logger
import pw.binom.logger.SEVERE
import pw.binom.logger.WARNING

object AndroidLogHandler : Logger.Handler {
    override fun logSync(
        logger: Logger,
        level: Logger.Level,
        text: String?,
        trace: String?,
        exception: Throwable?
    ) {
        when {
            level.priority >= Logger.SEVERE.priority -> Log.e(logger.pkg, text ?: "", exception)
            level.priority >= Logger.WARNING.priority -> Log.w(logger.pkg, text ?: "", exception)
            level.priority >= Logger.INFO.priority -> Log.i(logger.pkg, text ?: "", exception)
            level.priority >= Logger.DEBUG.priority -> Log.d(logger.pkg, text ?: "", exception)
            else -> Log.v(logger.pkg, text ?: "", exception)
        }
    }
}