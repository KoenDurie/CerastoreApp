package be.kdr.agvalarm.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.kdr.agvalarm.data.AppSettings
import be.kdr.agvalarm.model.BrokerReachability
import be.kdr.agvalarm.ui.SettingsViewModel
import be.kdr.agvalarm.ui.dashboard.GlassCard
import be.kdr.agvalarm.ui.theme.AccentOrange
import be.kdr.agvalarm.ui.theme.Ink
import be.kdr.agvalarm.ui.theme.LabelGrey
import be.kdr.agvalarm.ui.theme.Lime
import be.kdr.agvalarm.ui.theme.OfflineGray

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val persisted by viewModel.settings.collectAsStateWithLifecycle()
    val probing by viewModel.probing.collectAsStateWithLifecycle()
    val probe by viewModel.probeResult.collectAsStateWithLifecycle()

    var stubbeHost by rememberSaveable { mutableStateOf(persisted.stubbeHost) }
    var stubbePort by rememberSaveable { mutableStateOf(persisted.stubbePort.toString()) }
    var homeHost by rememberSaveable { mutableStateOf(persisted.homeHost) }
    var homePort by rememberSaveable { mutableStateOf(persisted.homePort.toString()) }
    var username by rememberSaveable { mutableStateOf(persisted.username) }
    var password by rememberSaveable { mutableStateOf(persisted.password) }
    var extraTopicFilter by rememberSaveable { mutableStateOf(persisted.extraTopicFilter) }
    var stubbeSsid by rememberSaveable { mutableStateOf(persisted.stubbeSsid) }
    var stubbeWifiPassword by rememberSaveable { mutableStateOf(persisted.stubbeWifiPassword) }
    var homeSsid by rememberSaveable { mutableStateOf(persisted.homeSsid) }

    LaunchedEffect(persisted) {
        stubbeHost = persisted.stubbeHost
        stubbePort = persisted.stubbePort.toString()
        homeHost = persisted.homeHost
        homePort = persisted.homePort.toString()
        username = persisted.username
        password = persisted.password
        extraTopicFilter = persisted.extraTopicFilter
        stubbeSsid = persisted.stubbeSsid
        stubbeWifiPassword = persisted.stubbeWifiPassword
        homeSsid = persisted.homeSsid
    }

    fun currentSettings(): AppSettings = AppSettings(
        stubbeHost = stubbeHost,
        stubbePort = stubbePort.toIntOrNull() ?: AppSettings.DEFAULT_MQTT_PORT,
        homeHost = homeHost,
        homePort = homePort.toIntOrNull() ?: AppSettings.DEFAULT_MQTT_PORT,
        username = username,
        password = password,
        extraTopicFilter = extraTopicFilter.trim(),
        stubbeSsid = stubbeSsid,
        stubbeWifiPassword = stubbeWifiPassword,
        homeSsid = homeSsid,
        notificationsEnabled = persisted.notificationsEnabled,
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Instellingen", style = MaterialTheme.typography.headlineMedium, color = Ink)
        }
        item {
            GlassCard {
                Text("MQTT-brokers", style = MaterialTheme.typography.titleLarge)
                Field("Stubbe-host", stubbeHost, { stubbeHost = it })
                Field("Stubbe-poort", stubbePort, { stubbePort = it }, KeyboardType.Number)
                Field("Thuis-host (LAN-IP van PC-KDR)", homeHost, { homeHost = it })
                Field("Thuis-poort", homePort, { homePort = it }, KeyboardType.Number)
                Field("Thuis Wi-Fi SSID", homeSsid, { homeSsid = it })
                Text(
                    "Op $homeSsid wordt Stubbe niet verwacht, tenzij er een VPN actief is.",
                    style = MaterialTheme.typography.labelSmall,
                )
                Field("Gebruikersnaam (optioneel)", username, { username = it })
                Field("Wachtwoord (optioneel)", password, { password = it }, password = true)
                Field("Extra topicfilter (optioneel, bv. #)", extraTopicFilter, { extraTopicFilter = it })
                Text(
                    "Standaard: inventory/#, quality/status en stubbe/agv/#. Geen publish, geen SQL.",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        item {
            GlassCard {
                Text("Stubbe Wi-Fi", style = MaterialTheme.typography.titleLarge)
                Field("SSID (optioneel)", stubbeSsid, { stubbeSsid = it })
                Field("Wi-Fi-wachtwoord (netwerksuggestie)", stubbeWifiPassword, { stubbeWifiPassword = it }, password = true)
            }
        }
        item {
            Button(
                onClick = {
                    viewModel.save(currentSettings())
                    viewModel.testConnection()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange, contentColor = androidx.compose.ui.graphics.Color.White),
                enabled = !probing,
            ) {
                if (probing) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .height(18.dp),
                        color = androidx.compose.ui.graphics.Color.White,
                        strokeWidth = 2.dp,
                    )
                }
                Text("Testverbinding")
            }
        }
        item {
            Button(
                onClick = {
                    viewModel.save(currentSettings())
                    viewModel.fireTestAgvAlarm()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentOrange,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                ),
            ) {
                Text("Test AGV-melding")
            }
        }
        item {
            Text(
                "Zelfde heads-up en popup als een echte storing (AGV 99, Testmelding).",
                style = MaterialTheme.typography.labelSmall,
                color = LabelGrey,
            )
        }
        probe?.let { item { ProbeCard(it) } }
        item {
            Button(
                onClick = { viewModel.save(currentSettings()) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color.White,
                    contentColor = AccentOrange,
                ),
            ) {
                Text("Opslaan")
            }
        }
        item {
            GlassCard(sage = true) {
                Text("Rechten", style = MaterialTheme.typography.titleLarge, color = androidx.compose.ui.graphics.Color.White)
                PermissionsList()
            }
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentOrange,
            focusedLabelColor = AccentOrange,
            cursorColor = AccentOrange,
            unfocusedBorderColor = LabelGrey.copy(alpha = 0.35f),
        ),
    )
}

@Composable
private fun ProbeCard(result: BrokerReachability) {
    GlassCard {
        Text("Testresultaat", style = MaterialTheme.typography.titleLarge)
        ReachLine("Stubbe", result.stubbeTarget, result.stubbeReachable)
        ReachLine("Thuis", result.homeTarget, result.homeReachable)
        if (!result.homeReachable) {
            Text(AppSettings.HOME_UNREACHABLE_HINT, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun ReachLine(name: String, target: String, ok: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
        Text(
            if (ok) "bereikbaar" else "niet bereikbaar",
            color = if (ok) Lime else OfflineGray,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text("$name $target", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PermissionsList() {
    val items = listOf(
        "INTERNET" to "Verbinding met MQTT-brokers.",
        "ACCESS_NETWORK_STATE" to "Netwerkwijzigingen voor auto-switch naar Stubbe.",
        "ACCESS_WIFI_STATE" to "Wi-Fi-status lezen.",
        "NEARBY_WIFI_DEVICES" to "SSID tonen (Android 13+).",
        "ACCESS_FINE_LOCATION" to "SSID op oudere Android.",
        "POST_NOTIFICATIONS" to "AGV-popup en heads-up.",
        "FOREGROUND_SERVICE" to "MQTT met scherm uit.",
        "RECEIVE_BOOT_COMPLETED" to "Herstart na boot.",
        "CHANGE_WIFI_STATE" to "Optionele Stubbe-netwerksuggestie.",
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (perm, why) ->
            Column {
                Text(perm, style = MaterialTheme.typography.labelLarge, color = Lime)
                Text(why, style = MaterialTheme.typography.bodyMedium, color = androidx.compose.ui.graphics.Color.White.copy(0.9f))
            }
        }
    }
}
