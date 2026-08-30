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
    fun orderTopicsAreCoveredByAgvWildcard() {
        assertTrue(TopicSubscriptions.DEFAULT.contains("stubbe/agv/#"))
        assertTrue(TopicSubscriptions.isOrderTopic("stubbe/agv/5/order"))
        assertTrue(TopicSubscriptions.isOrderTopic("stubbe/agv/orders"))
        assertEquals(
            listOf("stubbe/agv/+/order", "stubbe/agv/orders"),
            TopicSubscriptions.ORDER_FILTERS,
        )
    }

    @Test
    fun commandTopicsAreIdentified() {
        assertTrue(TopicSubscriptions.isCommandTopic("quality/robot/cmd"))
        assertTrue(TopicSubscriptions.isCommandTopic("quality/robot/ack"))
        assertFalse(TopicSubscriptions.isCommandTopic("quality/status"))
    }
}
