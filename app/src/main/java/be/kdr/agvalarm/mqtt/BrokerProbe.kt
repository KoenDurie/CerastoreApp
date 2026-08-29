package be.kdr.agvalarm.mqtt

import java.net.InetSocketAddress
import java.net.Socket

object BrokerProbe {
    const val DEFAULT_TIMEOUT_MS = 1_500

    fun isReachable(host: String, port: Int, timeoutMs: Int = DEFAULT_TIMEOUT_MS): Boolean {
        val trimmed = host.trim()
        if (trimmed.isEmpty() || port !in 1..65535) return false
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(trimmed, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }
}
