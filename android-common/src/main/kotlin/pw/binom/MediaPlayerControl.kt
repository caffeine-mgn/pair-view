package pw.binom

import android.media.MediaPlayer
import kotlinx.coroutines.suspendCancellableCoroutine
import pw.binom.io.Closeable
import pw.binom.logger.infoSync
import java.io.File
import kotlin.collections.minusAssign
import kotlin.collections.plusAssign
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalAtomicApi::class)
class MediaPlayerControl : MediaControl, Closeable {

    private val prepared = OneShortListeners<Unit>()
    private val seekEnd = OneShortListeners<Unit>()
    private val error = OneShortListeners<Unit>()
    private val completion = OneShortListeners<Unit>()

    override val currentTime: Duration
        get() = currentPlayer.load()?.currentPosition?.milliseconds ?: Duration.ZERO
    override val totalDuration: Duration
        get() = currentPlayer.load()?.duration?.milliseconds ?: Duration.ZERO
    override val isPlaying: Boolean
        get() = currentPlayer.load()?.isPlaying ?: false
    override val currentFile: File?
        get() = internalCurrentFile.load()

    private val commitedListeners = HashSet<() -> Unit>()
    private val openListeners = HashSet<(String) -> Unit>()
    private val playStatusListeners = HashSet<(Boolean, Duration) -> Unit>()
    private val seekListeners = HashSet<(Duration) -> Unit>()

    private fun setEventListeners(player: MediaPlayer) {
        player.setOnPreparedListener {
            prepared.fire(Unit)
        }
        player.setOnSeekCompleteListener {
            seekEnd.fire(Unit)
            seekListeners.forEach {
                it(player.currentPosition.milliseconds)
            }
        }
        player.setOnCompletionListener {
            completion.fire(Unit)
            commitedListeners.forEach {
                it()
            }
        }
        player.setOnErrorListener { mp, what, extra ->
            error.fire(Unit)
            true
        }
    }

    private var internalCurrentFile = AtomicReference<File?>(null)
    private var currentPlayer = AtomicReference<MediaPlayer?>(null)

    override suspend fun open(file: File) {
        if (internalCurrentFile.load() != null) {
            currentPlayer.load()?.also {
                it.stop()
                it.release()
            }
        }
        suspendCancellableCoroutine<Unit> { con ->
            val w = EventWaiter()
            con.invokeOnCancellation {
                w.cancel()
            }
            w.wait(prepared) { con.resume(Unit) }
            w.wait(error) { con.resumeWithException(IllegalStateException("Can't open file")) }
            val player = MediaPlayer()
            currentPlayer.store(player)
            setEventListeners(player)
            player.setDataSource(file.absolutePath)
            player.prepareAsync()
        }
        internalCurrentFile.store(file)
        openListeners.forEach {
            it(file.name)
        }
    }

    override suspend fun play() {
        val currentPlayer =
            currentPlayer.load() ?: throw IllegalStateException("No file for replay")
        currentPlayer.start()
        playStatusListeners.forEach {
            it(true, currentPlayer.currentPosition.milliseconds)
        }
    }

    override suspend fun pause() {
        val currentPlayer =
            currentPlayer.load() ?: throw IllegalStateException("No file for replay")
        currentPlayer.pause() ?: throw IllegalStateException("No file for replay")
        playStatusListeners.forEach {
            it(false, currentPlayer.currentPosition.milliseconds)
        }
    }

    override suspend fun seek(time: Duration) {
        val currentPlayer =
            currentPlayer.load() ?: throw IllegalStateException("No file for replay")
        suspendCancellableCoroutine<Unit> { con ->
            val w = EventWaiter()
            con.invokeOnCancellation {
                w.cancel()
            }
            w.wait(seekEnd) {
                con.resume(Unit)
            }
            w.wait(error) { con.resumeWithException(IllegalStateException("Can't seek")) }
            currentPlayer.seekTo(time.inWholeMilliseconds.toInt())
        }
    }

    override fun addOpenListener(func: (String) -> Unit): Closeable {
        openListeners += func
        return Closeable {
            openListeners -= func
        }
    }

    override fun addCommitedListener(func: () -> Unit): Closeable {
        commitedListeners += func
        return Closeable {
            commitedListeners -= func
        }
    }

    override fun addPlayingChangeListener(func: (Boolean, Duration) -> Unit): Closeable {
        playStatusListeners += func
        return Closeable {
            playStatusListeners -= func
        }
    }

    override fun addSeekListener(func: (Duration) -> Unit): Closeable {
        seekListeners += func
        return Closeable {
            seekListeners -= func
        }
    }

    override fun close() {
        currentPlayer.load()?.also {
            it.stop()
            it.release()
        }
    }
}