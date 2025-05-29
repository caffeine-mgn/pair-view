package pw.binom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import pw.binom.logger.Logger
import pw.binom.logger.infoSync

class MediaReceiver : BroadcastReceiver() {
    private val logger by Logger.ofThisOrGlobal
    override fun onReceive(context: Context?, intent: Intent) {
        logger.infoSync("EVENT!!! action: ${intent.action}")
    }
}