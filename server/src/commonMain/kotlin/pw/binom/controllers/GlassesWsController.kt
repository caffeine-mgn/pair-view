package pw.binom.controllers

import pw.binom.dto.Headers
import pw.binom.io.httpServer.HttpHandler
import pw.binom.io.httpServer.HttpServerExchange
import pw.binom.io.httpServer.acceptWebsocket
import pw.binom.logger.Logger
import pw.binom.logger.info
import pw.binom.services.DevicesService
import pw.binom.strong.inject

class GlassesWsController : HttpHandler {
    private val devicesService: DevicesService by inject()
    private val logger by Logger.ofThisOrGlobal
    override suspend fun handle(exchange: HttpServerExchange) {
        logger.info("Income request ${exchange.requestMethod} ${exchange.requestURI}")
        if (exchange.requestMethod != "GET") {
            return
        }
        if (!exchange.requestURI.path.isMatch("/api/glasses")){
            return
        }
        println("exchange.requestHeaders:")
        println(exchange.requestHeaders)
        val deviceId = exchange.requestHeaders.getSingleOrNull(Headers.DEVICE_ID)
        val deviceName = exchange.requestHeaders.getSingleOrNull(Headers.DEVICE_NAME)
        logger.info("Income connection:\n  deviceId: $deviceId\n  deviceName: ${deviceName}")
        if (deviceId == null || deviceName == null) {
            exchange.response().also {
                it.status = 400
                it.headers.contentType = "text/plain"
                it.send("Invalid connection params:\n\ndeviceId=$deviceId\ndeviceName=$deviceName")
            }
            return
        }
        val client = exchange.acceptWebsocket()
        try {
            devicesService.processing(
                deviceId = deviceId,
                deviceName = deviceName,
                connection = client,
            )
        } finally {
            client.asyncCloseAnyway()
        }
    }
}