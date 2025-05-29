package com.rayneo.arsdk.android.demo.ui.activity

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.graphics.SurfaceTexture.OnFrameAvailableListener
import android.opengl.GLES20
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.rayneo.arsdk.android.demo.EglCore
import com.rayneo.arsdk.android.demo.FullFrameRect
import com.rayneo.arsdk.android.demo.Texture2dProgram
import com.rayneo.arsdk.android.demo.WindowSurface
import com.rayneo.arsdk.android.touch.TempleAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pw.binom.ContextVariableHolder
import pw.binom.EventBroadcast
import pw.binom.ExchangeService
import pw.binom.FileManager
import pw.binom.SimpleExoPlayerController
import pw.binom.glasses.AbstractVideoActivity
import pw.binom.glasses.Methods
import pw.binom.glasses.NetworkService
import pw.binom.glasses.REvent
import pw.binom.glasses.RResponse
import pw.binom.glasses.dto.GlassesResponse
import pw.binom.logger.infoSync
import kotlin.math.absoluteValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


class VideoPlayerActivity : AbstractVideoActivity() {
    companion object {
        const val VIDEO_PLAYER_CHANNEL = "VideoPlayer"
        const val ACTION = "action"
        const val ACTION_PLAY = "play"
        const val ACTION_PAUSE = "pause"
        const val ACTION_OPEN_FILE = "openFile"
        const val ACTION_SEEK = "seek"
        const val ACTION_SEEK_DELTA = "seek_delta"
        const val TIME = "time"
        const val FILE = "file"
        var currentPlayingFile: String? = null
            private set
        var currentPlayingTime = Duration.ZERO
            private set
        var currentTotalTime = Duration.ZERO
            private set
        var playingState = false
            private set
        var isActive = false
            private set
    }

    private val contextVariableHolder = ContextVariableHolder(this)

    private val eventPublisher by contextVariableHolder.define { ctx ->
        EventBroadcast.publisher(
            context = ctx,
            channel = NetworkService.EVENTS,
            serializer = REvent.serializer()
        )
    }

    private lateinit var playerView: PlayerView
    private lateinit var surfaceView: SurfaceView
    private lateinit var root: RelativeLayout

    private lateinit var player: SimpleExoPlayer // com.google.android.exoplayer2.ui.PlayerView

    private var eglCore: EglCore? = null
    private var fullFrameBlit: FullFrameRect? = null
    private var textureId: Int = 0
    private var videoSurfaceTexture: SurfaceTexture? = null
    private val transformMatrix = FloatArray(16)

    private var mainDisplaySurface: WindowSurface? = null
    private var secondaryDisplaySurface: WindowSurface? = null
    private var currentVideoFile: String? = null

    var sizePadding = -100

    private val fileManager by contextVariableHolder.define {
        FileManager(it)
    }

    private val exchange by contextVariableHolder.define { ctx ->
        val service = ExchangeService(
            context = ctx,
            broadcastChannel = NetworkService.CHANNEL,
            server = false,
        )
        service.reg()
        service
    }.destroyWith {
        it.unreg()
    }

    /*
        private val viewManager = ViewManager { padding, align ->
            sizePadding = padding
            runOnUi {
                resize()
                playerView.redraw()
                surfaceView.redraw()
            }
        }
    */
    private var surface: Surface? = null

    //    val pathStr = "/storage/self/primary/DCIM/Camera/VID_20250404_124518.mp4"
//    val pathStr = "/storage/self/primary/Video/test.mp4"
//    val pathStr = "/storage/emulated/0/videos/test.mp4"
//    val path = Uri.fromFile(File(pathStr))


    private var controller: SimpleExoPlayerController? = null

    override fun onStart() {
        super.onStart()
        isActive = true
    }

    override fun onResume() {
        logger.infoSync("onResume")
        isActive = true
        super.onResume()
    }

    private fun initMethods() {
        val controller = controller!!
        exchange.implements(Methods.play) {
            controller.play()
            RResponse.OK
        }
        exchange.implements(Methods.pause) {
            controller.pause()
            RResponse.OK
        }
        exchange.implements(Methods.seek) {
            controller.seek(it.time)
            RResponse.OK
        }
        exchange.implements(Methods.getState) {
            RResponse.State(
                file = controller.currentFile?.name,
                totalDuration = controller.totalDuration,
                currentTime = controller.currentTime,
                isPlaying = controller.isPlaying,
            )
        }
        exchange.implements(Methods.seekDelta) {
            try {
                controller.seek(it.time + controller.currentTime)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            RResponse.OK
        }
        exchange.implements(Methods.openFile) {
            logger.infoSync("Opening file ${it.fileName}...")
            controller.open(fileManager.resolve(it.fileName))
            if (it.time != null) {
                controller.seek(it.time)
            }
            RResponse.OK
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        contextVariableHolder.create()
        isActive = true

        logger.infoSync("onCreate")
        val b = intent.extras
//        val fileName = b?.getString("file") ?: TODO()
//        val time = b?.getLong("time") ?: 0L

        super.onCreate(savedInstanceState)
        Log.i("MyActivity2", "CREATED!")
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

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
                    Log.i("DemoActivity", "EVENT->$it")
                    event(it)
                }
            }
        }

        root = RelativeLayout(this)

        player = SimpleExoPlayer.Builder(this).build().apply {
//            setMediaItem(
//                MediaItem.fromUri(
//                    obbDir.resolve("video").resolve(fileName).toURI().toURL().toString()
//                )
//            )
            playWhenReady = true
        }
        controller = SimpleExoPlayerController(player)
//        currentVideoFile = fileName
//        logger.infoSync("Opening file $fileName")
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                logger.infoSync(text = "onPlayerError:", exception = error)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                playingState = isPlaying
                logger.infoSync("onIsPlayingChanged($isPlaying)")
            }
        })
        surfaceView = SurfaceView(this)
        playerView = PlayerView(this)
        playerView.useController = false
        playerView.player = player

        (playerView.videoSurfaceView as SurfaceView).holder.addCallback(playerViewHolderCallback)
        surfaceView.holder.addCallback(surfaceViewHolderCallback)

        val var10003 = LinearLayout.LayoutParams(0, -1);
        var10003.weight = 1.0f

        val var10002 = LinearLayout.LayoutParams(0, -1);
        var10002.weight = 1.0f

        player.addListener(object : Player.Listener {
            override fun onRenderedFirstFrame() {
                resize()
                currentTotalTime = player.duration.milliseconds
            }
        })
//        player.videoScalingMode
        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
        player.prepare()
        root.addView(playerView)
        root.addView(surfaceView)
        player.addListener(object : Player.Listener {
            override fun onTimelineChanged(
                timeline: Timeline,
                reason: Int,
            ) {
                currentPlayingTime = player.currentPosition.milliseconds
            }
        })

        setContentView(root)
//        androidVideoClient = AndroidVideoClient(
//            url = config.serverUrl.toURL(),
//            handler = VideoClientHandlerImpl(
//                fileSystemManager = LocalFileSystemManager(obbDir),
//                playbackControl = VideoPlaybackControl(
//                    player = player,
//                    videoFilesRoot = obbDir.resolve("video"),
//                ),
//                viewManager = viewManager
//            ),
//            deviceId = config.id,
//            deviceName = config.name,
//        )
//        root.updateViewLayout()
        initMethods()

        controller!!.addSeekListener { time ->
            eventPublisher.publish(REvent.Seek(time))
        }
        controller!!.addPlayingChangeListener { status, time ->
            val event = if (status) {
                REvent.Play(time)
            } else {
                REvent.Pause(time)
            }
            eventPublisher.publish(event)
        }
        controller!!.addCommitedListener {
            eventPublisher.publish(REvent.Finished)
        }
        controller!!.addOpenListener { file ->
            eventPublisher.publish(REvent.Open(file))
        }
    }


    private fun resize() {
//        val wm =
//            getSystemService(WINDOW_SERVICE) as WindowManager
        val ww = windowManager.currentWindowMetrics.bounds.width() / 2
        val hh = windowManager.currentWindowMetrics.bounds.height()
//        WindowMetrics.getBounds().width()
        val ratio = player.videoSize.height.toDouble() / player.videoSize.width.toDouble()
        val paddingAbs = sizePadding.absoluteValue
        val paddingRatio = (ww - paddingAbs).toDouble() / ww.toDouble()

        Log.i("MyActivity2", "Video ratio: $ratio")

        val resultHH = (hh * ratio * paddingRatio).toInt()

        if (sizePadding >= 0) {
            playerView.setLayoutParams(
                RelativeLayout.LayoutParams(ww - sizePadding, resultHH).also {
                    it.leftMargin = 0
                })

            surfaceView.setLayoutParams(
                RelativeLayout.LayoutParams(ww - sizePadding, resultHH).also {
                    it.leftMargin = ww + sizePadding * 2
                })
        } else {
            playerView.setLayoutParams(
                RelativeLayout.LayoutParams(ww - paddingAbs, resultHH).also {
                    it.leftMargin = paddingAbs
                })
            surfaceView.setLayoutParams(
                RelativeLayout.LayoutParams(ww - paddingAbs, resultHH).also {
                    it.leftMargin = ww
                })
        }

//        playerView.updatePaddingRelative(
//            start = 0,
//            top = 0,
//            end = ww,
//            bottom = hh,
//        )

//        surfaceView.updatePaddingRelative(
//            start = ww,
//            top = 0,
//            end = windowManager.currentWindowMetrics.bounds.width(),
//            bottom = resultHH,
//        )

//        player.mi
        surfaceView.refreshDrawableState()
        playerView.refreshDrawableState()
    }

    private val surfaceViewHolderCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            secondaryDisplaySurface = WindowSurface(eglCore!!, holder.surface, false)
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
        override fun surfaceDestroyed(holder: SurfaceHolder) {}
    }

    private val frameAvailableListener = OnFrameAvailableListener {
        if (eglCore == null) return@OnFrameAvailableListener

        // PlayerView
        mainDisplaySurface?.let {
            drawFrame(it, playerView.width, playerView.height)
        }

        // SurfaceView
        secondaryDisplaySurface?.let {
            drawFrame(it, surfaceView.width, surfaceView.height)
        }
    }

    private val playerViewHolderCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            eglCore = EglCore()

            mainDisplaySurface = WindowSurface(eglCore!!, holder.surface, false).apply {
                makeCurrent()
            }
            fullFrameBlit =
                FullFrameRect(Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT))
            textureId = fullFrameBlit!!.createTextureObject()
            videoSurfaceTexture = SurfaceTexture(textureId).also {
                it.setOnFrameAvailableListener(frameAvailableListener)
            }

            surface = Surface(videoSurfaceTexture)

            player.setVideoSurface(surface)
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
        override fun surfaceDestroyed(holder: SurfaceHolder) {}
    }

//    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
//    override fun onTouchEvent(event: MotionEvent): Boolean {
////        Log.d(
////            "BaseTouchActivity",
////            "onTouchEvent：x = " + event.getX() + ", y = " + event.getY() + ",action = " + event.actionName() + ", deviceId = " + event.getDeviceId() + ",deviceName = " + event.getDevice()
////                .getName() + ",downTime = " + event.getDownTime() + ",eventTime = " + event.getEventTime()
////        )
//        if (MyTouchUtils.isRight(event)) {
////            this.touchDispatcher.onMotionEvent(event, this.callback)
//            this.motionEventDispatcher.dispatchTouchEvent(event)
//        }
//
//        return super.onTouchEvent(event)
//    }

    private fun drawFrame(windowSurface: WindowSurface, viewWidth: Int, viewHeight: Int) {
        windowSurface.makeCurrent()

        videoSurfaceTexture!!.apply {
            updateTexImage()
            getTransformMatrix(transformMatrix)
        }

        GLES20.glViewport(0, 0, viewWidth, viewHeight)

        fullFrameBlit!!.drawFrame(textureId, transformMatrix)

        windowSurface.swapBuffers()
    }

    override fun onPause() {
        Log.i("DemoActivity", "PAUSE!")
        logger.infoSync("onPause")
        isActive = false
        surface?.release()
        surface = null

        videoSurfaceTexture?.release()
        videoSurfaceTexture = null

        mainDisplaySurface?.release()
        mainDisplaySurface = null

        secondaryDisplaySurface?.release()
        secondaryDisplaySurface = null

        fullFrameBlit?.release(false)
        fullFrameBlit = null

        eglCore?.release()
        eglCore = null
//        androidVideoClient?.closeAnyway()
        super.onPause()
    }

    private fun buildState() = GlassesResponse.State(
        currentFile = controller!!.currentFile?.name,
        time = controller!!.currentTime,
        totalTime = controller!!.totalDuration,
        isPlaying = controller!!.isPlaying
    )

    private fun event(event: TempleAction) {
        Log.i("DemoActivity", "action = $event")
        when (event) {
            is TempleAction.DoubleClick -> {
                finish()
            }

            is TempleAction.Click -> {
                if (controller!!.isPlaying) {
                    eventPublisher.publish(REvent.IntentionPause(buildState()))
                } else {
                    eventPublisher.publish(REvent.IntentionPlay(buildState()))
                }
            }

            is TempleAction.SlideBackward ->
                eventPublisher.publish(REvent.IntentionSeekNext(buildState()))

            is TempleAction.SlideForward ->
                eventPublisher.publish(REvent.IntentionSeekNext(buildState()))

            else -> {

            }
        }
    }

    override fun onDestroy() {
        contextVariableHolder.destroy()
        super.onDestroy()
    }
}