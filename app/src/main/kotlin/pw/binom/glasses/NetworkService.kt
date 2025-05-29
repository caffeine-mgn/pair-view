package pw.binom.glasses

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.widget.Toast
import com.rayneo.arsdk.android.demo.DemoHomeActivity
import com.rayneo.arsdk.android.demo.ui.activity.VideoPlayerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.KSerializer
import pw.binom.AbstractNetworkService
import pw.binom.EventBroadcast
import pw.binom.ExchangeService
import pw.binom.dto.Actions
import pw.binom.glasses.RRequest.*
import pw.binom.glasses.dto.GlassesEvent
import pw.binom.glasses.dto.GlassesEvent.*
import pw.binom.glasses.dto.GlassesRequest
import pw.binom.glasses.dto.GlassesResponse
import pw.binom.glasses.dto.GlassesResponse.*
import pw.binom.logger.Logger
import pw.binom.logger.infoSync
import pw.binom.startService
import pw.binom.video.R
import kotlin.time.Duration.Companion.seconds

class NetworkService : AbstractNetworkService<GlassesRequest, GlassesResponse, GlassesEvent>() {
    companion object {
        const val CHANNEL = "glasses.video"
        const val EVENTS = "glasses.events"
        private val logger = Logger.Companion.getLogger("NetworkService.Companion")
        private fun control(context: Context, actions: Actions) {
            Intent(context, NetworkService::class.java).also {
                it.action = actions.name
                logger.infoSync("Starting the service in >=26 Mode")
//                context.startForegroundService(it)
                context.startService(it)
                return
            }
        }

        fun start(context: Context) {
            context.startService(NetworkService::class, start = true)
        }

        fun stop(context: Context) {
            context.startService(NetworkService::class, start = false)
        }

        fun isStarted(context: Context) =
            context.isServiceRunning(NetworkService::class.java)
    }

    private val eventListener by define { ctx ->
        EventBroadcast.listener(
            context = ctx,
            channel = EVENTS,
            serializer = REvent.serializer(),
            handler = this@NetworkService::incomeEvent
        )
    }

    private fun incomeEvent(event: REvent) {
        val glassesEvent = when (event) {
            is REvent.Pause -> GlassesEvent.Pause(event.time)
            is REvent.Play -> GlassesEvent.Play(event.time)
            is REvent.Seek -> GlassesEvent.Seek(event.time)
            is REvent.Finished -> GlassesEvent.Finished
            is REvent.Open -> Open(event.file)
            is REvent.IntentionPause -> GlassesEvent.IntentionPause(event.state)
            is REvent.IntentionPlay -> GlassesEvent.IntentionPlay(event.state)
            is REvent.IntentionSeekBack -> GlassesEvent.IntentionSeekBack(event.state)
            is REvent.IntentionSeekNext -> GlassesEvent.IntentionSeekNext(event.state)
        }
        GlobalScope.launch(networkManager) { sendEvent(glassesEvent) }
    }

    private val exchange by define { ctx ->
        val service = ExchangeService(
            context = ctx,
            broadcastChannel = CHANNEL,
            server = true,
        )
        service.reg()
        service
    }.destroyWith {
        it.unreg()
    }

    private var isServiceStarted = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.infoSync("onStartCommand executed with startId: $startId")
        if (intent != null) {
            val action = intent.action
            logger.infoSync("using an intent with action $action")
            when (action) {
                Actions.START.name -> startService()
                Actions.STOP.name -> stopService()
                else -> logger.infoSync("This should never happen. No action in the received intent")
            }
        } else {
            logger.infoSync(
                "with a null intent. It has been probably restarted by the system."
            )
        }
        // by returning this we make sure the service is restarted if the system kills the service
        return START_STICKY
    }

    override val requestSerializer: KSerializer<GlassesRequest>
        get() = GlassesRequest.serializer()
    override val responseSerializer: KSerializer<GlassesResponse>
        get() = GlassesResponse.serializer()
    override val eventSerializer: KSerializer<GlassesEvent>
        get() = GlassesEvent.serializer()

    override fun onCreate() {
        super.onCreate()
        logger.infoSync("The service has been created".uppercase())
        var notification = createNotification()
        startForeground(1, notification)
    }

    override fun onDestroy() {
        logger.infoSync("The service has been destroyed".uppercase())
        Toast.makeText(this, "Service destroyed", Toast.LENGTH_SHORT).show()
        super.onDestroy()
    }

    private suspend fun <T> ensureActivity(func: suspend () -> T): T {
        if (!VideoPlayerActivity.isActive) {
            val intent = Intent(this, VideoPlayerActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            Thread.sleep(1.seconds.inWholeMilliseconds)
        }
        var count = 5
        do {
            try {
                return withTimeout(5.seconds) {
                    func()
                }
            } catch (e: TimeoutCancellationException) {
                count--
                val intent = Intent(this, VideoPlayerActivity::class.java)
                startActivity(intent)
            }
        } while (count > 0)
        throw IllegalStateException("Can't open video activity")
    }

    override suspend fun rpc(param: GlassesRequest): GlassesResponse {
        return when (param) {
            GlassesRequest.GetLocalFiles -> {
                LocalFiles(fileManager.files.map { it.name })
            }

            is GlassesRequest.Open -> ensureActivity {
                exchange.call(
                    Methods.openFile, OpenVideoFile(
                        fileName = param.fileName,
                        time = null,
                    )
                )
                OK
            }

            is GlassesRequest.Pause -> ensureActivity {
                exchange.call(
                    Methods.pause, Pause(
                        time = param.time
                    )
                )
                OK
            }

            is GlassesRequest.Play -> ensureActivity {
                exchange.call(
                    Methods.play, Play(
                        time = param.time
                    )
                )
                OK
            }

            is GlassesRequest.Seek -> ensureActivity {
                exchange.call(
                    Methods.seek, Seek(
                        time = param.time
                    )
                )
                OK
            }

            is GlassesRequest.SeekDelta -> ensureActivity {
                exchange.call(
                    Methods.seekDelta, SeekDelta(
                        time = param.time
                    )
                )
                OK
            }

            GlassesRequest.GetState -> ensureActivity {
                val state = exchange.call(
                    Methods.getState, GetState
                )
                State(
                    currentFile = state.file,
                    time = state.currentTime,
                    totalTime = state.totalDuration,
                    isPlaying = state.isPlaying,
                )
            }
        }
    }

    private val wakeLock by define {
        (getSystemService(POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EndlessService::lock").apply {
                acquire()
            }
        }
    }.destroyWith {
        if (it.isHeld) {
            it.release()
        }
    }

    private fun startService() {
        if (isServiceStarted) return
        logger.infoSync("Starting the foreground service task")
        Toast.makeText(this, "Service starting its task", Toast.LENGTH_SHORT).show()
        isServiceStarted = true
    }

    private fun stopService() {
        GlobalScope.launch(Dispatchers.IO) {
            networkManager?.launch {
                runCatching { networkJob?.cancelAndJoin() }
                httpClient?.asyncClose()
            }?.cancelAndJoin()
            networkManager?.closeAnyway()
        }

        logger.infoSync("Stopping the foreground service")
        Toast.makeText(this, "Service stopping", Toast.LENGTH_SHORT).show()
        try {
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
            logger.infoSync("Service stopped without being started: ${e.message}")
        }
        isServiceStarted = false
//        setServiceState(this, ServiceState.STOPPED)
    }

    private fun createNotification(): Notification {
        val notificationChannelId = "ENDLESS SERVICE CHANNEL"

        // depending on the Android API that we're dealing with we will have
        // to use a specific method to create the notification
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager;
        val channel = NotificationChannel(
            notificationChannelId,
            "Endless Service notifications channel",
            NotificationManager.IMPORTANCE_HIGH
        ).let {
            it.description = "Endless Service channel"
            it.enableLights(true)
            it.lightColor = Color.RED
            it.enableVibration(true)
            it.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            it
        }
        notificationManager.createNotificationChannel(channel)

        val pendingIntent = Intent(this, DemoHomeActivity::class.java)
            .let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_MUTABLE)
            }

        val builder: Notification.Builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(
                this,
                notificationChannelId
            ) else Notification.Builder(this, null)

        return builder
            .setContentTitle("Endless Service")
            .setContentText("This is your favorite endless service working")
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setTicker("Ticker text")
            .setPriority(Notification.PRIORITY_HIGH) // for under android 26 compatibility
            .build()

    }
}