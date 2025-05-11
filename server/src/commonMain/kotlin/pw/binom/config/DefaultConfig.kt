package pw.binom.config

import pw.binom.controllers.ClientController
import pw.binom.controllers.GlassesWsController
import pw.binom.services.GlassesService
import pw.binom.strong.Strong
import pw.binom.strong.bean
import pw.binom.strong.properties.StrongProperties
import pw.binom.strong.web.server.WebConfig

fun DefaultConfig(config: StrongProperties) = Strong.config {
    it.bean { GlassesWsController() }
    it.bean { GlassesService() }
    it.bean { ClientController() }
}