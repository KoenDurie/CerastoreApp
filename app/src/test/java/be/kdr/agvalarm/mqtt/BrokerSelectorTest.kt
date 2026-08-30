package be.kdr.agvalarm.mqtt

import be.kdr.agvalarm.data.AppSettings
import be.kdr.agvalarm.model.BrokerTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrokerSelectorTest {

    @Test
    fun ssidMatchSelectsStubbeWithoutProbingHome() {
        var probed = false
        val selector = BrokerSelector { _, _ ->
            probed = true
            false
        }
        val settings = AppSettings(stubbeSsid = "StubbeHall")
        val target = selector.select(settings, "StubbeHall")
        assertTrue(target is BrokerTarget.Stubbe)
        assertEquals("10.0.0.20", target.host)
        assertFalse(probed)
    }

    @Test
    fun homeSsidWithoutVpnSelectsHomeAndSkipsStubbeProbe() {
        var probedHost: String? = null
        val selector = BrokerSelector { host, _ ->
            probedHost = host
            true
        }
        val target = selector.select(
            AppSettings(),
            currentSsid = "telenet-7E9C4",
            vpnActive = false,
        )
        assertTrue(target is BrokerTarget.Home)
        assertEquals("192.168.0.239", target.host)
        assertEquals(null, probedHost)
    }

    @Test
    fun homeSsidWithVpnProbesStubbe() {
        val selector = BrokerSelector { host, _ -> host == "10.0.0.20" }
        val target = selector.select(
            AppSettings(),
            currentSsid = "telenet-7E9C4",
            vpnActive = true,
        )
        assertTrue(target is BrokerTarget.Stubbe)
    }

    @Test
    fun reachableStubbeWinsOverHome() {
        val selector = BrokerSelector { host, _ -> host == "10.0.0.20" }
        val target = selector.select(AppSettings(), currentSsid = null)
        assertTrue(target is BrokerTarget.Stubbe)
    }

    @Test
    fun unreachableStubbeFallsBackToHomeLanIp() {
        val selector = BrokerSelector { _, _ -> false }
        val target = selector.select(AppSettings(), currentSsid = "other-net")
        assertTrue(target is BrokerTarget.Home)
        assertEquals("192.168.0.239", target.host)
    }
}
