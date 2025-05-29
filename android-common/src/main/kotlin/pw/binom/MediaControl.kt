package pw.binom

import pw.binom.io.Closeable
import java.io.File
import kotlin.time.Duration

interface MediaControl {
    val currentTime: Duration
    val totalDuration: Duration
    val isPlaying: Boolean
    val currentFile: File?
    suspend fun open(file: File)
    suspend fun play()
    suspend fun pause()
    suspend fun seek(time: Duration)

    fun addOpenListener(func: (String) -> Unit): Closeable
    fun addCommitedListener(func: () -> Unit): Closeable
    fun addPlayingChangeListener(func: (playing: Boolean, time: Duration) -> Unit): Closeable
    fun addSeekListener(func: (time: Duration) -> Unit): Closeable
}