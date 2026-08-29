package be.kdr.agvalarm.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "agv_alarm_settings",
)

class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    val settings: Flow<AppSettings> = dataStore.data
        .map { it.toAppSettings() }
        .distinctUntilChanged()

    suspend fun current(): AppSettings = dataStore.data.first().toAppSettings()

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs ->
            val next = transform(prefs.toAppSettings())
            prefs[Keys.STUBBE_HOST] = next.stubbeHost.trim()
            prefs[Keys.STUBBE_PORT] = next.stubbePort.coerceIn(1, 65535)
            prefs[Keys.HOME_HOST] = next.homeHost.trim()
            prefs[Keys.HOME_PORT] = next.homePort.coerceIn(1, 65535)
            prefs[Keys.USERNAME] = next.username
            prefs[Keys.PASSWORD] = next.password
            prefs[Keys.TOPIC_FILTER] = next.topicFilter.ifBlank { AppSettings.DEFAULT_TOPIC_FILTER }
            prefs[Keys.STUBBE_SSID] = next.stubbeSsid.trim()
            prefs[Keys.STUBBE_WIFI_PASSWORD] = next.stubbeWifiPassword
            prefs[Keys.NOTIFICATIONS] = next.notificationsEnabled
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        update { it.copy(notificationsEnabled = enabled) }
    }

    private fun Preferences.toAppSettings(): AppSettings = AppSettings(
        stubbeHost = this[Keys.STUBBE_HOST] ?: AppSettings.DEFAULT_STUBBE_HOST,
        stubbePort = this[Keys.STUBBE_PORT] ?: AppSettings.DEFAULT_MQTT_PORT,
        homeHost = this[Keys.HOME_HOST] ?: AppSettings.DEFAULT_HOME_HOST,
        homePort = this[Keys.HOME_PORT] ?: AppSettings.DEFAULT_MQTT_PORT,
        username = this[Keys.USERNAME].orEmpty(),
        password = this[Keys.PASSWORD].orEmpty(),
        topicFilter = this[Keys.TOPIC_FILTER] ?: AppSettings.DEFAULT_TOPIC_FILTER,
        stubbeSsid = this[Keys.STUBBE_SSID].orEmpty(),
        stubbeWifiPassword = this[Keys.STUBBE_WIFI_PASSWORD].orEmpty(),
        notificationsEnabled = this[Keys.NOTIFICATIONS] ?: true,
    )

    private object Keys {
        val STUBBE_HOST = stringPreferencesKey("stubbe_host")
        val STUBBE_PORT = intPreferencesKey("stubbe_port")
        val HOME_HOST = stringPreferencesKey("home_host")
        val HOME_PORT = intPreferencesKey("home_port")
        val USERNAME = stringPreferencesKey("username")
        val PASSWORD = stringPreferencesKey("password")
        val TOPIC_FILTER = stringPreferencesKey("topic_filter")
        val STUBBE_SSID = stringPreferencesKey("stubbe_ssid")
        val STUBBE_WIFI_PASSWORD = stringPreferencesKey("stubbe_wifi_password")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
    }
}
