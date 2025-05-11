package pw.binom.dto

import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.net.toFile
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.rayneo.arsdk.android.demo.runOnUi
import com.rayneo.arsdk.android.demo.runOnUiAsync
import pw.binom.PlaybackControl
import pw.binom.logger.Logger
import pw.binom.logger.info
import pw.binom.logger.infoSync
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

class VideoPlaybackControl(val player: Player, val videoFilesRoot: File) : PlaybackControl {
    private val logger by Logger.ofThisOrGlobal
    override suspend fun getCurrentFile(): String? =
        runOnUiAsync { player.currentMediaItem?.mediaMetadata?.mediaUri?.toFile()?.name }

    override suspend fun getCurrentTime(): Long =
        runOnUiAsync { player.currentPosition }

    override suspend fun isPlaying() = runOnUiAsync { player.isPlaying }

    override suspend fun getFileLength(): Long =
        runOnUiAsync { player.contentDuration }

    override fun open(file: String, time: Long) {
        logger.infoSync("Opening file $file on time ${time.milliseconds}")
        runOnUi {
            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(videoFilesRoot.resolve(file))))
        }
    }

    override fun play(time: Long) {
        runOnUi {
            player.seekTo(time)
            player.play()
        }
    }

    override fun pause(time: Long) {
        runOnUi {
            player.pause()
            player.seekTo(time)
        }
    }

    override fun jump(time: Long) {
        runOnUi {
            player.seekTo(time)
        }
    }
}