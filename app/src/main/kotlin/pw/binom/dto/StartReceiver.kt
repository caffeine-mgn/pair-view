package pw.binom.dto

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import pw.binom.logger.Logger
import pw.binom.logger.infoSync

class StartReceiver : BroadcastReceiver() {
    private val logger by Logger.ofThisOrGlobal
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED /*&& getServiceState(context) == ServiceState.STARTED*/) {
            Intent(context, NetworkService::class.java).also {
                it.action = Actions.START.name
                logger.infoSync("Starting the service in >=26 Mode from a BroadcastReceiver")
                context.startForegroundService(it)
                return
//                logger.infoSync("Starting the service in < 26 Mode from a BroadcastReceiver")
//                context.startService(it)
            }
        }
    }
}