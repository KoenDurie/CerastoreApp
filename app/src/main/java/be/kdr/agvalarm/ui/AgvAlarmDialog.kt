package be.kdr.agvalarm.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import be.kdr.agvalarm.model.AgvAlarmPopup
import be.kdr.agvalarm.ui.dashboard.CoralPill
import be.kdr.agvalarm.ui.dashboard.WarningTriangle
import be.kdr.agvalarm.ui.theme.AccentOrange
import be.kdr.agvalarm.ui.theme.CardWhite
import be.kdr.agvalarm.ui.theme.Ink

@Composable
fun AgvAlarmDialog(
    popup: AgvAlarmPopup,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentOrange,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                ),
            ) {
                Text("OK", fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.Start) {
                CoralPill("STORING")
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "AGV",
                    style = MaterialTheme.typography.labelSmall,
                    color = Ink.copy(alpha = 0.6f),
                )
                Text(
                    text = popup.vehicleId,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 48.sp,
                    color = Ink,
                )
                Text(
                    text = popup.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink,
                )
            }
        },
        text = {
            Text(popup.message, style = MaterialTheme.typography.bodyLarge, color = Ink)
        },
        icon = { WarningTriangle() },
        containerColor = CardWhite,
        shape = RoundedCornerShape(24.dp),
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    )
}
