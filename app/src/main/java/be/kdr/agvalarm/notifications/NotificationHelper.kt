package be.kdr.agvalarm.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import be.kdr.agvalarm.MainActivity
import be.kdr.agvalarm.R
import be.kdr.agvalarm.model.BrokerTarget
import be.kdr.agvalarm.model.ConnectionStatus
import be.kdr.agvalarm.model.ConnectionUiState
import be.kdr.agvalarm.model.MqttEvent
import be.kdr.agvalarm.mqtt.AlarmClassifier
import be.kdr.agvalarm.mqtt.AlarmVerdict

class NotificationHelper(context: Context) {

    private val appContext = context.applicationContext
    private val manager = NotificationManagerCompat.from(appContext)

    init {
        createChannels()
    }

    fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return manager.areNotificationsEnabled()
    }

    fun statusNotification(state: ConnectionUiState): Notification {
        val text = statusText(state)
        val tap = activityIntent(eventId = null, requestCode = 1)
        return NotificationCompat.Builder(appContext, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_stat_agv)
            .setContentTitle(appContext.getString(R.string.app_name))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(tap)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    fun notifyAlarm(event: MqttEvent, verdict: AlarmVerdict) {
        val title = AlarmClassifier.notificationTitle(event.topic, event.payload)
        val body = event.displayMessage?.takeIf { it.isNotBlank() }
            ?: event.payload.trim().ifEmpty { event.topic }.take(240)
        val tap = activityIntent(eventId = event.id, requestCode = AlarmClassifier.notificationId(verdict))
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_stat_agv)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$body\n\n${event.topic}"))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(longArrayOf(0, 400, 200, 400))
            .setLights(Color.argb(255, 255, 179, 0), 400, 400)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(tap)
            .build()
        runCatching {
            manager.notify(AlarmClassifier.notificationId(verdict), notification)
        }
    }

    private fun statusText(state: ConnectionUiState): String {
        val broker = state.broker
        return when (state.status) {
            ConnectionStatus.CONNECTED -> when (broker) {
                is BrokerTarget.Stubbe -> "AGV Alarm · Verbonden met Stubbe"
                is BrokerTarget.Home -> "AGV Alarm · Verbonden met Thuis"
                null -> "AGV Alarm · Verbonden"
            }
            ConnectionStatus.CONNECTING -> when (broker) {
                is BrokerTarget.Stubbe -> "AGV Alarm · Verbinden met Stubbe…"
                is BrokerTarget.Home -> "AGV Alarm · Verbinden met Thuis…"
                null -> "AGV Alarm · Verbinden…"
            }
            ConnectionStatus.OFFLINE -> "AGV Alarm · Offline"
        }
    }

    private fun activityIntent(eventId: Long?, requestCode: Int): PendingIntent {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (eventId != null) {
                putExtra(MainActivity.EXTRA_EVENT_ID, eventId)
            }
        }
        return PendingIntent.getActivity(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_STATUS,
                "MQTT-status",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Toont of AGV Alarm verbonden is met de MQTT-broker."
                setShowBadge(false)
            },
        )
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val alarmChannel = NotificationChannel(
            CHANNEL_ALARM,
            "AGV storing",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Popup en heads-up wanneer een AGV in storing gaat."
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400)
            enableLights(true)
            lightColor = Color.argb(255, 255, 179, 0)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(
                alarmSound,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }
        nm.createNotificationChannel(alarmChannel)
    }

    companion object {
        const val CHANNEL_STATUS = "mqtt_status"
        const val CHANNEL_ALARM = "agv_storing"
        const val STATUS_ID = 1001
    }
}
