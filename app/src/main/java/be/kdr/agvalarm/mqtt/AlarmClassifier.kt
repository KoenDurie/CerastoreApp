package be.kdr.agvalarm.mqtt

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
import kotlinx.serialization.json.jsonPrimitive

/**
 * Classifies MQTT traffic as an AGV alarm using topic, JSON fields, and payload text.
 * Alarm topics do not exist yet; this is ready for when they appear on the broker.
 */
object AlarmClassifier {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val alarmKeywords = listOf("alarm", "error", "fault", "storing")
    private val errorStateValues = setOf("error", "fault", "alarm", "stopped", "storing")
    private val jsonAlarmKeys = listOf("alarm", "error")
    private val jsonStateKeys = listOf("severity", "state", "status")
    private val agvIdKeys = listOf("agv", "agvId", "agv_id", "vehicle", "vehicleId", "vehicle_id")
    private val agvSegment = Regex("""agv[-_]?\d+""", RegexOption.IGNORE_CASE)

    fun isAlarm(topic: String, payload: String): Boolean {
        if (containsKeyword(topic)) return true
        val obj = parseObject(payload)
        if (obj != null) {
            if (isAlarmJsonObject(obj)) return true
            if (jsonTextContainsKeyword(obj)) return true
            return false
        }
        return containsKeyword(payload)
    }

    fun extractAgvId(topic: String, payload: String): String? {
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

    private fun JsonObject.stringValue(key: String): String? {
        val element = this[key] ?: return null
        if (element is JsonNull) return null
        if (element is JsonPrimitive) {
            return element.contentOrNull?.takeIf { it.isNotBlank() }
        }
        return null
    }
}
