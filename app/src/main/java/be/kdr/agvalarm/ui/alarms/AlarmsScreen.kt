package be.kdr.agvalarm.ui.alarms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.kdr.agvalarm.data.TopicSubscriptions
import be.kdr.agvalarm.ui.HomeViewModel
import be.kdr.agvalarm.ui.events.EventRow
import be.kdr.agvalarm.ui.theme.Ink
import be.kdr.agvalarm.ui.theme.LabelGrey

@Composable
fun AlarmsScreen(viewModel: HomeViewModel) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    val highlightId by viewModel.highlightedEventId.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val visible = events.filter { !TopicSubscriptions.isOrderTopic(it.topic) }

    LaunchedEffect(highlightId, visible) {
        val id = highlightId ?: return@LaunchedEffect
        val index = visible.indexOfFirst { it.id == id }
        if (index >= 0) listState.animateScrollToItem(index)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Storingen", style = MaterialTheme.typography.headlineMedium, color = Ink)
            Text(
                "MQTT-log, nieuwste eerst. AGV-alarmen en kwaliteitsrobot — geen idle HOME/orders.",
                style = MaterialTheme.typography.labelSmall,
                color = LabelGrey,
            )
        }
        if (visible.isEmpty()) {
            item {
                Text(
                    "Nog geen berichten.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LabelGrey,
                )
            }
        } else {
            items(visible, key = { it.id }) { event ->
                EventRow(event = event, highlighted = event.id == highlightId, compact = false)
            }
        }
    }
}
