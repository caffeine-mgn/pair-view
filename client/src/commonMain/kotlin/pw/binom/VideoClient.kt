package pw.binom

interface VideoClient {
    interface Handler {
        suspend fun getCurrentPlayingFile(): String?
        suspend fun getCurrentPlayingTime(): Long
        suspend fun isPlayingStatus(): Boolean
        suspend fun openFile(name: String, time: Long)
        suspend fun seek(time: Long)
        suspend fun play(time:Long)
        suspend fun pause(time:Long)
        suspend fun getLocalFiles(): List<String>
        suspend fun updateView(padding: Int, align: Int)
    }
}