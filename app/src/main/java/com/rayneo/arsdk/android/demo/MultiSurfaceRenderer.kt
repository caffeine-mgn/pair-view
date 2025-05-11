package com.rayneo.arsdk.android.demo
/*
import android.view.Surface
import com.google.android.exoplayer2.BaseRenderer
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Renderer
import com.google.android.exoplayer2.video.VideoDecoderOutputBufferRenderer

class MultiSurfaceRenderer(
    private val surfaces: List<Surface>
) : BaseRenderer(C.TRACK_TYPE_VIDEO){
    private var inputFormat: Format? = null
    private var decoder: VideoDecoderOutputBufferRenderer? = null

    override fun getName() = "MultiSurfaceRenderer"

    override fun supportsFormat(format: Format) = true

    override fun render(positionUs: Long, elapsedRealtimeUs: Long) {
        // Рендеринг в все поверхности
        surfaces.forEach { surface ->
            decoder?.
            decoder?.renderToSurface(surface)
        }
    }

    override fun handleMessage(messageType: Int, message: Any) {
        when (messageType) {
            MSG_SET_VIDEO_OUTPUT -> {
                decoder = message as? VideoDecoderOutputBufferRenderer
            }
        }
    }
}
*/