package pw.binom.dto

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.rayneo.arsdk.android.demo.ui.activity.VideoPlayerActivity
import kotlinx.serialization.protobuf.ProtoBuf
import pw.binom.DeviceClient.DeviceEventProcessor
import pw.binom.LocalFileSystemManager
import pw.binom.logger.Logger
import pw.binom.logger.infoSync

class ServiceDeviceEventProcessor(
    val context: Context,
    val config: Config,
) : DeviceEventProcessor {
    private val fileSystemManager = LocalFileSystemManager(context.obbDir)
    private val logger by Logger.ofThisOrGlobal

    fun reg() {
        exchanger.reg()
    }

    fun unreg() {
        exchanger.unreg()
    }

    private suspend fun sendAndReceive(cmd: RRequest): RResponse {
        val request = ProtoBuf.encodeToByteArray(RRequest.serializer(), cmd)
        val response = exchanger.sendAndReceive(request)
        return ProtoBuf.decodeFromByteArray(RResponse.serializer(), response)
    }

    private val exchanger = ExchangeService(
        context = context,
        broadcastChannel = Channels.VIDEO_ACTIVITY,
        selfChannel = Channels.NETWORK_SERVICE,
    ) { data ->
        ByteArray(0)
    }

    suspend fun processing(request: ControlRequestDto): ControlResponseDto =
        when (request) {
            is ControlRequestDto.Play -> {
                if (!VideoPlayerActivity.isActive) {
                    ControlResponseDto.Error("Video file not opened")
                } else {
                    sendAndReceive(RRequest.Play(request.time))
                    ControlResponseDto.OK
                }
            }

            is ControlRequestDto.Pause -> {
                if (!VideoPlayerActivity.isActive) {
                    ControlResponseDto.Error("Video file not opened")
                } else {
                    sendAndReceive(RRequest.Pause(request.time))
                    ControlResponseDto.OK
                }
            }

            is ControlRequestDto.Seek -> {
                if (!VideoPlayerActivity.isActive) {
                    ControlResponseDto.Error("Video file not opened")
                } else {
                    sendAndReceive(RRequest.Seek(request.time))
                    ControlResponseDto.OK
                }
            }

            is ControlRequestDto.SeekDelta -> {
                if (!VideoPlayerActivity.isActive) {
                    ControlResponseDto.Error("Video file not opened")
                } else {
                    sendAndReceive(RRequest.SeekDelta(request.time))
                    ControlResponseDto.OK
                }
            }

            is ControlRequestDto.OpenVideoFile -> {
                if (VideoPlayerActivity.isActive) {
                    sendAndReceive(
                        RRequest.OpenVideoFile(
                            fileName = request.fileName,
                            time = request.time,
                        )
                    )
                    ControlResponseDto.OK
                } else {
                    val intent = Intent(context, VideoPlayerActivity::class.java)
                    val b = Bundle()
                    b.putString("file", request.fileName)
                    b.putLong("time", request.time.inWholeSeconds)
                    intent.putExtras(b)
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent)
                    ControlResponseDto.OK
                }
            }

            is ControlRequestDto.GetState -> {
                if (VideoPlayerActivity.isActive) {
                    ControlResponseDto.CurrentState.Video(
                        isPlaying = VideoPlayerActivity.playingState,
                        fileName = VideoPlayerActivity.currentPlayingFile,
                        currentPosition = VideoPlayerActivity.currentPlayingTime,
                        totalDuration = VideoPlayerActivity.currentPlayingTime,
                    )
                } else {
                    ControlResponseDto.CurrentState.NoneState
                }
            }

            is ControlRequestDto.GetAvailableVideoFiles -> {
                ControlResponseDto.Files(fileSystemManager.getFiles())
            }

//            else -> ControlResponseDto.Error("Unknown message")
            ControlRequestDto.CloseCurrentView -> ControlResponseDto.OK
        }

    override suspend fun processing(msg: ByteArray): ByteArray {
        val request = ProtoBuf.decodeFromByteArray(ControlRequestDto.serializer(), msg)
        logger.infoSync("Income $request")
        val response = processing(request)
        logger.infoSync("Outcome $response")
        return ProtoBuf.encodeToByteArray(ControlResponseDto.serializer(), response)
    }
}