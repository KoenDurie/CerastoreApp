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
            prefs[Keys.EXTRA_TOPIC_FILTER] = next.extraTopicFilter.trim()
            prefs[Keys.STUBBE_SSID] = next.stubbeSsid.trim()
            prefs[Keys.STUBBE_WIFI_PASSWORD] = next.stubbeWifiPassword
            prefs[Keys.HOME_SSID] = next.homeSsid.trim()
            prefs[Keys.NOTIFICATIONS] = next.notificationsEnabled
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        update { it.copy(notificationsEnabled = enabled) }
    }

    private fun Preferences.toAppSettings(): AppSettings = AppSettings(
        stubbeHost = this[Keys.STUBBE_HOST] ?: AppSettings.DEFAULT_STUBBE_HOST,
        stubbePort = this[Keys.STUBBE_PORT] ?: AppSettings.DEFAULT_MQTT_PORT,
        homeHost = migratedHomeHost(this[Keys.HOME_HOST]),
        homePort = this[Keys.HOME_PORT] ?: AppSettings.DEFAULT_MQTT_PORT,
        username = this[Keys.USERNAME].orEmpty(),
        password = this[Keys.PASSWORD].orEmpty(),
        extraTopicFilter = migratedExtraFilter(this[Keys.EXTRA_TOPIC_FILTER], this[Keys.LEGACY_TOPIC_FILTER]),
        stubbeSsid = this[Keys.STUBBE_SSID].orEmpty(),
        stubbeWifiPassword = this[Keys.STUBBE_WIFI_PASSWORD].orEmpty(),
        homeSsid = this[Keys.HOME_SSID] ?: AppSettings.DEFAULT_HOME_SSID,
        notificationsEnabled = this[Keys.NOTIFICATIONS] ?: true,
    )

    private fun migratedHomeHost(stored: String?): String {
        if (stored.isNullOrBlank() || stored.equals("PC-KDR", ignoreCase = true)) {
            return AppSettings.DEFAULT_HOME_HOST
        }
        return stored
    }

    private fun migratedExtraFilter(extra: String?, legacy: String?): String {
        if (extra != null) return extra
        if (legacy.isNullOrBlank() || legacy == "#") return ""
        return legacy
    }

    private object Keys {
        val STUBBE_HOST = stringPreferencesKey("stubbe_host")
        val STUBBE_PORT = intPreferencesKey("stubbe_port")
        val HOME_HOST = stringPreferencesKey("home_host")
        val HOME_PORT = intPreferencesKey("home_port")
        val USERNAME = stringPreferencesKey("username")
        val PASSWORD = stringPreferencesKey("password")
        val EXTRA_TOPIC_FILTER = stringPreferencesKey("extra_topic_filter")
        val LEGACY_TOPIC_FILTER = stringPreferencesKey("topic_filter")
        val STUBBE_SSID = stringPreferencesKey("stubbe_ssid")
        val STUBBE_WIFI_PASSWORD = stringPreferencesKey("stubbe_wifi_password")
        val HOME_SSID = stringPreferencesKey("home_ssid")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
    }
}
