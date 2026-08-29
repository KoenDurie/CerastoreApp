package be.kdr.agvalarm.mqtt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmClassifierTest {

    @Test
    fun topicContainingAlarmIsAlarm() {
        assertTrue(AlarmClassifier.isAlarm("stubbe/agv1/alarm", """{"ok":true}"""))
    }

    @Test
    fun topicContainingErrorIsAlarm() {
        assertTrue(AlarmClassifier.isAlarm("factory/line/error/12", "running"))
    }

    @Test
    fun topicContainingFaultIsAlarm() {
        assertTrue(AlarmClassifier.isAlarm("jbt/fault", ""))
    }

    @Test
    fun topicContainingStoringIsAlarm() {
        assertTrue(AlarmClassifier.isAlarm("agv/storing", "x"))
    }

    @Test
    fun jsonStateErrorIsAlarm() {
        assertTrue(AlarmClassifier.isAlarm("telemetry", """{"state":"error"}"""))
    }

    @Test
    fun jsonStatusStoppedIsAlarm() {
        assertTrue(AlarmClassifier.isAlarm("telemetry", """{"status":"stopped"}"""))
    }

    @Test
    fun jsonSeverityFaultIsAlarm() {
        assertTrue(AlarmClassifier.isAlarm("telemetry", """{"severity":"fault"}"""))
    }

    @Test
    fun jsonAlarmTrueIsAlarm() {
        assertTrue(AlarmClassifier.isAlarm("telemetry", """{"alarm":true}"""))
    }

    @Test
    fun jsonAlarmFalseIsNotAlarm() {
        assertFalse(AlarmClassifier.isAlarm("telemetry", """{"alarm":false,"state":"running"}"""))
    }

    @Test
    fun jsonMessageValueWithErrorIsAlarm() {
        assertTrue(AlarmClassifier.isAlarm("telemetry", """{"message":"drive error on motor 2"}"""))
    }

    @Test
    fun jsonErrorFieldStringIsAlarm() {
        assertTrue(AlarmClassifier.isAlarm("telemetry", """{"error":"E-STOP"}"""))
    }

    @Test
    fun jsonRunningIsNotAlarm() {
        assertFalse(AlarmClassifier.isAlarm("agv/01/status", """{"state":"running","status":"ok"}"""))
    }

    @Test
    fun payloadTextStoringIsAlarm() {
        assertTrue(AlarmClassifier.isAlarm("logs", "storing op lijn 3"))
    }

    @Test
    fun payloadTextErrorIsAlarm() {
        assertTrue(AlarmClassifier.isAlarm("logs", "Drive error on motor 2"))
    }

    @Test
    fun emptyTelemetryIsNotAlarm() {
        assertFalse(AlarmClassifier.isAlarm("sensors/temp", "21.4"))
    }

    @Test
    fun extractsVehicleIdFromJson() {
        assertEquals("JBT-01", AlarmClassifier.extractAgvId("x", """{"vehicleId":"JBT-01"}"""))
    }

    @Test
    fun extractsAgvFromJson() {
        assertEquals("12", AlarmClassifier.extractAgvId("x", """{"agv":"12"}"""))
    }

    @Test
    fun extractsAgvSegmentFromTopic() {
        assertEquals("agv-12", AlarmClassifier.extractAgvId("stubbe/agv-12/error", "{}"))
    }

    @Test
    fun extractsSegmentAfterAgv() {
        assertEquals("7", AlarmClassifier.extractAgvId("site/agv/7/status", "{}"))
    }

    @Test
    fun missingAgvIdIsNull() {
        assertNull(AlarmClassifier.extractAgvId("factory/line/status", """{"state":"ok"}"""))
    }

    @Test
    fun qualityStatusRobotInErrorIsAlarm() {
        assertTrue(
            AlarmClassifier.isAlarm(
                "quality/status",
                """{"robotInError":true,"operationMode":"AUTO"}""",
            ),
        )
    }

    @Test
    fun qualityStatusErrorFieldIsAlarm() {
        assertTrue(
            AlarmClassifier.isAlarm(
                "quality/status",
                """{"error":"E-STOP","robotInError":false}""",
            ),
        )
    }

    @Test
    fun qualityStatusHealthyIsNotAlarm() {
        assertFalse(
            AlarmClassifier.isAlarm(
                "quality/status",
                """{"error":"","robotInError":false,"robotActiveAlarmsSummaryDisplay":"","operationMode":"AUTO"}""",
            ),
        )
    }

    @Test
    fun qualityStatusAlarmSummaryIsAlarm() {
        assertTrue(
            AlarmClassifier.isAlarm(
                "quality/status",
                """{"robotInError":false,"robotActiveAlarmsSummaryDisplay":"SRVO-001"}""",
            ),
        )
    }

    @Test
    fun qualityStatusNotificationTitleIsRobot() {
        assertEquals(
            "Kwaliteitsrobot storing",
            AlarmClassifier.notificationTitle("quality/status", """{"robotInError":true}"""),
        )
    }

    @Test
    fun inventoryJsonIsNotAlarm() {
        assertFalse(
            AlarmClassifier.isAlarm(
                "inventory/SnijLijn",
                """{"locationName":"SnijLijn","productName":"Board","color":"#fff","textColor":"#000"}""",
            ),
        )
    }

    @Test
    fun commandTopicsAreNotAlarms() {
        assertFalse(AlarmClassifier.isAlarm("quality/robot/cmd", """{"cmd":"start"}"""))
        assertFalse(AlarmClassifier.isAlarm("quality/robot/ack", """{"ok":true}"""))
    }

    @Test
    fun stubbeAgvAlarmTrueNotifies() {
        val payload = """{"vehicleId":7,"alarm":true,"error":true,"state":"error","message":"AGV 7 in error."}"""
        val verdict = AlarmClassifier.evaluate("stubbe/agv/7/alarm", payload)
        assertTrue(verdict.notify)
        assertTrue(verdict.isActiveAlarm)
        assertFalse(verdict.isResolved)
        assertTrue(verdict.isAgv)
        assertEquals("7", verdict.vehicleId)
        assertEquals("AGV 7 in error.", verdict.message)
        assertEquals("AGV 7 storing", AlarmClassifier.notificationTitle("stubbe/agv/7/alarm", payload))
        assertEquals(2007, AlarmClassifier.notificationId(verdict))
    }

    @Test
    fun stubbeAgvAlarmFalseDoesNotNotify() {
        val payload = """{"vehicleId":7,"alarm":false,"error":false,"state":"ok"}"""
        val verdict = AlarmClassifier.evaluate("stubbe/agv/7/alarm", payload)
        assertFalse(verdict.notify)
        assertFalse(verdict.isActiveAlarm)
        assertTrue(verdict.isResolved)
        assertFalse(AlarmClassifier.shouldNotify("stubbe/agv/7/alarm", payload))
    }

    @Test
    fun twoAgvsGetDistinctNotificationIds() {
        val a = AlarmClassifier.evaluate(
            "stubbe/agv/3/alarm",
            """{"vehicleId":3,"alarm":true,"error":true,"state":"error"}""",
        )
        val b = AlarmClassifier.evaluate(
            "stubbe/agv/8/alarm",
            """{"vehicleId":8,"alarm":true,"error":true,"state":"error"}""",
        )
        assertTrue(AlarmClassifier.notificationId(a) != AlarmClassifier.notificationId(b))
    }

    @Test
    fun qualityNotificationIdIsNotAgv() {
        val quality = AlarmClassifier.evaluate("quality/status", """{"robotInError":true}""")
        val agv = AlarmClassifier.evaluate(
            "stubbe/agv/1/alarm",
            """{"vehicleId":1,"alarm":true,"error":true,"state":"error"}""",
        )
        assertEquals(1900, AlarmClassifier.notificationId(quality))
        assertTrue(AlarmClassifier.notificationId(quality) != AlarmClassifier.notificationId(agv))
    }
}
