package pw.binom

class VideoClientHandlerImpl(
    val fileSystemManager: FileSystemManager,
    val playbackControl: PlaybackControl,
    val viewManager: ViewManager,
) : VideoClient.Handler {
    override suspend fun getCurrentPlayingFile(): String? = playbackControl.getCurrentFile()

    override suspend fun getCurrentPlayingTime(): Long = playbackControl.getCurrentTime()

    override suspend fun isPlayingStatus(): Boolean = playbackControl.isPlaying()

    override suspend fun openFile(name: String, time: Long) {
        playbackControl.open(name, time)
    }

    override suspend fun seek(time: Long) {
        playbackControl.jump(time)
    }

    override suspend fun play(time: Long) {
        playbackControl.play(time)
    }

    override suspend fun pause(time: Long) {
        playbackControl.pause(time)
    }

    override suspend fun getLocalFiles(): List<String> =
        fileSystemManager.getFiles()

    override suspend fun updateView(padding: Int, align: Int) {
        viewManager.update(padding = padding, align = align)
    }
}