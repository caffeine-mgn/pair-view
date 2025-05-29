package pw.binom

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.session.MediaSession
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import pw.binom.logger.Logger
import pw.binom.logger.infoSync
import pw.binom.phone.dto.PhoneEvent
import pw.binom.phone.dto.PhoneRequest
import pw.binom.phone.dto.PhoneResponse
import kotlin.concurrent.atomics.ExperimentalAtomicApi


class BackgroundService : AbstractNetworkService<PhoneRequest, PhoneResponse, PhoneEvent>() {
    companion object {
        const val CHANNEL = "android-audio"
        private val logger = Logger.getLogger("BackgroundService")

        fun control(context: Context, start: Boolean) {
            context.startService(BackgroundService::class, start = start)
        }
    }


//    private val player by define {
//        MediaPlayer()
//    }.destroyWith {
//        it.release()
//    }

    private val control by define {
        MediaPlayerControl()
    }.destroyWith {
        it.close()
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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        control.addCommitedListener {
            networkManager.launch {
                sendEvent(PhoneEvent.Finished)
            }
        }
        control.addPlayingChangeListener { status, time ->
            networkManager.launch {
                val ev = if (status) {
                    PhoneEvent.Play(time)
                } else {
                    PhoneEvent.Pause(time)
                }
                sendEvent(ev)
            }
        }
        control.addSeekListener { time ->
            networkManager.launch {
                sendEvent(PhoneEvent.Seek(time))
            }
        }
        control.addOpenListener { file ->
            networkManager.launch {
                sendEvent(PhoneEvent.Open(file))
            }
        }
        exchange.implements(Methods.getServiceStatus) {
            Methods.ServiceState(
                running = true,
                connected = isConnected,
            )
        }
        PhoneRequest
    }

    val CHANNEL_ID = "MyPlayer"
    val NOTIFICATION_ID = 42

    override fun connected() {
        GlobalScope.launch {
            exchange.call(
                Methods.sendServiceState, Methods.ServiceState(
                    running = true,
                    connected = isConnected
                )
            )
        }
        super.connected()
    }

    override fun disconnected() {
        GlobalScope.launch {
            exchange.call(
                Methods.sendServiceState, Methods.ServiceState(
                    running = true,
                    connected = isConnected
                )
            )
        }
        super.disconnected()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val isStart = intent.action == "start"
        if (isStart) {
            obbDir.mkdirs()
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            val channel = NotificationChannel(
                CHANNEL_ID,  // ID канала
                "Музыкальный плеер",  // Название канала
                NotificationManager.IMPORTANCE_DEFAULT // Уровень важности
            )
            channel.setDescription("Уведомления для аудиоплеера")
            notificationManager.createNotificationChannel(channel)

            // Для кнопки Play
            val playIntent: Intent = Intent(this, MediaReceiver::class.java)
            playIntent.setAction("ACTION_PLAY")
            val pendingIntentPlay = PendingIntent.getBroadcast(
                this,
                0,
                playIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )


// Для кнопки Pause
            val pauseIntent: Intent = Intent(this, MediaReceiver::class.java)
            pauseIntent.setAction("ACTION_PAUSE")
            val pendingIntentPause = PendingIntent.getBroadcast(
                this,
                1,
                pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            val mediaSession = MediaSession(this, CHANNEL_ID)
            val ee = androidx.media.app.NotificationCompat.MediaStyle()
            MediaSessionCompat.Token.fromToken(null)
//            Notification.Action.
            val builder2 = Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_media_play)
                .setContentTitle("Моя музыка")
                .setContentText("Трек 1")
//                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setStyle(Notification.MediaStyle().setMediaSession(mediaSession.getSessionToken()))
                .addAction(R.drawable.ic_media_play, "Play", pendingIntentPlay)
                .addAction(R.drawable.ic_media_pause, "Pause", pendingIntentPause)
                .setAutoCancel(false)

//            val notificationManager =
//                context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, builder2.build())
            /*
                        val builder: NotificationCompat.Builder? = NotificationCompat.Builder(this, CHANNEL_ID)
            //                .setSmallIcon(R.drawable.ic_music_note)
                            .setSmallIcon(R.drawable.ic_media_play)
                            .setContentTitle("Моя музыка")
                            .setContentText("Трек 1")
                            .setPriority(NotificationCompat.PRIORITY_LOW)
                            .setStyle(
                                MediaStyle()
                                    .setMediaSession(mediaSession.getSessionToken())
                                    .setShowActionsInCompactView(0, 1, 2) // Кнопки play/pause/next
                            )
                            .addAction(R.drawable.ic_media_play, "Play", pendingIntentPlay)
                            .addAction(R.drawable.ic_media_pause, "Pause", pendingIntentPause)
                            .setAutoCancel(false)
                        */
        }
        return START_STICKY
    }


    override fun onDestroy() {
        super.onDestroy()
    }

    override val requestSerializer: KSerializer<PhoneRequest>
        get() = PhoneRequest.serializer()
    override val responseSerializer: KSerializer<PhoneResponse>
        get() = PhoneResponse.serializer()
    override val eventSerializer: KSerializer<PhoneEvent>
        get() = PhoneEvent.serializer()

    @OptIn(ExperimentalAtomicApi::class)
    override suspend fun rpc(param: PhoneRequest) =
        when (param) {
            PhoneRequest.GetLocalFiles -> {
                PhoneResponse.LocalFiles(fileManager.files.map { it.name })
            }

            is PhoneRequest.Open -> {
                val audio = fileManager.resolve(param.fileName)
                if (!audio.isFile) {
                    throw IllegalStateException("File ${param.fileName} not found")
                }
                control.open(audio)
                logger.infoSync("Open")
                PhoneResponse.OK
            }

            is PhoneRequest.Pause -> {
                control.pause()
                if (param.time != null) {
                    control.seek(param.time!!)
                }
                logger.infoSync("Pause")
                PhoneResponse.OK
            }

            is PhoneRequest.Play -> {
                if (param.time != null) {
                    control.seek(param.time!!)
                }
                control.play()
                logger.infoSync("Play")
                PhoneResponse.OK
            }

            is PhoneRequest.Seek -> {
                control.seek(param.time)
                logger.infoSync("Seek")
                PhoneResponse.OK
            }

            is PhoneRequest.SeekDelta -> {
                control.seek(param.time + control.currentTime)
                logger.infoSync("SeekDelta")
                PhoneResponse.OK
            }

            PhoneRequest.GetState -> {
                PhoneResponse.State(
                    currentFile = control.currentFile?.name,
                    time = control.currentTime,
                    totalTime = control.totalDuration,
                    isPlaying = control.isPlaying,
                )
            }
        }
}