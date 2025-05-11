package com.rayneo.arsdk.android.demo.ui.activity

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import pw.binom.video.R
import pw.binom.video.databinding.MyTestActivityBinding
import com.rayneo.arsdk.android.demo.ui.onCreated
import com.rayneo.arsdk.android.touch.TempleAction
import com.rayneo.arsdk.android.ui.activity.BaseEventActivity
import com.rayneo.arsdk.android.util.FLogger
import kotlinx.coroutines.launch

class MyTestActivity : BaseEventActivity() {

    private var firstInited = false
    private var secondInited = false

    val pathStr = "/storage/self/primary/DCIM/Camera/VID_20250404_124518.mp4"
    lateinit var surfaceFirst: SurfaceView
    lateinit var surfaceSecond: SurfaceView
    private var mediaPlayer: MediaPlayer? = null

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
            return
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                templeActionViewModel.state.collect {
                    event(it)
                }
            }
        }

        val root = LinearLayout(this)
        surfaceFirst = SurfaceView(this)
        surfaceSecond = SurfaceView(this)

        surfaceFirst.holder.onCreated {
            firstInited = true
            tryInitMediaPlayer()
        }

        surfaceFirst.holder.onCreated {
            secondInited = true
            tryInitMediaPlayer()
        }

        root.addView(surfaceFirst)
        root.addView(surfaceSecond)
        setContentView(root)


    }

    private fun tryInitMediaPlayer() {
        if (!firstInited || !secondInited) {
            return
        }
        val mediaPlayer = MediaPlayer().apply {
            setDataSource(pathStr) // или ресурс (R.raw.video)
            setDisplay(surfaceFirst.holder)
            prepareAsync()
            setOnPreparedListener { start() }
        }
        this.mediaPlayer = mediaPlayer
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.stop()
        mediaPlayer?.release()
    }

    private fun event(event: TempleAction) {
        FLogger.i("DemoActivity", "action = $event")
        when (event) {
            is TempleAction.DoubleClick -> {
                finish()
            }

            else -> {

            }
        }
    }
}