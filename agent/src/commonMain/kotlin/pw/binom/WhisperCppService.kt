package pw.binom

import io.github.givimad.whisperjni.WhisperFullParams
import io.github.givimad.whisperjni.WhisperJNI
import java.lang.System.getProperty
import java.nio.file.Path
import kotlin.io.path.Path

/*
class WhisperCppService {
    init {
        WhisperJNI.loadLibrary() // load platform binaries
        WhisperJNI.setLibraryLogger(null) // capture/disable whisper.cpp log
        val whisper = WhisperJNI()
        val samples: FloatArray = readJFKFileSamples()
        val ctx = whisper.init(Path.of(getProperty("user.home"), "ggml-tiny.bin"))
        val params = WhisperFullParams()
        val result = whisper.full(ctx, params, samples, samples.size)
        if (result != 0) {
            throw RuntimeException("Transcription failed with code " + result)
        }
        val numSegments = whisper.fullNSegments(ctx)
        assertEquals(1, numSegments)
        val text = whisper.fullGetSegmentText(ctx, 0)
        assertEquals(
            " And so my fellow Americans ask not what your country can do for you ask what you can do for your country.",
            text
        )
        ctx.close() // free native memory, should be called when we don't need the context anymore.
    }
}
*/