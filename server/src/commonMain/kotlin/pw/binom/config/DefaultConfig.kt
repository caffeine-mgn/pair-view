package pw.binom.config

import pw.binom.controllers.GlassesWsController
import pw.binom.services.DevicesService
import pw.binom.strong.Strong
import pw.binom.strong.bean
import pw.binom.strong.properties.StrongProperties

fun DefaultConfig(config: StrongProperties) = Strong.config {
    it.bean { GlassesWsController() }
    it.bean { DevicesService() }
//    it.bean { ClientController() }
//    it.bean { ControlListener() }
//    it.bean { DeviceMessageListener() }
}