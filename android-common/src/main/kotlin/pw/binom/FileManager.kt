package pw.binom

import android.content.Context

class FileManager(context: Context) {
    val mediaRoot = context.obbDir.resolve("media")
    val downloading = context.obbDir.resolve("downloading")

    fun resolve(name: String) = mediaRoot.resolve(name)

    val files
        get() = mediaRoot.takeIf { it.isDirectory }
            ?.listFiles()?.toList()
            ?: emptyList()
}