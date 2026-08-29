package be.kdr.agvalarm.data

data class AppSettings(
    val stubbeHost: String = DEFAULT_STUBBE_HOST,
    val stubbePort: Int = DEFAULT_MQTT_PORT,
    val homeHost: String = DEFAULT_HOME_HOST,
    val homePort: Int = DEFAULT_MQTT_PORT,
    val username: String = "",
    val password: String = "",
    val extraTopicFilter: String = "",
    val stubbeSsid: String = "",
    val stubbeWifiPassword: String = "",
    val homeSsid: String = DEFAULT_HOME_SSID,
    val notificationsEnabled: Boolean = true,
) {
    fun subscriptionKey(): String = TopicSubscriptions.filters(extraTopicFilter).joinToString("|")

    companion object {
        const val DEFAULT_STUBBE_HOST = "10.0.0.20"
        const val DEFAULT_HOME_HOST = "192.168.0.239"
        const val DEFAULT_HOME_SSID = "telenet-7E9C4"
        const val DEFAULT_MQTT_PORT = 1883
        const val KEEP_ALIVE_SECONDS = 30
        const val CONNECT_TIMEOUT_SECONDS = 10
        const val HOME_LOCALHOST_HINT =
            "Mosquitto op PC-KDR luistert mogelijk alleen op localhost. Open listener 1883 op 0.0.0.0 en firewallpoort 1883."
    }
}

object TopicSubscriptions {
    val DEFAULT = listOf("inventory/#", "quality/status")
    private val COMMAND_TOPICS = setOf(
        "quality/robot/cmd",
        "quality/robot/ack",
    )

    fun filters(extraTopicFilter: String): List<String> {
        val extra = extraTopicFilter.trim()
        val result = DEFAULT.toMutableList()
        if (extra.isNotEmpty() && extra !in result) {
            result += extra
        }
        return result.distinct()
    }

    fun isCommandTopic(topic: String): Boolean =
        COMMAND_TOPICS.any { it.equals(topic, ignoreCase = true) }
}
