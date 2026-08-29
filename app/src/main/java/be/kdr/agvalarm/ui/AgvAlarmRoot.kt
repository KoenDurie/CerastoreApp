package be.kdr.agvalarm.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import be.kdr.agvalarm.mqtt.MqttManager
import be.kdr.agvalarm.ui.home.HomeScreen
import be.kdr.agvalarm.ui.settings.SettingsScreen
import be.kdr.agvalarm.ui.theme.AgvAlarmTheme

@Composable
fun AgvAlarmRoot(
    factory: AppViewModelFactory,
    mqttManager: MqttManager,
    navController: NavHostController = rememberNavController(),
) {
    val popup by mqttManager.activePopup.collectAsStateWithLifecycle()
    AgvAlarmTheme {
        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                val vm: HomeViewModel = viewModel(factory = factory)
                HomeScreen(
                    viewModel = vm,
                    onOpenSettings = { navController.navigate("settings") },
                )
            }
            composable("settings") {
                val vm: SettingsViewModel = viewModel(factory = factory)
                SettingsScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                )
            }
        }
        popup?.let { current ->
            AgvAlarmDialog(
                popup = current,
                onConfirm = mqttManager::dismissPopup,
            )
        }
    }
}
