package be.kdr.agvalarm.mqtt

import be.kdr.agvalarm.data.TopicSubscriptions
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject

data class AlarmVerdict(
    val notify: Boolean,
    val isActiveAlarm: Boolean,
    val isResolved: Boolean,
    val isQualityRobot: Boolean,
    val isAgv: Boolean,
    val vehicleId: String?,
    val message: String?,
) {
    companion object {
        val None = AlarmVerdict(
            notify = false,
            isActiveAlarm = false,
            isResolved = false,
            isQualityRobot = false,
            isAgv = false,
            vehicleId = null,
            message = null,
        )
    }
}

/**
 * Classifies MQTT traffic. AGV fleet alarms arrive as retained
 * `stubbe/agv/{id}/alarm` from a PC-KDR sidecar (not SQL in the app).
 * quality/status is the Fanuc quality-cell robot, not the AGV fleet.
 */
object AlarmClassifier {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val alarmKeywords = listOf("alarm", "error", "fault", "storing")
    private val errorStateValues = setOf("error", "fault", "alarm", "stopped", "storing")
    private val jsonAlarmKeys = listOf("alarm", "error", "robotInError")
    private val jsonStateKeys = listOf("severity", "state", "status")
    private val agvIdKeys = listOf("vehicleId", "vehicle_id", "agv", "agvId", "agv_id", "vehicle")
    private val agvSegment = Regex("""agv[-_]?\d+""", RegexOption.IGNORE_CASE)
    private val stubbeAgvAlarm = Regex("""^stubbe/agv/([^/]+)/alarm$""", RegexOption.IGNORE_CASE)

    fun evaluate(topic: String, payload: String): AlarmVerdict {
        if (TopicSubscriptions.isCommandTopic(topic)) return AlarmVerdict.None
        if (topic.startsWith("inventory/", ignoreCase = true)) return AlarmVerdict.None

        val obj = parseObject(payload)
        val vehicleId = extractAgvId(topic, payload)
        val message = obj.stringValue("message")

        if (topic.equals("quality/status", ignoreCase = true)) {
            val active = isQualityRobotAlarm(obj, payload)
            return AlarmVerdict(
                notify = active,
                isActiveAlarm = active,
                isResolved = false,
                isQualityRobot = true,
                isAgv = false,
                vehicleId = null,
                message = message,
            )
        }

        val stubbeMatch = stubbeAgvAlarm.matchEntire(topic)
        if (stubbeMatch != null) {
            val id = obj.stringValue("vehicleId") ?: stubbeMatch.groupValues[1]
            val alarmOn = isTruthyAlarmField(obj?.get("alarm")) || isTruthyAlarmField(obj?.get("error"))
            val explicitlyOff = obj != null &&
                !isTruthyAlarmField(obj["alarm"]) &&
                !isTruthyAlarmField(obj["error"]) &&
                (obj.containsKey("alarm") || obj.containsKey("error"))
            return if (alarmOn) {
                AlarmVerdict(
                    notify = true,
                    isActiveAlarm = true,
                    isResolved = false,
                    isQualityRobot = false,
                    isAgv = true,
                    vehicleId = id,
                    message = message ?: "AGV $id in error.",
                )
            } else if (explicitlyOff) {
                AlarmVerdict(
                    notify = false,
                    isActiveAlarm = false,
                    isResolved = true,
                    isQualityRobot = false,
                    isAgv = true,
                    vehicleId = id,
                    message = message,
                )
            } else {
                AlarmVerdict(
                    notify = true,
                    isActiveAlarm = true,
                    isResolved = false,
                    isQualityRobot = false,
                    isAgv = true,
                    vehicleId = id,
                    message = message ?: "AGV $id storing",
                )
            }
        }

        if (obj != null && (obj.containsKey("alarm") || obj.containsKey("error"))) {
            val on = isTruthyAlarmField(obj["alarm"]) || isTruthyAlarmField(obj["error"])
            if (!on) {
                return AlarmVerdict.None.copy(vehicleId = vehicleId, message = message)
            }
            return AlarmVerdict(
                notify = true,
                isActiveAlarm = true,
                isResolved = false,
                isQualityRobot = false,
                isAgv = vehicleId != null,
                vehicleId = vehicleId,
                message = message,
            )
        }

        val generic = when {
            containsKeyword(topic) -> true
            obj != null && isAlarmJsonObject(obj) -> true
            obj != null && jsonTextContainsKeyword(obj) -> true
            obj == null && containsKeyword(payload) -> true
            else -> false
        }
        return AlarmVerdict(
            notify = generic,
            isActiveAlarm = generic,
            isResolved = false,
            isQualityRobot = false,
            isAgv = vehicleId != null,
            vehicleId = vehicleId,
            message = message,
        )
    }

    fun isAlarm(topic: String, payload: String): Boolean = evaluate(topic, payload).isActiveAlarm

    fun shouldNotify(topic: String, payload: String): Boolean = evaluate(topic, payload).notify

    fun notificationTitle(topic: String, payload: String): String {
        val verdict = evaluate(topic, payload)
        if (verdict.isQualityRobot) return "Kwaliteitsrobot storing"
        val id = verdict.vehicleId
        if (verdict.isAgv && !id.isNullOrBlank()) return "AGV $id storing"
        return id?.takeIf { it.isNotBlank() }?.let { "AGV $it storing" } ?: "AGV storing"
    }

    fun extractAgvId(topic: String, payload: String): String? {
        if (topic.equals("quality/status", ignoreCase = true)) return null
        stubbeAgvAlarm.matchEntire(topic)?.let { match ->
            parseObject(payload).stringValue("vehicleId")?.let { return it }
            return match.groupValues[1]
        }
        parseObject(payload)?.let { obj ->
            for (key in agvIdKeys) {
                val value = obj.stringValue(key)
                if (!value.isNullOrBlank()) return value.trim()
            }
        }
        val segments = topic.split('/').filter { it.isNotBlank() }
        segments.forEachIndexed { index, segment ->
            if (agvSegment.matches(segment)) return segment
            if (segment.equals("agv", ignoreCase = true) && index + 1 < segments.size) {
                return segments[index + 1]
            }
        }
        return null
    }

    fun notificationId(verdict: AlarmVerdict): Int {
        if (verdict.isQualityRobot) return QUALITY_NOTIFICATION_ID
        val n = verdict.vehicleId?.toIntOrNull()
        return if (n != null) AGV_NOTIFICATION_BASE + n else AGV_NOTIFICATION_FALLBACK
    }

    private fun isQualityRobotAlarm(obj: JsonObject?, payload: String): Boolean {
        if (obj == null) return containsKeyword(payload)
        if (isTruthyAlarmField(obj["robotInError"])) return true
        if (isTruthyAlarmField(obj["error"])) return true
        if (isTruthyAlarmField(obj["alarm"])) return true
        val summary = obj.stringValue("robotActiveAlarmsSummaryDisplay")
        return !summary.isNullOrBlank()
    }

    private fun isAlarmJsonObject(obj: JsonObject): Boolean {
        for (key in jsonAlarmKeys) {
            if (isTruthyAlarmField(obj[key])) return true
        }
        for (key in jsonStateKeys) {
            val value = obj.stringValue(key)?.lowercase()?.trim() ?: continue
            if (value in errorStateValues) return true
        }
        return false
    }

    private fun jsonTextContainsKeyword(obj: JsonObject): Boolean {
        for ((_, value) in obj) {
            when (value) {
                is JsonPrimitive -> {
                    val text = value.contentOrNull ?: continue
                    if (containsKeyword(text)) return true
                }
                is JsonObject -> if (jsonTextContainsKeyword(value)) return true
                is JsonArray -> {
                    for (el in value) {
                        if (el is JsonObject && jsonTextContainsKeyword(el)) return true
                        if (el is JsonPrimitive && containsKeyword(el.contentOrNull.orEmpty())) return true
                    }
                }
                else -> Unit
            }
        }
        return false
    }

    private fun isTruthyAlarmField(element: JsonElement?): Boolean {
        if (element == null || element is JsonNull) return false
        return when (element) {
            is JsonPrimitive -> {
                val bool = element.booleanOrNull
                if (bool != null) return bool
                val number = element.doubleOrNull
                if (number != null) return number != 0.0
                val text = element.contentOrNull?.trim()?.lowercase().orEmpty()
                if (text.isEmpty()) return false
                text !in setOf("false", "0", "ok", "none", "null", "normal", "off")
            }
            is JsonObject, is JsonArray -> true
            else -> false
        }
    }

    private fun containsKeyword(text: String): Boolean {
        val lower = text.lowercase()
        return alarmKeywords.any { it in lower }
    }

    private fun parseObject(payload: String): JsonObject? {
        val trimmed = payload.trim()
        if (trimmed.isEmpty() || trimmed[0] != '{') return null
        return try {
            json.parseToJsonElement(trimmed).jsonObject
        } catch (_: Exception) {
            null
        }
    }

    private fun JsonObject?.stringValue(key: String): String? {
        val element = this?.get(key) ?: return null
        if (element is JsonNull) return null
        if (element is JsonPrimitive) {
            return element.contentOrNull?.takeIf { it.isNotBlank() }
        }
        return null
    }

    const val QUALITY_NOTIFICATION_ID = 1900
    const val AGV_NOTIFICATION_BASE = 2000
    const val AGV_NOTIFICATION_FALLBACK = 2998
}
