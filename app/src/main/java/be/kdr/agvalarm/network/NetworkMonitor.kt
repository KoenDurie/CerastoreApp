package be.kdr.agvalarm.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import be.kdr.agvalarm.model.NetworkUiState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class NetworkMonitor(context: Context) {

    private val appContext = context.applicationContext
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val changes: Flow<Unit> = callbackFlow {
        val emitChange = { trySend(Unit) }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                emitChange()
            }

            override fun onLost(network: Network) {
                emitChange()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                emitChange()
            }

            override fun onUnavailable() {
                emitChange()
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)

        val wifiReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                emitChange()
            }
        }
        val filter = IntentFilter().apply {
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(wifiReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            appContext.registerReceiver(wifiReceiver, filter)
        }

        emitChange()
        awaitClose {
            runCatching { connectivityManager.unregisterNetworkCallback(callback) }
            runCatching { appContext.unregisterReceiver(wifiReceiver) }
        }
    }

    fun currentNetwork(): NetworkUiState {
        val active = connectivityManager.activeNetwork
        val caps = active?.let { connectivityManager.getNetworkCapabilities(it) }
            ?: return NetworkUiState(transportLabel = "geen netwerk")

        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                val ssid = currentSsid(caps)
                NetworkUiState(
                    transportLabel = ssid ?: "Wi-Fi",
                    ssid = ssid,
                )
            }
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->
                NetworkUiState(transportLabel = "mobiel")
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->
                NetworkUiState(transportLabel = "ethernet")
            else -> NetworkUiState(transportLabel = "ander netwerk")
        }
    }

    fun currentSsid(): String? = currentNetwork().ssid

    private fun currentSsid(caps: NetworkCapabilities): String? {
        val fromCaps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            (caps.transportInfo as? WifiInfo)?.ssid
        } else {
            null
        }
        val raw = fromCaps ?: legacySsid()
        return sanitizeSsid(raw)
    }

    @Suppress("DEPRECATION")
    private fun legacySsid(): String? {
        val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return wifiManager.connectionInfo?.ssid
    }

    companion object {
        fun sanitizeSsid(raw: String?): String? {
            if (raw.isNullOrBlank()) return null
            val trimmed = raw.trim().removeSurrounding("\"")
            if (trimmed.isEmpty() || trimmed.equals("<unknown ssid>", ignoreCase = true)) {
                return null
            }
            return trimmed
        }
    }
}
