package be.kdr.agvalarm

import android.app.Application
import be.kdr.agvalarm.data.SettingsRepository
import be.kdr.agvalarm.mqtt.MqttManager
import be.kdr.agvalarm.notifications.NotificationHelper

class AgvAlarmApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        System.setProperty("io.netty.transport.noNative", "true")
        container = AppContainer(this)
    }
}

class AppContainer(application: Application) {
    val settingsRepository = SettingsRepository(application)
    val notificationHelper = NotificationHelper(application)
    val mqttManager = MqttManager(
        context = application,
        settingsRepository = settingsRepository,
        notificationHelper = notificationHelper,
    )
}
