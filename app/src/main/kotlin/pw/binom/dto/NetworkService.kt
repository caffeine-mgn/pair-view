package pw.binom.dto

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.widget.Toast
import com.rayneo.arsdk.android.demo.DemoHomeActivity
import com.rayneo.arsdk.android.demo.runOnUi
import com.rayneo.arsdk.android.demo.ui.activity.VideoPlayerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import pw.binom.DeviceClient
import pw.binom.LocalFileSystemManager
import pw.binom.http.client.HttpClientRunnable
import pw.binom.http.client.factory.Https11ConnectionFactory
import pw.binom.http.client.factory.NativeNetChannelFactory
import pw.binom.logger.Logger
import pw.binom.logger.info
import pw.binom.logger.infoSync
import pw.binom.network.MultiFixedSizeThreadNetworkDispatcher
import pw.binom.url.toURL
import pw.binom.video.R
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.seconds


class NetworkService : Service() {
    companion object {
        private val logger = Logger.getLogger("NetworkService.Companion")
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
            if (isStarted(context)) {
                return
            }
            control(context, Actions.START)
        }

        fun stop(context: Context) {
            if (!isStarted(context)) {
                return
            }
            control(context, Actions.START)
        }

        fun isStarted(context: Context) =
            context.isServiceRunning(NetworkService::class.java)
    }

    private val logger by Logger.ofThisOrGlobal

    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStarted = false
    private lateinit var localFileSystemManager: LocalFileSystemManager

    /*
        private val handler = object : VideoClient.Handler {

            override suspend fun getCurrentPlayingFile(): String? =
                VideoPlayerActivity.currentPlayingFile

            override suspend fun getCurrentPlayingTime(): Long =
                VideoPlayerActivity.currentPlayingTime

            override suspend fun isPlayingStatus(): Boolean =
                VideoPlayerActivity.playingState

            override suspend fun openFile(name: String, time: Long) {
                if (VideoPlayerActivity.isActive) {
                    sendEvent(VideoPlayerActivity.VIDEO_PLAYER_CHANNEL) {
                        putExtra(VideoPlayerActivity.ACTION, VideoPlayerActivity.ACTION_OPEN_FILE)
                        putExtra(VideoPlayerActivity.FILE, name)
                    }
                } else {
                    val intent = Intent(this@NetworkService, VideoPlayerActivity::class.java)
                    val b = Bundle()
                    b.putString("file", name)
                    b.putLong("time", time)
                    intent.putExtras(b)
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent)
                }
            }

            override suspend fun seek(time: Long) {
                sendEvent(VideoPlayerActivity.VIDEO_PLAYER_CHANNEL) {
                    putExtra(VideoPlayerActivity.ACTION, VideoPlayerActivity.ACTION_SEEK)
                    putExtra(VideoPlayerActivity.TIME, time)
                }
            }

            override suspend fun seekDelta(time: Long) {
                sendEvent(VideoPlayerActivity.VIDEO_PLAYER_CHANNEL) {
                    putExtra(VideoPlayerActivity.ACTION, VideoPlayerActivity.ACTION_SEEK_DELTA)
                    putExtra(VideoPlayerActivity.TIME, time)
                }
            }

            override suspend fun play(time: Long) {
                sendEvent(VideoPlayerActivity.VIDEO_PLAYER_CHANNEL) {
                    putExtra(VideoPlayerActivity.ACTION, VideoPlayerActivity.ACTION_PLAY)
                    putExtra(VideoPlayerActivity.TIME, time)
                }
            }

            override suspend fun pause(time: Long) {
                sendEvent(VideoPlayerActivity.VIDEO_PLAYER_CHANNEL) {
                    putExtra(VideoPlayerActivity.ACTION, VideoPlayerActivity.ACTION_PAUSE)
                    putExtra(VideoPlayerActivity.TIME, time)
                }
            }

            override suspend fun getLocalFiles(): List<String> =
                localFileSystemManager.getFiles()

            override suspend fun updateView(padding: Int, align: Int) {
                TODO("Not yet implemented")
            }

        }
    */
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

    private lateinit var config: Config
    private lateinit var serviceDeviceEventProcessor: ServiceDeviceEventProcessor
    override fun onCreate() {
        super.onCreate()
        logger.infoSync("The service has been created".uppercase())
        var notification = createNotification()
        startForeground(1, notification)
        config = Config.open(this)
        localFileSystemManager = LocalFileSystemManager(obbDir)
        serviceDeviceEventProcessor =
            ServiceDeviceEventProcessor(context = this, config = Config.open(this))
        serviceDeviceEventProcessor.reg()
    }

    override fun onDestroy() {
        logger.infoSync("The service has been destroyed".uppercase())
        Toast.makeText(this, "Service destroyed", Toast.LENGTH_SHORT).show()
        serviceDeviceEventProcessor.unreg()
        super.onDestroy()
    }

    private var networkManager: MultiFixedSizeThreadNetworkDispatcher? = null
    private var networkJob: Job? = null
    private var httpClient: HttpClientRunnable? = null

    private suspend fun processing() {
        logger.info("Service processing started")
        try {
            while (coroutineContext.isActive) {
                val client = try {
                    withTimeout(20.seconds) {
                        DeviceClient.create(
                            handler = serviceDeviceEventProcessor,
                            client = httpClient!!,
                            url = config.serverUrl.toURL(),
                            deviceId = config.id,
                            deviceName = config.name,
                        )
                    }
                } catch (e: TimeoutCancellationException) {
                    logger.info("Connection timeout")
                    delay(10.seconds)
                    continue
                } catch (e: Throwable) {
                    logger.info("Can't connect")
                    delay(10.seconds)
                    continue
                }
                runOnUi {
                    Toast.makeText(this, "Connected to server!", Toast.LENGTH_SHORT).show()
                }
                try {
                    client.processing()
                } catch (e: Throwable) {
                    logger.info("Disconnected :(")
                    e.printStackTrace()
                    continue
                } finally {
                    runOnUi {
                        Toast.makeText(this, "Connection lost", Toast.LENGTH_SHORT).show()
                    }
                    runCatching { client.asyncClose() }
                }
            }
        } finally {
            logger.info("Service processing finished")
        }
    }

    private fun startService() {
        if (isServiceStarted) return
        logger.infoSync("Starting the foreground service task")
        Toast.makeText(this, "Service starting its task", Toast.LENGTH_SHORT).show()
        isServiceStarted = true
//        setServiceState(this, ServiceState.STARTED)

        // we need this lock so our service gets not affected by Doze Mode
        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EndlessService::lock").apply {
                    acquire()
                }
            }
        val nm = MultiFixedSizeThreadNetworkDispatcher(
            Runtime.getRuntime().availableProcessors(),
            selectTimeout = 10.seconds
        )
        val cl = HttpClientRunnable(
            factory = Https11ConnectionFactory(),
            source = NativeNetChannelFactory(nm),
            idleCoroutineContext = nm,
        )
        httpClient = cl
        networkManager = nm
        nm.launch {
            while (isActive) {
                logger.infoSync("Steel active!")
                delay(5.seconds)
            }
        }
        networkJob = nm.launch {
            processing()
        }
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
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
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
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;
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