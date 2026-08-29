package be.kdr.agvalarm.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.DialogProperties
import be.kdr.agvalarm.model.AgvAlarmPopup
import be.kdr.agvalarm.ui.theme.Amber
import be.kdr.agvalarm.ui.theme.CharcoalElevated

@Composable
fun AgvAlarmDialog(
    popup: AgvAlarmPopup,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { /* blocking tot OK */ },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Amber,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("OK")
            }
        },
        title = {
            Text(
                text = popup.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Amber,
            )
        },
        text = {
            Text(
                text = popup.message,
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        containerColor = CharcoalElevated,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    )
}
