package com.rayneo.arsdk.android.demo.ui.activity

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.View
import androidx.core.app.ActivityCompat
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import pw.binom.video.databinding.VideoPlayerBinding
import com.rayneo.arsdk.android.ui.activity.BaseMirrorActivity
import java.io.File


class VideoPlayerActivity : BaseMirrorActivity<VideoPlayerBinding>() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var surfaceHolder: SurfaceHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissionGranted1 =
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        val permissionGranted2 =
            checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        if (!permissionGranted1 || !permissionGranted2) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                1
            )
        }

//        val pathStr = "/storage/self/primary/DCIM/Camera/VID_20250404_124518.mp4"
        val pathStr = "/storage/emulated/0/videos/test.mp4"
        val path = Uri.fromFile(File(pathStr))
        mBindingPair.setLeft {
            val mediaItem = MediaItem.fromUri(path)

            val rf: DefaultRenderersFactory = DefaultRenderersFactory(
                this@VideoPlayerActivity,
            )
            rf.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
//            val player = ExoPlayerFactory . newSimpleInstance (rf, DefaultTrackSelector(), DefaultLoadControl())
            videoView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            videoView.player = ExoPlayer.Builder(this@VideoPlayerActivity).setRenderersFactory(rf).build()
            videoView.player!!.setMediaItem(mediaItem)
            videoView.player!!.prepare()
            videoView.player!!.playWhenReady = true
//            surfaceHolder=videoView.holder
//            videoView.holder.addCallback(
//                object : SurfaceHolder.Callback {
//                    override fun surfaceCreated(p0: SurfaceHolder) {
//                        videoView.requestFocus()
//                        mediaPlayer = MediaPlayer().apply {
//                            setDataSource(pathStr) // или ресурс (R.raw.video)
//                            setDisplay(surfaceHolder)
//                            prepareAsync()
//                            setOnPreparedListener { start() }
//                        }
//                    }
//
//                    override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
//
//                    }
//
//                    override fun surfaceDestroyed(p0: SurfaceHolder) {
//                        mediaPlayer.release()
//                    }
//
//                }
//            )
        }

    }

    override fun onPause() {
        super.onPause()
        mediaPlayer.release()
    }
}