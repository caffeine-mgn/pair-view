package com.rayneo.arsdk.android.demo.ui.activity

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.graphics.SurfaceTexture.OnFrameAvailableListener
import android.net.Uri
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
import com.rayneo.arsdk.android.demo.runOnUi
import com.rayneo.arsdk.android.touch.TempleAction
import kotlinx.coroutines.launch
import kotlinx.serialization.protobuf.ProtoBuf
import pw.binom.LocalFileSystemManager
import pw.binom.ViewManager
import pw.binom.dto.AbstractVideoActivity
import pw.binom.dto.Channels
import pw.binom.dto.Config
import pw.binom.dto.ExchangeService
import pw.binom.dto.RRequest
import pw.binom.dto.RResponse
import pw.binom.dto.redraw
import pw.binom.logger.Logger
import pw.binom.logger.infoSync
import kotlin.math.absoluteValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds


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

    private val logger by Logger.ofThisOrGlobal
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

    var sizePadding = -100

    private val viewManager = ViewManager { padding, align ->
        sizePadding = padding
        runOnUi {
            resize()
            playerView.redraw()
            surfaceView.redraw()
        }
    }

    private var surface: Surface? = null
    private lateinit var localFileSystemManager: LocalFileSystemManager
    private lateinit var exchanger: ExchangeService

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val bundle = intent.extras ?: return
            val action = bundle.getString(ACTION)
            logger.infoSync("Income action: $action")
            logger.infoSync("bundle: $bundle")
            when (action) {
                ACTION_SEEK -> {
                    val time = bundle.getLong(TIME) * 1000
                    logger.infoSync("Seek to $time seconds")
                    player.seekTo(time)
                }

                ACTION_SEEK_DELTA -> {
                    player.seekTo(player.currentPosition + bundle.getLong(TIME) * 1000)
                }

                ACTION_PLAY -> {
                    player.seekTo(bundle.getLong(TIME) * 1000)
                    player.play()
                }

                ACTION_PAUSE -> {
                    player.pause()
                    player.seekTo(bundle.getLong(TIME) * 1000)
                }

                ACTION_OPEN_FILE -> {
                    val fileName = bundle.getString(FILE) ?: TODO()
                    player.setMediaItem(
                        MediaItem.fromUri(
                            Uri.fromFile(
                                obbDir.resolve("video").resolve(fileName)
                            )
                        )
                    )
                    player.playWhenReady = true
                }

                else -> logger.infoSync("Unknown action \"$action\"")
            }
        }
    }

    //    val pathStr = "/storage/self/primary/DCIM/Camera/VID_20250404_124518.mp4"
//    val pathStr = "/storage/self/primary/Video/test.mp4"
//    val pathStr = "/storage/emulated/0/videos/test.mp4"
//    val path = Uri.fromFile(File(pathStr))

    private lateinit var config: Config
    override fun onStart() {
        super.onStart()
        isActive = true
    }

    override fun onResume() {
        logger.infoSync("onResume")
        isActive = true
        receiver
        registerReceiver(receiver, IntentFilter(VIDEO_PLAYER_CHANNEL))
        exchanger.reg()
        super.onResume()
    }

    private fun incomeCommand(request: RRequest): RResponse =
        when (request) {
            is RRequest.OpenVideoFile -> {
                player.setMediaItem(
                    MediaItem.fromUri(
                        Uri.fromFile(
                            obbDir.resolve("video").resolve(request.fileName)
                        )
                    )
                )
                player.playWhenReady = true
                RResponse.OK
            }

            is RRequest.Pause -> {
                player.pause()
                request.time?.let { player.seekTo(it.inWholeMilliseconds) }
                RResponse.OK
            }

            is RRequest.Play -> {
                request.time?.let { player.seekTo(it.inWholeMilliseconds) }
                player.play()
                RResponse.OK
            }

            is RRequest.Seek -> {
                player.seekTo(request.time.inWholeMilliseconds)
                RResponse.OK
            }

            is RRequest.SeekDelta -> {
                player.seekTo(request.time.inWholeMilliseconds + player.currentPosition)
                RResponse.OK
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        isActive = true

        exchanger = ExchangeService(
            context = this,
            broadcastChannel = Channels.NETWORK_SERVICE,
            selfChannel = Channels.VIDEO_ACTIVITY,
        ) { data ->
            val income = ProtoBuf.decodeFromByteArray(RRequest.serializer(), data)
            val resp = incomeCommand(income)
            ProtoBuf.encodeToByteArray(RResponse.serializer(), resp)
        }

        logger.infoSync("onCreate")
        localFileSystemManager = LocalFileSystemManager(obbDir)
        val b = intent.extras
        val fileName = b?.getString("file") ?: TODO()
        val time = b?.getLong("time") ?: 0L

        config = Config.open(this)
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
            setMediaItem(
                MediaItem.fromUri(
                    obbDir.resolve("video").resolve(fileName).toURI().toURL().toString()
                )
            )
            playWhenReady = true
        }
        logger.infoSync("Opening file $fileName")
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

        Log.i(
            "MyActivity2",
            "Video Size: ${player.videoSize.height.toDouble()} x ${player.videoSize.width.toDouble()}"
        )
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
        Log.i(
            "MyActivity",
            "Render first frame! player.videoSize=${player.videoSize.width}x${player.videoSize.height}"
        )
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
        exchanger.unreg()
        unregisterReceiver(receiver)
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

    private fun event(event: TempleAction) {
        Log.i("DemoActivity", "action = $event")
        when (event) {
            is TempleAction.DoubleClick -> {
                finish()
            }

            is TempleAction.Click -> {
                if (player.isPlaying) {
                    player.pause()
                } else {
                    player.play()
                }
            }

            is TempleAction.SlideBackward -> player.seekTo(player.currentPosition - 5_000)
            is TempleAction.SlideForward -> player.seekTo(player.currentPosition + 5_000)
            else -> {

            }
        }
    }
}