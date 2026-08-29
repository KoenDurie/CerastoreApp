package be.kdr.agvalarm.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopicSubscriptionsTest {

    @Test
    fun defaultFiltersIncludeAgvAlarms() {
        assertEquals(
            listOf("inventory/#", "quality/status", "stubbe/agv/#"),
            TopicSubscriptions.filters(""),
        )
    }

    @Test
    fun extraHashAddsDiscoveryFilter() {
        assertEquals(
            listOf("inventory/#", "quality/status", "stubbe/agv/#", "#"),
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
