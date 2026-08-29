package be.kdr.agvalarm.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopicSubscriptionsTest {

    @Test
    fun defaultFiltersAreInventoryAndQuality() {
        assertEquals(
            listOf("inventory/#", "quality/status"),
            TopicSubscriptions.filters(""),
        )
    }

    @Test
    fun extraHashAddsDiscoveryFilter() {
        assertEquals(
            listOf("inventory/#", "quality/status", "#"),
            TopicSubscriptions.filters("#"),
        )
    }

    @Test
    fun commandTopicsAreIdentified() {
        assertTrue(TopicSubscriptions.isCommandTopic("quality/robot/cmd"))
        assertTrue(TopicSubscriptions.isCommandTopic("quality/robot/ack"))
        assertFalse(TopicSubscriptions.isCommandTopic("quality/status"))
    }
}
