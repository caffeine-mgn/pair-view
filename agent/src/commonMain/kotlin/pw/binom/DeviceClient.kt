package pw.binom

import kotlinx.serialization.protobuf.ProtoBuf
import pw.binom.dto.ControlRequestDto
import pw.binom.dto.ControlResponseDto
import pw.binom.mq.nats.NatsMqConnection
import pw.binom.strong.inject

class DeviceClient {
    private val mq: NatsMqConnection by inject()

    suspend fun send(deviceId: String, data: ByteArray) =
        mq.sendAndReceive("devices.$deviceId.control") {
            it.send(data = data)
        }.data

    suspend fun send(deviceId: String, request: ControlRequestDto): ControlResponseDto {
        val resp = send(
            deviceId = deviceId,
            data = ProtoBuf.encodeToByteArray(ControlRequestDto.serializer(), request)
        )
        return ProtoBuf.decodeFromByteArray(ControlResponseDto.serializer(), resp)
    }
}