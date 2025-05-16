package pw.binom.dto

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import pw.binom.logger.Logger
import pw.binom.logger.infoSync


class PairingRequest : BroadcastReceiver() {
    private val logger by Logger.ofThisOrGlobal
    override fun onReceive(context: Context?, intent: Intent) {
        if (intent.action == "android.bluetooth.device.action.PAIRING_REQUEST") {
            try {
                val device =
                    intent.getParcelableExtra<BluetoothDevice?>(BluetoothDevice.EXTRA_DEVICE)
                val pin = intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", 0)
                //the pin in case you need to accept for an specific pin
                logger.infoSync("PIN: $pin")
                //maybe you look for a name or address
                logger.infoSync("Bonded: ${device!!.getName()}")
                val pinBytes = pin.toString().encodeToByteArray()
                device.setPin(pinBytes)
                //setPairing confirmation if neeeded
                device.setPairingConfirmation(true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}