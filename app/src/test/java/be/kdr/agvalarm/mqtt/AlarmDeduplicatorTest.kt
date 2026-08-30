package be.kdr.agvalarm.mqtt

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmDeduplicatorTest {

    @Test
    fun sameTopicAndPayloadWithinWindowIsDeduped() {
        var now = 1_000L
        val dedupe = AlarmDeduplicator(windowMs = 60_000L, clock = { now })
        assertTrue(dedupe.shouldNotify("agv/1/alarm", "estop"))
        assertFalse(dedupe.shouldNotify("agv/1/alarm", "estop"))
        now = 30_000L
        assertFalse(dedupe.shouldNotify("agv/1/alarm", "estop"))
        now = 62_000L
        assertTrue(dedupe.shouldNotify("agv/1/alarm", "estop"))
    }

    @Test
    fun differentPayloadIsNotDeduped() {
        val dedupe = AlarmDeduplicator(windowMs = 60_000L, clock = { 0L })
        assertTrue(dedupe.shouldNotify("agv/1/alarm", "a"))
        assertTrue(dedupe.shouldNotify("agv/1/alarm", "b"))
    }
}
