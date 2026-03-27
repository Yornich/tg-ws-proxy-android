package com.tgwsproxy.proxy

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.tgwsproxy.MainActivity
import com.tgwsproxy.R
import com.tgwsproxy.data.LogBuffer
import com.tgwsproxy.data.ProxyRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import java.net.ServerSocket
import javax.inject.Inject

@AndroidEntryPoint
class ProxyService : Service() {

    @Inject lateinit var repository: ProxyRepository
    @Inject lateinit var logBuffer: LogBuffer

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverSocket: ServerSocket? = null
    private val stats = ProxyStats()
    private val wsPool = WsPool()
    private val wsBlacklist = mutableSetOf<Pair<Int, Boolean>>()
    private val dcFailUntil = mutableMapOf<Pair<Int, Boolean>, Long>()

    companion object {
        const val ACTION_START   = "com.tgwsproxy.START"
        const val ACTION_STOP    = "com.tgwsproxy.STOP"
        const val CHANNEL_ID     = "proxy_service"
        const val CHANNEL_SILENT = "proxy_service_silent"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopProxy()
            else        -> startProxy()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null


    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startProxy() {
        val config = repository.currentConfig
        stats.reset()
        repository.setRunning(true)
        startForeground(NOTIFICATION_ID, buildNotification(config))

        serviceScope.launch {
            try {
                val ss = ServerSocket(config.port)
                serverSocket = ss
                logBuffer.log("INFO", "Прокси запущен на ${config.host}:${config.port}")

                val dcIpMap = config.dcIpList.associate { it.dc to it.ip }
                wsPool.warmup(dcIpMap)

                while (!ss.isClosed) {
                    val client = try { ss.accept() } catch (_: Exception) { break }
                    try {
                        if (config.tcpNodelay) client.tcpNoDelay = true
                        client.receiveBufferSize = config.bufKb * 1024
                        client.sendBufferSize    = config.bufKb * 1024
                    } catch (_: Exception) {}

                    launch {
                        Socks5Handler(
                            clientSocket = client,
                            config       = config,
                            stats        = stats,
                            wsPool       = wsPool,
                            wsBlacklist  = wsBlacklist,
                            dcFailUntil  = dcFailUntil,
                            logger       = { msg ->
                                if (config.verboseLog || !msg.contains("[DEBUG]"))
                                    logBuffer.log("DEBUG", msg)
                            }
                        ).handle()
                        repository.updateStats(stats.snapshot())
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Proxy server error")
                logBuffer.log("ERROR", "Ошибка прокси: $e")
            } finally {
                repository.setRunning(false)
                logBuffer.log("INFO", "Прокси остановлен")
            }
        }


        serviceScope.launch {
            while (true) {
                delay(10_000)
                repository.updateStats(stats.snapshot())
                val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIFICATION_ID, buildNotification(repository.currentConfig))
            }
        }
    }

    private fun stopProxy() {
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
        wsPool.shutdown()
        repository.setRunning(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(config: ProxyConfig): Notification {
        val snap = stats.snapshot()
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )


        if (!config.showNotification) {
            return NotificationCompat.Builder(this, CHANNEL_SILENT)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(openIntent)
                .setOngoing(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .build()
        }

        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, ProxyService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val up   = snap.trafficUp
        val down = snap.trafficDown
        val bigText = "SOCKS5: ${config.host}:${config.port}\n" +
                "Соединений: ${snap.connectionsTotal}  WS: ${snap.connectionsWs}  TCP: ${snap.connectionsTcpFallback}\n" +
                "TX: $up   RX: $down"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText("${config.host}:${config.port}  TX: $up  RX: $down")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setContentIntent(openIntent)
            .addAction(R.drawable.ic_notification, getString(R.string.notification_action_stop), stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager


        nm.createNotificationChannel(NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        })


        nm.createNotificationChannel(NotificationChannel(
            CHANNEL_SILENT,
            "Фоновая служба",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_SECRET
        })
    }
}
