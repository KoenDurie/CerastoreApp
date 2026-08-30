package be.kdr.agvalarm.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import be.kdr.agvalarm.model.AgvVehicleState
import be.kdr.agvalarm.ui.dashboard.GlassCard
import be.kdr.agvalarm.ui.theme.AccentOrange
import be.kdr.agvalarm.ui.theme.CardWhite
import be.kdr.agvalarm.ui.theme.Coral
import be.kdr.agvalarm.ui.theme.Ink
import be.kdr.agvalarm.ui.theme.LabelGrey
import be.kdr.agvalarm.ui.theme.TrackGrey
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val FleetIds = 2..10
private val Brussels: ZoneId = ZoneId.of("Europe/Brussels")
private val TimeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM HH:mm:ss")

enum class AgvDotKind { Idle, Busy, Error }

@Composable
fun AgvOrderStrip(
    vehicles: Map<Int, AgvVehicleState>,
    alarmIds: Set<String>,
    onOpen: (AgvVehicleState) -> Unit,
) {
    val busy = vehicles.values
        .filter { it.hasActiveWmsOrder }
        .sortedBy { it.vehicleId }
    GlassCard {
        Text("AGV-orders", style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(8.dp))
        FleetDots(
            vehicles = vehicles,
            alarmIds = alarmIds,
            onOpenBusy = onOpen,
        )
        Spacer(Modifier.height(10.dp))
        if (busy.isEmpty()) {
            Text(
                "Geen AGV met een order",
                style = MaterialTheme.typography.bodyMedium,
                color = LabelGrey,
            )
        } else {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                busy.forEach { state ->
                    OrderChip(state = state, onClick = { onOpen(state) })
                }
            }
        }
    }
}

@Composable
private fun FleetDots(
    vehicles: Map<Int, AgvVehicleState>,
    alarmIds: Set<String>,
    onOpenBusy: (AgvVehicleState) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        FleetIds.forEach { id ->
            val state = vehicles[id]
            val kind = when {
                state?.error == true || alarmIds.contains(id.toString()) -> AgvDotKind.Error
                state?.hasActiveWmsOrder == true -> AgvDotKind.Busy
                else -> AgvDotKind.Idle
            }
            val color = when (kind) {
                AgvDotKind.Error -> Coral
                AgvDotKind.Busy -> AccentOrange
                AgvDotKind.Idle -> TrackGrey
            }
            val tappable = kind == AgvDotKind.Busy && state != null
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = if (tappable) {
                    Modifier.clickable { onOpenBusy(state!!) }
                } else {
                    Modifier
                },
            ) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(color),
                )
                Text(
                    text = id.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (kind == AgvDotKind.Idle) LabelGrey else Ink,
                )
            }
        }
    }
}

@Composable
private fun OrderChip(state: AgvVehicleState, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = state.displayName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AccentOrange,
        )
        Text(
            text = state.routeLabel ?: "bezig",
            style = MaterialTheme.typography.labelSmall,
            color = LabelGrey,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgvOrderSheet(
    state: AgvVehicleState,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CardWhite,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text("Order", style = MaterialTheme.typography.labelSmall, color = LabelGrey)
            Text(
                text = state.displayName,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Ink,
            )
            Text(
                text = "AGV ${state.vehicleId}",
                style = MaterialTheme.typography.bodyMedium,
                color = LabelGrey,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = state.routeLabel ?: "—",
                style = MaterialTheme.typography.headlineMedium,
                color = Ink,
            )
            Spacer(Modifier.height(12.dp))
            DetailRow("SKU", state.order?.sku)
            DetailRow("Status", state.order?.status)
            DetailRow("Geladen", state.loaded?.let { if (it) "Ja" else "Nee" })
            DetailRow("Batterij", state.batteryLevel?.let { "$it%" })
            DetailRow("ETA", formatEta(state.etaSeconds))
            DetailRow("Start", formatIso(state.order?.startTime ?: state.ts))
            DetailRow("Job", state.order?.jobId?.toString())
            DetailRow("Batch", state.order?.batchNr)
            DetailRow("Machine", state.order?.machName)
            DetailRow("Order-id", state.order?.id?.toString())
            DetailRow("JBT-order", state.order?.jbtOrderId?.toString())
            DetailRow("WMS-id", state.order?.wmsId)
            DetailRow("Node", state.currentNode?.toString())
            state.order?.progress?.let { DetailRow("Voortgang", "$it%") }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String?) {
    val shown = value?.takeIf { it.isNotBlank() } ?: return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = LabelGrey)
        Text(
            text = shown,
            style = MaterialTheme.typography.bodyLarge,
            color = Ink,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

private fun formatEta(seconds: Int?): String? {
    if (seconds == null) return null
    val s = seconds.coerceAtLeast(0)
    return when {
        s < 60 -> "$s s"
        s < 3600 -> "${s / 60} min"
        else -> "${s / 3600} u ${(s % 3600) / 60} min"
    }
}

private fun formatIso(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    return try {
        TimeFmt.format(Instant.parse(raw).atZone(Brussels))
    } catch (_: Exception) {
        raw
    }
}
