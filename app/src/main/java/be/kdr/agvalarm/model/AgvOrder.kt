package be.kdr.agvalarm.model

data class AgvWmsOrder(
    val id: Long? = null,
    val jbtOrderId: Long? = null,
    val status: String? = null,
    val wmsId: String? = null,
    val origin: String? = null,
    val destination: String? = null,
    val sku: String? = null,
    val startTime: String? = null,
    val jobId: Long? = null,
    val batchNr: String? = null,
    val machName: String? = null,
    val progress: Int? = null,
) {
    val isHomeOrCharge: Boolean
        get() = isParkName(destination)
}

data class AgvVehicleState(
    val vehicleId: Int,
    val name: String,
    val busy: Boolean,
    val loaded: Boolean? = null,
    val batteryLevel: Int? = null,
    val error: Boolean = false,
    val currentNode: Int? = null,
    val etaSeconds: Int? = null,
    val ts: String? = null,
    val order: AgvWmsOrder? = null,
    val updatedAtMillis: Long = 0L,
) {
    /** WMS work only — idle HOME/ChargePark is not an order. */
    val hasActiveWmsOrder: Boolean
        get() = busy && order?.isHomeOrCharge != true

    val displayName: String
        get() = name.ifBlank { "AGV$vehicleId" }

    val routeLabel: String?
        get() {
            val from = order?.origin?.takeIf { it.isNotBlank() }
            val to = order?.destination?.takeIf { it.isNotBlank() }
            return when {
                from != null && to != null -> "$from → $to"
                to != null -> to
                from != null -> from
                else -> null
            }
        }
}

fun isParkName(value: String?): Boolean {
    val t = value?.trim()?.uppercase().orEmpty()
    if (t.isEmpty()) return false
    if (t == "HOME") return true
    if (t == "CHARGEPARK") return true
    if (t.contains("CHARGE") && t.contains("PARK")) return true
    return false
}
