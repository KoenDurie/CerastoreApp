package be.kdr.agvalarm.mqtt

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
        val settings = be.kdr.agvalarm.data.AppSettings(stubbeSsid = "StubbeHall")
        val target = selector.select(settings, "StubbeHall")
        assertTrue(target is be.kdr.agvalarm.model.BrokerTarget.Stubbe)
        assertEquals("10.0.0.20", target.host)
        assertFalse(probed)
    }

    @Test
    fun reachableStubbeWinsOverHome() {
        val selector = BrokerSelector { host, _ -> host == "10.0.0.20" }
        val target = selector.select(be.kdr.agvalarm.data.AppSettings(), currentSsid = null)
        assertTrue(target is be.kdr.agvalarm.model.BrokerTarget.Stubbe)
    }

    @Test
    fun unreachableStubbeFallsBackToHome() {
        val selector = BrokerSelector { _, _ -> false }
        val target = selector.select(be.kdr.agvalarm.data.AppSettings(), currentSsid = "HomeNet")
        assertTrue(target is be.kdr.agvalarm.model.BrokerTarget.Home)
        assertEquals("PC-KDR", target.host)
    }
}
