package be.kdr.agvalarm.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.kdr.agvalarm.data.AppSettings
import be.kdr.agvalarm.model.BrokerReachability
import be.kdr.agvalarm.ui.SettingsViewModel
import be.kdr.agvalarm.ui.theme.Amber
import be.kdr.agvalarm.ui.theme.CharcoalElevated
import be.kdr.agvalarm.ui.theme.ConnectedGreen
import be.kdr.agvalarm.ui.theme.OfflineGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Instellingen") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.save(currentSettings())
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Terug")
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("MQTT-brokers", style = MaterialTheme.typography.titleLarge, color = Amber)
            }
            item {
                Field("Stubbe-host", stubbeHost, { stubbeHost = it })
            }
            item {
                Field("Stubbe-poort", stubbePort, { stubbePort = it }, KeyboardType.Number)
            }
            item {
                Field("Thuis-host (LAN-IP van PC-KDR)", homeHost, { homeHost = it })
            }
            item {
                Field("Thuis-poort", homePort, { homePort = it }, KeyboardType.Number)
            }
            item {
                Field("Thuis Wi-Fi SSID", homeSsid, { homeSsid = it })
            }
            item {
                Text(
                    "Op $homeSsid wordt Stubbe (10.0.0.20) niet verwacht, tenzij er een VPN actief is.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Field("Gebruikersnaam (optioneel)", username, { username = it })
            }
            item {
                Field(
                    label = "Wachtwoord (optioneel)",
                    value = password,
                    onValueChange = { password = it },
                    password = true,
                )
            }
            item {
                Field("Extra topicfilter (optioneel, bv. #)", extraTopicFilter, { extraTopicFilter = it })
            }
            item {
                Text(
                    "Standaard: inventory/# en quality/status (bestaande retained topics). Extra leeg laten, of # voor discovery. De app publiceert niets en negeert quality/robot/cmd en quality/robot/ack. AGV-IDs zitten niet op MQTT.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Spacer(Modifier.height(8.dp))
                Text("Stubbe Wi-Fi", style = MaterialTheme.typography.titleLarge, color = Amber)
            }
            item {
                Field("SSID (optioneel)", stubbeSsid, { stubbeSsid = it })
            }
            item {
                Field(
                    label = "Wi-Fi-wachtwoord (alleen voor netwerksuggestie)",
                    value = stubbeWifiPassword,
                    onValueChange = { stubbeWifiPassword = it },
                    password = true,
                )
            }
            item {
                Text(
                    "Als de huidige SSID overeenkomt, verbindt de app met Stubbe. Android kan wifi niet stilzwijgend joinen; met SSID + wachtwoord mag de app dat netwerk wel voorstellen wanneer je in de buurt bent. Stubbe-SSID is op dit toestel nog niet bekend.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Button(
                    onClick = {
                        viewModel.save(currentSettings())
                        viewModel.testConnection()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = MaterialTheme.colorScheme.onPrimary),
                    enabled = !probing,
                ) {
                    if (probing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .height(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    }
                    Text("Testverbinding")
                }
            }
            probe?.let { result ->
                item { ProbeCard(result) }
            }
            item {
                Button(
                    onClick = { viewModel.save(currentSettings()) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CharcoalElevated, contentColor = Amber),
                ) {
                    Text("Opslaan")
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                Text("Rechten", style = MaterialTheme.typography.titleLarge, color = Amber)
            }
            item { PermissionsCard() }
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
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Amber,
            focusedLabelColor = Amber,
            cursorColor = Amber,
        ),
    )
}

@Composable
private fun ProbeCard(result: BrokerReachability) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CharcoalElevated),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Testresultaat", style = MaterialTheme.typography.titleLarge)
            ReachLine("Stubbe", result.stubbeTarget, result.stubbeReachable)
            ReachLine("Thuis", result.homeTarget, result.homeReachable)
            if (!result.homeReachable) {
                Text(
                    AppSettings.HOME_UNREACHABLE_HINT,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ReachLine(name: String, target: String, ok: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = if (ok) "bereikbaar" else "niet bereikbaar",
            color = if (ok) ConnectedGreen else OfflineGray,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text("$name $target", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PermissionsCard() {
    val items = listOf(
        "INTERNET" to "Verbinding maken met de MQTT-brokers.",
        "ACCESS_NETWORK_STATE" to "Netwerkwijzigingen detecteren (wifi, mobiel, ethernet) zodat de app meteen naar Stubbe kan schakelen.",
        "ACCESS_WIFI_STATE" to "Wi-Fi-status lezen.",
        "NEARBY_WIFI_DEVICES" to "Huidige SSID tonen (Android 13+). Alleen gebruikt om Stubbe-wifi te herkennen, niet voor locatie.",
        "ACCESS_FINE_LOCATION" to "Op oudere Android-versies nodig om de SSID te lezen. Wordt niet gebruikt om je te volgen.",
        "POST_NOTIFICATIONS" to "Alarmmeldingen en de statusmelding van de achtergronddienst.",
        "FOREGROUND_SERVICE / DATA_SYNC" to "MQTT verbonden houden met het scherm uit.",
        "RECEIVE_BOOT_COMPLETED" to "De MQTT-dienst herstarten na het opstarten van de telefoon.",
        "CHANGE_WIFI_STATE" to "Optionele netwerksuggestie voor Stubbe-wifi (alleen als SSID + wachtwoord ingevuld zijn).",
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = CharcoalElevated),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.forEach { (perm, why) ->
                Column {
                    Text(perm, style = MaterialTheme.typography.labelLarge, color = Amber)
                    Text(why, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
