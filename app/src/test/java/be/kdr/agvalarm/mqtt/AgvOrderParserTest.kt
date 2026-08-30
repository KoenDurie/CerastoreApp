package be.kdr.agvalarm.mqtt

import be.kdr.agvalarm.data.TopicSubscriptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgvOrderParserTest {

    private val busy = """
        {"vehicleId":5,"name":"AGV5","busy":true,"loaded":true,"batteryLevel":90,"error":false,"currentNode":3029,"etaSeconds":42,"ts":"2026-08-28T15:46:44.463Z","order":{"id":650664,"jbtOrderId":981296,"status":"Active","wmsId":null,"origin":"MATCOPICK","destination":"L001_8","sku":"RBZW40DE8,10TT","startTime":"2026-08-28T15:46:44.463Z","jobId":24149,"batchNr":null,"machName":null,"progress":0}}
    """.trimIndent()

    private val idle = """
        {"vehicleId":5,"name":"AGV5","busy":false,"loaded":false,"batteryLevel":88,"error":false,"currentNode":100,"etaSeconds":null,"ts":"2026-08-28T16:00:00.000Z","order":null}
    """.trimIndent()

    @Test
    fun busyOrderIsActiveWmsWork() {
        val state = AgvOrderParser.parse("stubbe/agv/5/order", busy).single()
        assertEquals(5, state.vehicleId)
        assertEquals("AGV5", state.name)
        assertTrue(state.busy)
        assertTrue(state.hasActiveWmsOrder)
        assertEquals("MATCOPICK → L001_8", state.routeLabel)
        assertEquals("RBZW40DE8,10TT", state.order?.sku)
        assertEquals(42, state.etaSeconds)
        assertEquals(90, state.batteryLevel)
        assertEquals(true, state.loaded)
        assertFalse(state.error)
        assertEquals(24149L, state.order?.jobId)
        assertEquals(650664L, state.order?.id)
    }

    @Test
    fun idleOrderIsNotAlarmAndNotWmsWork() {
        val state = AgvOrderParser.parse("stubbe/agv/5/order", idle).single()
        assertFalse(state.busy)
        assertFalse(state.hasActiveWmsOrder)
        assertFalse(AlarmClassifier.isAlarm("stubbe/agv/5/order", idle))
        assertFalse(AlarmClassifier.shouldNotify("stubbe/agv/5/order", idle))
    }

    @Test
    fun busyJsonIsNotAnAlarm() {
        assertFalse(AlarmClassifier.isAlarm("stubbe/agv/5/order", busy))
        assertFalse(AlarmClassifier.shouldNotify("stubbe/agv/5/order", busy))
        assertFalse(AlarmClassifier.evaluate("stubbe/agv/5/order", busy).isActiveAlarm)
    }

    @Test
    fun homeDestinationIsIgnoredEvenIfBusy() {
        val payload = """{"vehicleId":3,"name":"AGV3","busy":true,"error":false,"order":{"origin":"L001_8","destination":"HOME"}}"""
        val state = AgvOrderParser.parse("stubbe/agv/3/order", payload).single()
        assertTrue(state.busy)
        assertFalse(state.hasActiveWmsOrder)
        assertFalse(AlarmClassifier.isAlarm("stubbe/agv/3/order", payload))
    }

    @Test
    fun chargeParkDestinationIsIgnored() {
        val payload = """{"vehicleId":4,"name":"AGV4","busy":true,"order":{"origin":"MATCOPICK","destination":"ChargePark"}}"""
        val state = AgvOrderParser.parse("stubbe/agv/4/order", payload).single()
        assertFalse(state.hasActiveWmsOrder)
    }

    @Test
    fun snapshotArrayUpdatesFleet() {
        val snap = """[{"vehicleId":2,"name":"AGV2","busy":false,"order":null},$busy]"""
        val states = AgvOrderParser.parse("stubbe/agv/orders", snap)
        assertEquals(2, states.size)
        assertFalse(states.first { it.vehicleId == 2 }.hasActiveWmsOrder)
        assertTrue(states.first { it.vehicleId == 5 }.hasActiveWmsOrder)
        assertFalse(AlarmClassifier.isAlarm("stubbe/agv/orders", snap))
    }

    @Test
    fun topicIdFillsMissingVehicleId() {
        val payload = """{"name":"AGV7","busy":true,"order":{"origin":"A","destination":"B"}}"""
        val state = AgvOrderParser.parse("stubbe/agv/7/order", payload).single()
        assertEquals(7, state.vehicleId)
        assertTrue(state.hasActiveWmsOrder)
    }

    @Test
    fun inventoryTopicIsIgnoredByParser() {
        assertTrue(AgvOrderParser.parse("inventory/x", busy).isEmpty())
        assertTrue(AgvOrderParser.parse("stubbe/agv/5/alarm", busy).isEmpty())
    }

    @Test
    fun orderTopicHelpers() {
        assertTrue(TopicSubscriptions.isOrderTopic("stubbe/agv/5/order"))
        assertTrue(TopicSubscriptions.isOrderTopic("stubbe/agv/orders"))
        assertFalse(TopicSubscriptions.isOrderTopic("stubbe/agv/5/alarm"))
        assertFalse(TopicSubscriptions.isOrderTopic("stubbe/agv/#"))
    }
}
