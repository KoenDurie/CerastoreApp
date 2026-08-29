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
}
