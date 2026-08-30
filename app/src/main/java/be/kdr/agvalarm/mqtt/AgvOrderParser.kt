package be.kdr.agvalarm.mqtt

import be.kdr.agvalarm.data.TopicSubscriptions
import be.kdr.agvalarm.model.AgvVehicleState
import be.kdr.agvalarm.model.AgvWmsOrder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Cerastore retained WMS-order traffic on `stubbe/agv/{id}/order`
 * or snapshot `stubbe/agv/orders`. Idle / HOME / ChargePark is not an alarm.
 */
object AgvOrderParser {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val vehicleFromTopic = Regex("""^stubbe/agv/([^/]+)/order$""", RegexOption.IGNORE_CASE)

    fun parse(topic: String, payload: String, nowMillis: Long = System.currentTimeMillis()): List<AgvVehicleState> {
        if (!TopicSubscriptions.isOrderTopic(topic)) return emptyList()
        val trimmed = payload.trim()
        if (trimmed.isEmpty()) return emptyList()
        val topicId = vehicleFromTopic.matchEntire(topic)?.groupValues?.get(1)
        return try {
            val element = json.parseToJsonElement(trimmed)
            parseElement(element, topicId, nowMillis)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseElement(
        element: JsonElement,
        topicId: String?,
        nowMillis: Long,
    ): List<AgvVehicleState> {
        return when (element) {
            is JsonArray -> element.mapNotNull { parseVehicle(it, topicId, nowMillis) }
            is JsonObject -> {
                val nested = element["vehicles"] ?: element["orders"] ?: element["agvs"]
                when (nested) {
                    is JsonArray -> nested.mapNotNull { parseVehicle(it, topicId, nowMillis) }
                    is JsonObject -> listOfNotNull(parseVehicle(nested, topicId, nowMillis))
                    else -> {
                        parseVehicle(element, topicId, nowMillis)?.let { listOf(it) }
                            ?: element.values.mapNotNull { parseVehicle(it, null, nowMillis) }
                    }
                }
            }
            else -> emptyList()
        }
    }

    private fun parseVehicle(
        element: JsonElement,
        topicId: String?,
        nowMillis: Long,
    ): AgvVehicleState? {
        val obj = element as? JsonObject ?: return null
        if (!looksLikeVehicle(obj) && topicId == null) return null
        val id = obj.intValue("vehicleId")
            ?: obj.intValue("vehicle_id")
            ?: obj.intValue("agvId")
            ?: topicId?.toIntOrNull()
            ?: return null
        val orderEl = obj["order"]
        val order = if (orderEl == null || orderEl is JsonNull) null else parseOrder(orderEl)
        val name = obj.stringValue("name")?.takeIf { it.isNotBlank() } ?: "AGV$id"
        return AgvVehicleState(
            vehicleId = id,
            name = name,
            busy = obj.booleanValue("busy") ?: false,
            loaded = obj.booleanValue("loaded"),
            batteryLevel = obj.intValue("batteryLevel") ?: obj.intValue("battery"),
            error = obj.booleanValue("error") ?: false,
            currentNode = obj.intValue("currentNode"),
            etaSeconds = obj.intValue("etaSeconds"),
            ts = obj.stringValue("ts"),
            order = order,
            updatedAtMillis = nowMillis,
        )
    }

    private fun parseOrder(element: JsonElement): AgvWmsOrder? {
        val obj = element as? JsonObject ?: return null
        return AgvWmsOrder(
            id = obj.longValue("id"),
            jbtOrderId = obj.longValue("jbtOrderId"),
            status = obj.stringValue("status"),
            wmsId = obj.stringValue("wmsId"),
            origin = obj.stringValue("origin"),
            destination = obj.stringValue("destination"),
            sku = obj.stringValue("sku"),
            startTime = obj.stringValue("startTime"),
            jobId = obj.longValue("jobId"),
            batchNr = obj.stringValue("batchNr"),
            machName = obj.stringValue("machName"),
            progress = obj.intValue("progress"),
        )
    }

    private fun looksLikeVehicle(obj: JsonObject): Boolean =
        obj.containsKey("vehicleId") ||
            obj.containsKey("vehicle_id") ||
            obj.containsKey("busy") ||
            obj.containsKey("order")

    private fun JsonObject.stringValue(key: String): String? {
        val element = get(key) ?: return null
        if (element is JsonNull) return null
        val prim = element as? JsonPrimitive ?: return null
        return prim.contentOrNull?.trim()?.takeIf { it.isNotEmpty() && it != "null" }
    }

    private fun JsonObject.booleanValue(key: String): Boolean? {
        val element = get(key) ?: return null
        if (element is JsonNull) return null
        val prim = element as? JsonPrimitive ?: return null
        prim.booleanOrNull?.let { return it }
        return when (prim.contentOrNull?.trim()?.lowercase()) {
            "true", "1" -> true
            "false", "0" -> false
            else -> null
        }
    }

    private fun JsonObject.intValue(key: String): Int? = longValue(key)?.toInt()

    private fun JsonObject.longValue(key: String): Long? {
        val element = get(key) ?: return null
        if (element is JsonNull) return null
        val prim = element as? JsonPrimitive ?: return null
        prim.longOrNull?.let { return it }
        prim.doubleOrNull?.let { return it.toLong() }
        return prim.contentOrNull?.trim()?.toLongOrNull()
    }
}
