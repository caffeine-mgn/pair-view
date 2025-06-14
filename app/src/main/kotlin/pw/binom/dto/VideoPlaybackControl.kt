package pw.binom.dto
/*
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.net.toFile
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import pw.binom.logger.Logger
import pw.binom.logger.info
import pw.binom.logger.infoSync
import pw.binom.runOnUiAsync
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

class VideoPlaybackControl(val player: Player, val videoFilesRoot: File) : PlaybackControl {
    private val logger by Logger.ofThisOrGlobal
    var currentFile: String? = null
    override suspend fun getCurrentFile(): String? =
        runOnUiAsync { currentFile }

    override suspend fun getCurrentTime(): Long =
        runOnUiAsync { player.currentPosition }

    override suspend fun isPlaying() = runOnUiAsync { player.isPlaying }

    override suspend fun getFileLength(): Long =
        runOnUiAsync { player.contentDuration }

    override fun open(file: String, time: Long) {
        currentFile = file
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

 */