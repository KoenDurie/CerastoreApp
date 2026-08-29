package be.kdr.agvalarm.model

sealed class BrokerTarget {
    abstract val host: String
    abstract val port: Int
    abstract val environmentLabel: String

    val hostPort: String get() = "$host:$port"
    val displayName: String get() = "$environmentLabel $host"

    data class Stubbe(
        override val host: String,
        override val port: Int,
    ) : BrokerTarget() {
        override val environmentLabel: String = "Stubbe"
    }

    data class Home(
        override val host: String,
        override val port: Int,
    ) : BrokerTarget() {
        override val environmentLabel: String = "Thuis"
    }
}

enum class ConnectionStatus {
    CONNECTED,
    CONNECTING,
    OFFLINE,
}

data class ConnectionUiState(
    val status: ConnectionStatus = ConnectionStatus.OFFLINE,
    val broker: BrokerTarget? = null,
    val lastError: String? = null,
)

data class NetworkUiState(
    val transportLabel: String = "geen netwerk",
    val ssid: String? = null,
    val vpnActive: Boolean = false,
)

data class MqttEvent(
    val id: Long,
    val timestampMillis: Long,
    val topic: String,
    val payload: String,
    val isAlarm: Boolean,
    val agvId: String?,
)

data class BrokerReachability(
    val stubbeReachable: Boolean,
    val homeReachable: Boolean,
    val stubbeTarget: String,
    val homeTarget: String,
)
