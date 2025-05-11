package pw.binom.controllers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import pw.binom.dto.GlassesDto
import pw.binom.dto.JumpDto
import pw.binom.dto.OpenFileDto
import pw.binom.dto.UpdateViewDto
import pw.binom.io.httpServer.HttpHandler
import pw.binom.io.httpServer.HttpServerExchange
import pw.binom.io.httpServer.HttpServerResponse
import pw.binom.services.GlassesService
import pw.binom.strong.inject
import pw.binom.url.toPathMask

class ClientController : HttpHandler {
    private val glassesService: GlassesService by inject()
    private val actionsPlay = "/api/clients/{id}/actions/play".toPathMask()
    private val actionsPause = "/api/clients/{id}/actions/pause".toPathMask()
    private val actionsOpenFile = "/api/clients/{id}/actions/openFile".toPathMask()
    private val actionsSeek = "/api/clients/{id}/actions/seek".toPathMask()
    private val getFiles = "/api/clients/{id}/files".toPathMask()
    private val updateView = "/api/clients/{id}/config/view".toPathMask()
    override suspend fun handle(exchange: HttpServerExchange) {
        when {
            exchange.requestMethod == "GET" && exchange.requestURI.isMatch("/api/clients") -> getClients(
                exchange
            )

            exchange.requestMethod == "POST" && exchange.requestURI.path.isMatch(actionsPlay) -> actionPlay(
                clientId = exchange.requestURI.path.getVariables(actionsPlay)!!["id"]!!,
                exchange = exchange,
            )

            exchange.requestMethod == "POST" && exchange.requestURI.path.isMatch(actionsPause) -> actionPause(
                clientId = exchange.requestURI.path.getVariables(actionsPause)!!["id"]!!,
                exchange = exchange,
            )

            exchange.requestMethod == "GET" && exchange.requestURI.path.isMatch(getFiles) -> getFiles(
                clientId = exchange.requestURI.path.getVariables(getFiles)!!["id"]!!,
                exchange = exchange,
            )

            exchange.requestMethod == "POST" && exchange.requestURI.path.isMatch(actionsOpenFile) -> openFile(
                clientId = exchange.requestURI.path.getVariables(actionsOpenFile)!!["id"]!!,
                exchange = exchange,
            )

            exchange.requestMethod == "POST" && exchange.requestURI.path.isMatch(actionsSeek) -> seek(
                clientId = exchange.requestURI.path.getVariables(actionsSeek)!!["id"]!!,
                exchange = exchange,
            )

            exchange.requestMethod == "POST" && exchange.requestURI.path.isMatch(updateView) -> updateView(
                clientId = exchange.requestURI.path.getVariables(updateView)!!["id"]!!,
                exchange = exchange,
            )
        }
    }

    suspend fun getClients(exchange: HttpServerExchange) {
        val glasses = glassesService.glasses.map {
            GlassesDto(
                id = it.id,
                name = it.name,
            )
        }
        exchange.response().also {
            it.sendJson(ListSerializer(GlassesDto.serializer()), glasses)
        }
    }

    suspend fun getFiles(clientId: String, exchange: HttpServerExchange) {
        val glasses = glassesService.findById(clientId)
        if (glasses == null) {
            exchange.response().also {
                it.status = 400
                it.send("Glasses with id $clientId not found")
                return
            }
        }
        exchange.response().also {
            it.sendJson(ListSerializer(String.serializer()), glasses.getFiles())
        }
    }

    suspend fun seek(clientId: String, exchange: HttpServerExchange) {
        val glasses = glassesService.findById(clientId)
        if (glasses == null) {
            exchange.response().also {
                it.status = 400
                it.send("Glasses with id $clientId not found")
                return
            }
        }
        val dto = Json.decodeFromString(JumpDto.serializer(), exchange.readAllText())
        glasses.seek(time = dto.time)
        exchange.startResponse(200)
    }

    suspend fun openFile(clientId: String, exchange: HttpServerExchange) {
        val glasses = glassesService.findById(clientId)
        if (glasses == null) {
            exchange.response().also {
                it.status = 400
                it.send("Glasses with id $clientId not found")
                return
            }
        }
        val dto = Json.decodeFromString(OpenFileDto.serializer(), exchange.readAllText())
        glasses.play(file = dto.name, time = 0)
        exchange.startResponse(200)
    }

    suspend fun actionPlay(clientId: String, exchange: HttpServerExchange) {
        val glasses = glassesService.findById(clientId)
        if (glasses == null) {
            exchange.response().also {
                it.status = 400
                it.send("Glasses with id $clientId not found")
                return
            }
        }
        val state = glasses.getState()
        glasses.play(time = state.time)
        exchange.startResponse(200)
    }

    suspend fun actionPause(clientId: String, exchange: HttpServerExchange) {
        val glasses = glassesService.findById(clientId)
        if (glasses == null) {
            exchange.response().also {
                it.status = 400
                it.send("Glasses with id $clientId not found")
                return
            }
        }

        val state = glasses.getState()
        glasses.pause(time = state.time)
        exchange.startResponse(200)
    }

    suspend fun updateView(clientId: String, exchange: HttpServerExchange) {
        val glasses = glassesService.findById(clientId)
        if (glasses == null) {
            exchange.response().also {
                it.status = 400
                it.send("Glasses with id $clientId not found")
                return
            }
        }

        val dto = Json.decodeFromString(UpdateViewDto.serializer(), exchange.readAllText())
        glasses.updateView(
            padding = dto.padding,
            align = dto.align,
        )
        exchange.startResponse(200)
    }
}

suspend fun <T> HttpServerResponse.sendJson(
    serializer: KSerializer<T>,
    value: T,
    json: Json = Json
) {
    status = 200
    headers.contentType = "application/json"
    val body = json.encodeToString(serializer, value)
    send(body)
}