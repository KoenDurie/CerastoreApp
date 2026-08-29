package be.kdr.agvalarm.mqtt

import be.kdr.agvalarm.data.AppSettings
import be.kdr.agvalarm.model.BrokerTarget

class BrokerSelector(
    private val probe: (host: String, port: Int) -> Boolean = { host, port ->
        BrokerProbe.isReachable(host, port)
    },
) {
    fun select(settings: AppSettings, currentSsid: String?): BrokerTarget {
        val stubbe = BrokerTarget.Stubbe(settings.stubbeHost, settings.stubbePort)
        val home = BrokerTarget.Home(settings.homeHost, settings.homePort)
        val configuredSsid = settings.stubbeSsid.trim()
        if (configuredSsid.isNotEmpty() &&
            currentSsid != null &&
            currentSsid.equals(configuredSsid, ignoreCase = true)
        ) {
            return stubbe
        }
        return if (probe(settings.stubbeHost, settings.stubbePort)) stubbe else home
    }
}
