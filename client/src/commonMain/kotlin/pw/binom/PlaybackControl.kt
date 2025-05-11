package pw.binom

interface PlaybackControl {
    suspend fun getCurrentFile(): String?
    suspend fun getCurrentTime(): Long
    suspend fun isPlaying(): Boolean
    suspend fun getFileLength(): Long

    fun open(file: String, time: Long)
    fun play(time: Long)
    fun pause(time: Long)
    fun jump(time: Long)
}