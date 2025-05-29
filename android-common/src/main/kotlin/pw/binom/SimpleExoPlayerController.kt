package pw.binom

import android.net.Uri
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import pw.binom.io.Closeable
import pw.binom.logger.Logger
import pw.binom.logger.infoSync
import pw.binom.logger.warnSync
import java.io.File
import java.io.FileNotFoundException
import kotlin.collections.minusAssign
import kotlin.collections.plusAssign
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalAtomicApi::class)
class SimpleExoPlayerController(val player: SimpleExoPlayer) : MediaControl {
    private val logger by Logger.ofThisOrGlobal
    override val currentTime: Duration
        get() = runBlocking {
            runOnUiAsync {
                player.currentPosition.milliseconds
            }
        }
    override val totalDuration: Duration
        get() = runBlocking {
            runOnUiAsync {
                player.duration.milliseconds
            }
        }
    override val isPlaying: Boolean
        get() = runBlocking {
            runOnUiAsync {
                player.isPlaying
            }
        }
    override val currentFile: File?
        get() = internalCurrentFile.load()
    private val commitedListeners = HashSet<() -> Unit>()

    private val ready = OneShortListeners<Unit>()
    private val error = OneShortListeners<Unit>()
    private val playing = OneShortListeners<Boolean>()
    private val completion = OneShortListeners<Unit>()
    private var internalCurrentFile = AtomicReference<File?>(null)
    private val playStatusListeners = HashSet<(Boolean, Duration) -> Unit>()
    private val seekListeners = HashSet<(Duration) -> Unit>()
    private val openListeners = HashSet<(String) -> Unit>()

    private val listener = object : Player.Listener {
        override fun onSeekProcessed() {
            super.onSeekProcessed()
        }

        override fun onEvents(
            player: Player,
            events: Player.Events,
        ) {
            val eventsStr = (0 until events.size()).map {
                events.get(it)
            }.joinToString()
            logger.infoSync("onEvents: $events -> $eventsStr")
            super.onEvents(player, events)
        }

        override fun onPlayerError(error: PlaybackException) {
            logger.infoSync("onPlayerError: $error")
            this@SimpleExoPlayerController.error.fire(Unit)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            logger.infoSync("onPlaybackStateChanged: playbackState=$playbackState")
            if (playbackState == ExoPlayer.STATE_READY) {
                ready.fire(Unit)
            }
            if (playbackState == ExoPlayer.STATE_ENDED) {
                completion.fire(Unit)
                commitedListeners.forEach {
                    it()
                }
            }
        }

        override fun onIsLoadingChanged(isLoading: Boolean) {
            logger.infoSync("onIsLoadingChanged: isLoading=$isLoading")
            if (!isLoading) {
                ready.fire(Unit)
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            logger.infoSync("onIsPlayingChanged: isPlaying=$isPlaying")
            playing.fire(isPlaying)
            val currentPosition = player.currentPosition.milliseconds
            playStatusListeners.forEach {
                it(isPlaying, currentPosition)
            }
        }

        override fun onRenderedFirstFrame() {
            logger.infoSync("onRenderedFirstFrame")
            ready.fire(Unit)
        }
    }

    init {
        player.addListener(listener)
//        player.playWhenReady = true
    }


    override suspend fun open(file: File) {
        if (!file.isFile) {
            throw FileNotFoundException("File $file not found")
        }
        suspendCancellableCoroutine<Unit> { con ->
            val w = EventWaiter()
            con.invokeOnCancellation {
                w.cancel()
            }
            w.wait(ready) { con.resume(Unit) }
            w.wait(error) { con.resumeWithException(IllegalStateException("Can't open file")) }
            try {
                runOnUi {
                    player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
                    player.prepare()
                    player.play()
                }
            } catch (e: Throwable) {
                logger.warnSync(text = "Can't open file", exception = e)
            }
        }
        internalCurrentFile.store(file)
        openListeners.forEach {
            it(file.name)
        }
    }

    override suspend fun play() {
        runOnUi {
            player.play()
        }
    }

    override suspend fun pause() {
        runOnUi {
            player.pause()
        }
    }

    override suspend fun seek(time: Duration) {
        suspendCancellableCoroutine<Unit> { con ->
            val w = EventWaiter()
            con.invokeOnCancellation {
                w.cancel()
            }
            w.wait(ready) { con.resume(Unit) }
            w.wait(error) { con.resumeWithException(IllegalStateException("Can't open file")) }
            runOnUi {
                player.seekTo(time.inWholeMilliseconds)
            }
            seekListeners.forEach {
                it(time)
            }
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
}