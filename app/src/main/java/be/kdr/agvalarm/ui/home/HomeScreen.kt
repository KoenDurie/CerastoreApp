package be.kdr.agvalarm.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.kdr.agvalarm.data.AppSettings
import be.kdr.agvalarm.model.BrokerTarget
import be.kdr.agvalarm.model.ConnectionStatus
import be.kdr.agvalarm.model.ConnectionUiState
import be.kdr.agvalarm.model.MqttEvent
import be.kdr.agvalarm.ui.HomeViewModel
import be.kdr.agvalarm.ui.theme.AlarmRed
import be.kdr.agvalarm.ui.theme.Amber
import be.kdr.agvalarm.ui.theme.CharcoalElevated
import be.kdr.agvalarm.ui.theme.ConnectedGreen
import be.kdr.agvalarm.ui.theme.OfflineGray
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TimeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
private val Brussels: ZoneId = ZoneId.of("Europe/Brussels")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenSettings: () -> Unit,
) {
    val connection by viewModel.connection.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val network by viewModel.network.collectAsStateWithLifecycle()
    val notifications by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val highlightId by viewModel.highlightedEventId.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(highlightId, events) {
        val id = highlightId ?: return@LaunchedEffect
        val index = events.indexOfFirst { it.id == id }
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AGV Alarm") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Instellingen")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = Amber,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ConnectionBadge(connection)
            }
            if (connection.broker is BrokerTarget.Home &&
                connection.status != ConnectionStatus.CONNECTED
            ) {
                item {
                    Text(
                        text = AppSettings.HOME_UNREACHABLE_HINT,
                        style = MaterialTheme.typography.bodyLarge,
                        color = AlarmRed,
                    )
                }
            }
            item {
                NetworkRow(label = network.transportLabel)
            }
            item {
                NotificationsToggle(
                    enabled = notifications,
                    onToggle = viewModel::setNotificationsEnabled,
                )
            }
            item {
                Text(
                    text = "Live MQTT",
                    style = MaterialTheme.typography.titleLarge,
                    color = Amber,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (events.isEmpty()) {
                item {
                    EmptyTraffic(connection)
                }
            } else {
                items(events, key = { it.id }) { event ->
                    EventRow(event = event, highlighted = event.id == highlightId)
                }
            }
        }
    }
}

@Composable
private fun ConnectionBadge(state: ConnectionUiState) {
    val (statusText, accent) = when (state.status) {
        ConnectionStatus.CONNECTED -> "Verbonden" to ConnectedGreen
        ConnectionStatus.CONNECTING -> "Verbinden…" to Amber
        ConnectionStatus.OFFLINE -> "Offline" to OfflineGray
    }
    val brokerLine = state.broker?.let { broker ->
        val name = when (broker) {
            is BrokerTarget.Stubbe -> "Stubbe ${broker.host}"
            is BrokerTarget.Home -> "Thuis ${broker.host}"
        }
        "$name · ${broker.hostPort}"
    } ?: "geen broker geselecteerd"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CharcoalElevated),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.headlineMedium,
                    color = accent,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = brokerLine,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun NetworkRow(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Outlined.Wifi, contentDescription = null, tint = Amber)
        Text(
            text = "Netwerk: $label",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun NotificationsToggle(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CharcoalElevated),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Meldingen", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = if (enabled) "Aan — storingen komen als melding" else "Uit — alleen in de log",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = Amber.copy(alpha = 0.5f),
                    checkedThumbColor = Amber,
                ),
            )
        }
    }
}

@Composable
private fun EmptyTraffic(connection: ConnectionUiState) {
    val homeOffline = connection.broker is BrokerTarget.Home &&
        connection.status != ConnectionStatus.CONNECTED
    val text = when {
        homeOffline -> AppSettings.HOME_UNREACHABLE_HINT
        connection.status == ConnectionStatus.CONNECTED ->
            "Verbonden. Nog geen MQTT-berichten. Geabonneerd op inventory/# en quality/status. AGV-alarmen zitten nog niet op MQTT (die komen via SNMP/SQL)."
        else -> "Nog geen MQTT-berichten. Wachten op verbinding met de broker."
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = if (homeOffline) AlarmRed else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 12.dp),
    )
}

@Composable
private fun EventRow(event: MqttEvent, highlighted: Boolean) {
    val targetBorder by animateColorAsState(
        if (highlighted) Amber else Color.Transparent,
        label = "highlight",
    )
    val container = if (event.isAlarm) Color(0xFF3A1C12) else CharcoalElevated
    val time = TimeFmt.format(Instant.ofEpochMilli(event.timestampMillis).atZone(Brussels))
    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (highlighted || event.isAlarm) {
                    Modifier.border(
                        width = if (highlighted) 2.dp else 1.dp,
                        color = if (highlighted) targetBorder else AlarmRed.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(10.dp),
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelLarge,
                    color = Amber,
                    fontFamily = FontFamily.Monospace,
                )
                if (event.isAlarm) {
                    Text(
                        text = "STORING",
                        style = MaterialTheme.typography.labelLarge,
                        color = AlarmRed,
                        fontWeight = FontWeight.Bold,
                    )
                }
                event.agvId?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = event.topic,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Monospace,
            )
            val preview = event.payload.replace('\n', ' ').take(180)
            if (preview.isNotBlank()) {
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
