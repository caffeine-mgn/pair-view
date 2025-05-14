package pw.binom

import java.io.File

class LocalFileSystemManager(root: File) : FileSystemManager {
    private val videoFiles = root.resolve("video")

    init {
        videoFiles.mkdirs()
    }

    override fun getFiles(): List<String> =
        videoFiles.listFiles().filter { it.isFile }.map { it.name }
}