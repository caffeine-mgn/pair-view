package com.rayneo.arsdk.android.demo
/*
import android.os.Handler
import android.view.Surface
import com.google.android.exoplayer2.Renderer
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.audio.AudioRendererEventListener
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer
import com.google.android.exoplayer2.metadata.MetadataOutput
import com.google.android.exoplayer2.metadata.MetadataRenderer
import com.google.android.exoplayer2.text.TextOutput
import com.google.android.exoplayer2.text.TextRenderer
import com.google.android.exoplayer2.video.VideoRendererEventListener
import com.google.android.exoplayer2.video.spherical.CameraMotionRenderer

class MultiSurfaceRendererFactory(
    private val surfaces: List<Surface>
) : RenderersFactory {
    override fun createRenderers(
        eventHandler: Handler,
        videoRendererEventListener: VideoRendererEventListener,
        audioRendererEventListener: AudioRendererEventListener,
        textRendererOutput: TextOutput,
        metadataRendererOutput: MetadataOutput
    ): Array<Renderer> =
        arrayOf(
            MultiSurfaceRenderer(surfaces),
            MediaCodecAudioRenderer(context, MediaCodecSelector.DEFAULT),
            TextRenderer(textRendererOutput, eventHandler.looper),
            MetadataRenderer(metadataRendererOutput, eventHandler.looper),
            CameraMotionRenderer(cameraMotionListener),
            EmptyRenderer()
        )
}
*/