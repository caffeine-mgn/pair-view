package pw.binom

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import pw.binom.dto.GlassesDto
import pw.binom.dto.JumpDto
import pw.binom.dto.OpenFileDto
import pw.binom.dto.PlaybackState
import pw.binom.dto.Response
import pw.binom.dto.UpdateViewDto
import pw.binom.url.toURI

object Api {
    suspend fun getClients() = Network.get(
        url = "/api/clients".toURI(),
        responseSerializer = Response.serializer(ListSerializer(GlassesDto.serializer()))
    )

    suspend fun play(id: String) {
        Network.post(
            url = "/api/clients/${id}/actions/play".toURI(),
            request = Unit,
            requestSerializer = Unit.serializer(),
            responseSerializer = Response.serializer(Unit.serializer())
        )
    }

    suspend fun pause(id: String) {
        Network.post(
            url = "/api/clients/$id/actions/pause".toURI(),
            request = Unit,
            requestSerializer = Unit.serializer(),
            responseSerializer = Response.serializer(Unit.serializer())
        )
    }

    suspend fun getFiles(id: String) =
        Network.get(
            url = "/api/clients/$id/files".toURI(),
            responseSerializer = Response.serializer(ListSerializer(String.serializer()))
        )

    suspend fun getState(id: String) =
        Network.get(
            url = "/api/clients/$id/state".toURI(),
            responseSerializer = Response.serializer(PlaybackState.serializer())
        )

    suspend fun openFile(id: String, file: String) {
        Network.post(
            url = "/api/clients/$id/actions/openFile".toURI(),
            request = OpenFileDto(
                name = file
            ),
            requestSerializer = OpenFileDto.serializer(),
            responseSerializer = Response.serializer(Unit.serializer())
        )
    }

    suspend fun seek(id: String, position: Long) {
        Network.post(
            url = "/api/clients/$id/actions/openFile".toURI(),
            request = JumpDto(
                time = position
            ),
            requestSerializer = JumpDto.serializer(),
            responseSerializer = Response.serializer(Unit.serializer())
        )
    }

    suspend fun updateView(
        id: String,
        padding: Int,
        align: Int,
    ) {
        Network.post(
            url = "/api/clients/$id/actions/openFile".toURI(),
            request = UpdateViewDto(
                padding = padding,
                align = align,
            ),
            requestSerializer = UpdateViewDto.serializer(),
            responseSerializer = Response.serializer(Unit.serializer()),
        )
    }
}