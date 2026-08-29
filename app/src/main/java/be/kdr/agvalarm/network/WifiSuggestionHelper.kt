package be.kdr.agvalarm.network

import android.content.Context
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.util.Log
import androidx.core.content.getSystemService
import android.net.wifi.WifiManager

class WifiSuggestionHelper(context: Context) {

    private val appContext = context.applicationContext
    private val wifiManager = appContext.getSystemService<WifiManager>()

    fun syncSuggestion(ssid: String, password: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val manager = wifiManager ?: return
        runCatching { manager.removeNetworkSuggestions(emptyList()) }
        val trimmedSsid = ssid.trim()
        if (trimmedSsid.isEmpty() || password.isEmpty()) return
        val suggestion = WifiNetworkSuggestion.Builder()
            .setSsid(trimmedSsid)
            .setWpa2Passphrase(password)
            .setIsAppInteractionRequired(false)
            .build()
        val status = manager.addNetworkSuggestions(listOf(suggestion))
        Log.i(TAG, "Wi-Fi suggestion for '$trimmedSsid' status=$status")
    }

    companion object {
        private const val TAG = "WifiSuggestion"
    }
}
