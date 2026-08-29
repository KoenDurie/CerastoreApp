package be.kdr.agvalarm.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import be.kdr.agvalarm.AgvAlarmApplication
import be.kdr.agvalarm.mqtt.MqttManager
import be.kdr.agvalarm.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MqttForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var mqttManager: MqttManager
    private lateinit var notificationHelper: NotificationHelper

    override fun onCreate() {
        super.onCreate()
        val container = (application as AgvAlarmApplication).container
        mqttManager = container.mqttManager
        notificationHelper = container.notificationHelper
        val notification = notificationHelper.statusNotification(mqttManager.connection.value)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NotificationHelper.STATUS_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NotificationHelper.STATUS_ID, notification)
        }
        mqttManager.start()
        scope.launch {
            mqttManager.connection.collect { state ->
                startForegroundCompat(notificationHelper.statusNotification(state))
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mqttManager.start()
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundCompat(notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NotificationHelper.STATUS_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NotificationHelper.STATUS_ID, notification)
        }
    }
}
