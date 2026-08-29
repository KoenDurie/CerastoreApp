package be.kdr.agvalarm.mqtt

import be.kdr.agvalarm.data.AppSettings
import be.kdr.agvalarm.model.BrokerTarget

class BrokerSelector(
    private val probe: (host: String, port: Int) -> Boolean = { host, port ->
        BrokerProbe.isReachable(host, port)
    },
) {
    fun select(
        settings: AppSettings,
        currentSsid: String?,
        vpnActive: Boolean = false,
    ): BrokerTarget {
        val stubbe = BrokerTarget.Stubbe(settings.stubbeHost, settings.stubbePort)
        val home = BrokerTarget.Home(settings.homeHost, settings.homePort)
        val stubbeSsid = settings.stubbeSsid.trim()
        if (stubbeSsid.isNotEmpty() && currentSsid.equalsSsid(stubbeSsid)) {
            return stubbe
        }
        val homeSsid = settings.homeSsid.trim().ifBlank { AppSettings.DEFAULT_HOME_SSID }
        if (currentSsid.equalsSsid(homeSsid) && !vpnActive) {
            return home
        }
        return if (probe(settings.stubbeHost, settings.stubbePort)) stubbe else home
    }

    private fun String?.equalsSsid(configured: String): Boolean =
        this != null && configured.isNotEmpty() && this.equals(configured, ignoreCase = true)
}
