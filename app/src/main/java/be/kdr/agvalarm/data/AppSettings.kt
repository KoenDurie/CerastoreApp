package be.kdr.agvalarm.data

data class AppSettings(
    val stubbeHost: String = DEFAULT_STUBBE_HOST,
    val stubbePort: Int = DEFAULT_MQTT_PORT,
    val homeHost: String = DEFAULT_HOME_HOST,
    val homePort: Int = DEFAULT_MQTT_PORT,
    val username: String = "",
    val password: String = "",
    val topicFilter: String = DEFAULT_TOPIC_FILTER,
    val stubbeSsid: String = "",
    val stubbeWifiPassword: String = "",
    val notificationsEnabled: Boolean = true,
) {
    companion object {
        const val DEFAULT_STUBBE_HOST = "10.0.0.20"
        const val DEFAULT_HOME_HOST = "PC-KDR"
        const val DEFAULT_MQTT_PORT = 1883
        const val DEFAULT_TOPIC_FILTER = "#"
        const val KEEP_ALIVE_SECONDS = 30
    }
}
