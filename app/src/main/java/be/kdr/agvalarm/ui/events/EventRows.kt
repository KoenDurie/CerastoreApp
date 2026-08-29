package be.kdr.agvalarm.ui.events

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import be.kdr.agvalarm.model.MqttEvent
import be.kdr.agvalarm.ui.dashboard.CoralPill
import be.kdr.agvalarm.ui.dashboard.WarningTriangle
import be.kdr.agvalarm.ui.theme.AccentOrange
import be.kdr.agvalarm.ui.theme.CardWhite
import be.kdr.agvalarm.ui.theme.Ink
import be.kdr.agvalarm.ui.theme.LabelGrey
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TimeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
private val Brussels: ZoneId = ZoneId.of("Europe/Brussels")

@Composable
fun EventRow(event: MqttEvent, highlighted: Boolean, compact: Boolean = false) {
    val highlightColor by animateColorAsState(
        if (highlighted) AccentOrange else Color.Transparent,
        label = "hl",
    )
    val container = when {
        event.isAlarm -> Color(0xFFFFF1EE)
        event.isResolved -> Color(0xFFF3F3EE)
        else -> CardWhite
    }
    val time = TimeFmt.format(Instant.ofEpochMilli(event.timestampMillis).atZone(Brussels))
    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (event.isAlarm) 3.dp else 1.dp),
        modifier = Modifier.fillMaxWidth(),
        border = if (highlighted) {
            androidx.compose.foundation.BorderStroke(2.dp, highlightColor)
        } else {
            null
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = if (compact) 10.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (event.isAlarm) {
                WarningTriangle()
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = time,
                        style = MaterialTheme.typography.labelLarge,
                        color = AccentOrange,
                        fontFamily = FontFamily.Monospace,
                    )
                    if (event.isAlarm) {
                        CoralPill("STORING")
                    } else if (event.isResolved) {
                        Text(
                            "OPGELOST",
                            style = MaterialTheme.typography.labelLarge,
                            color = LabelGrey,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    event.agvId?.let {
                        Text(it, style = MaterialTheme.typography.labelLarge, color = Ink)
                    }
                }
                if (!compact) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = event.topic,
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = FontFamily.Monospace,
                        color = Ink,
                    )
                    val preview = (event.displayMessage ?: event.payload).replace('\n', ' ').take(160)
                    if (preview.isNotBlank()) {
                        Text(
                            text = preview,
                            style = MaterialTheme.typography.bodyMedium,
                            color = LabelGrey,
                        )
                    }
                } else {
                    Text(
                        text = event.topic,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = LabelGrey,
                    )
                }
            }
        }
    }
}
