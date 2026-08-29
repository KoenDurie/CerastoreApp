package be.kdr.agvalarm.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.kdr.agvalarm.data.AppSettings
import be.kdr.agvalarm.model.BrokerTarget
import be.kdr.agvalarm.model.ConnectionStatus
import be.kdr.agvalarm.model.ConnectionUiState
import be.kdr.agvalarm.ui.HomeViewModel
import be.kdr.agvalarm.ui.dashboard.CoralPill
import be.kdr.agvalarm.ui.dashboard.GlassCard
import be.kdr.agvalarm.ui.dashboard.KpiCell
import be.kdr.agvalarm.ui.dashboard.LimeDot
import be.kdr.agvalarm.ui.dashboard.MiniGauge
import be.kdr.agvalarm.ui.dashboard.SegmentedBar
import be.kdr.agvalarm.ui.events.EventRow
import be.kdr.agvalarm.ui.theme.AccentOrange
import be.kdr.agvalarm.ui.theme.Coral
import be.kdr.agvalarm.ui.theme.Ink
import be.kdr.agvalarm.ui.theme.LabelGrey
import be.kdr.agvalarm.ui.theme.Lime
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenAlarms: () -> Unit,
) {
    val connection by viewModel.connection.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val network by viewModel.network.collectAsStateWithLifecycle()
    val notifications by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val highlightId by viewModel.highlightedEventId.collectAsStateWithLifecycle()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
    }

    val hasAlarm = events.any { it.isAlarm }
    val (hero, heroColor) = when {
        hasAlarm -> "STORING" to Coral
        connection.status == ConnectionStatus.CONNECTED -> "VERBONDEN" to Ink
        connection.status == ConnectionStatus.CONNECTING -> "VERBINDEN" to AccentOrange
        else -> "OFFLINE" to LabelGrey
    }
    val filled = when {
        hasAlarm -> 12
        connection.status == ConnectionStatus.CONNECTED -> 12
        connection.status == ConnectionStatus.CONNECTING -> 5
        else -> 1
    }
    val brokerName = when (connection.broker) {
        is BrokerTarget.Stubbe -> "Stubbe"
        is BrokerTarget.Home -> "Thuis"
        null -> "—"
    }
    val hostPort = connection.broker?.hostPort ?: "—"
    val lastAge = events.firstOrNull()?.let { ageLabel(now - it.timestampMillis) } ?: "—"
    val activeAgvs = events.filter { it.isAlarm && !it.agvId.isNullOrBlank() }
        .distinctBy { it.agvId }
    val homeOffline = connection.broker is BrokerTarget.Home &&
        connection.status != ConnectionStatus.CONNECTED

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = hero,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = heroColor,
            )
            Text(
                text = "$brokerName  ·  $hostPort",
                style = MaterialTheme.typography.bodyMedium,
                color = LabelGrey,
            )
            if (hasAlarm) {
                Spacer(Modifier.height(8.dp))
                CoralPill("AGV storing")
            }
        }
        item {
            SegmentedBar(filled = filled)
        }
        item {
            Row(Modifier.fillMaxWidth()) {
                KpiCell(value = brokerName, label = "broker")
                KpiCell(value = lastAge, label = "laatste bericht")
                KpiCell(
                    value = if (notifications) "Aan" else "Uit",
                    label = "meldingen",
                )
            }
        }
        if (homeOffline) {
            item {
                GlassCard {
                    Text(
                        AppSettings.HOME_UNREACHABLE_HINT,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Coral,
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GlassCard(modifier = Modifier.weight(1f)) {
                    Text("Verbinding", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (connection.status == ConnectionStatus.CONNECTED && !hasAlarm) {
                            LimeDot()
                            Spacer(Modifier.padding(4.dp))
                        }
                        Text(
                            hero,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = heroColor,
                        )
                    }
                    Text(network.transportLabel, style = MaterialTheme.typography.labelSmall)
                    Text(hostPort, style = MaterialTheme.typography.bodyMedium)
                }
                GlassCard(modifier = Modifier.weight(1f), sage = true) {
                    Text("Meldingen", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.8f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (notifications) "Aan" else "Uit",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = notifications,
                            onCheckedChange = viewModel::setNotificationsEnabled,
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = Lime,
                                checkedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFF5A624C),
                            ),
                        )
                    }
                    MiniGauge(
                        progress = when (connection.status) {
                            ConnectionStatus.CONNECTED -> 1f
                            ConnectionStatus.CONNECTING -> 0.4f
                            ConnectionStatus.OFFLINE -> 0.08f
                        },
                    )
                }
            }
        }
        if (activeAgvs.isNotEmpty()) {
            item {
                GlassCard(onExpand = onOpenAlarms) {
                    Text("Actieve AGV", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        activeAgvs.take(6).forEach { event ->
                            CoralPill("AGV ${event.agvId}")
                        }
                    }
                }
            }
        }
        item {
            GlassCard(onExpand = onOpenAlarms) {
                Text("Laatste MQTT", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(8.dp))
                if (events.isEmpty()) {
                    EmptyTraffic(connection)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        events.take(5).forEach { event ->
                            EventRow(
                                event = event,
                                highlighted = event.id == highlightId,
                                compact = true,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTraffic(connection: ConnectionUiState) {
    val text = when {
        connection.status == ConnectionStatus.CONNECTED ->
            "Nog geen MQTT-berichten. Geabonneerd op inventory/#, quality/status en stubbe/agv/#."
        else -> "Wachten op verbinding met de broker."
    }
    Text(text, style = MaterialTheme.typography.bodyMedium, color = LabelGrey)
}

private fun ageLabel(deltaMs: Long): String {
    val s = (deltaMs / 1000).coerceAtLeast(0)
    return when {
        s < 5 -> "nu"
        s < 60 -> "${s}s"
        s < 3600 -> "${s / 60}m"
        else -> "${s / 3600}u"
    }
}