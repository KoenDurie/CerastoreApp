package be.kdr.agvalarm.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.fillMaxSize
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import be.kdr.agvalarm.mqtt.MqttManager
import be.kdr.agvalarm.ui.alarms.AlarmsScreen
import be.kdr.agvalarm.ui.dashboard.DashTab
import be.kdr.agvalarm.ui.dashboard.DashboardScaffold
import be.kdr.agvalarm.ui.home.HomeScreen
import be.kdr.agvalarm.ui.settings.SettingsScreen
import be.kdr.agvalarm.ui.theme.AgvAlarmTheme

@Composable
fun AgvAlarmRoot(
    factory: AppViewModelFactory,
    mqttManager: MqttManager,
) {
    val popup by mqttManager.activePopup.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val route = navController.currentBackStackEntryAsState().value?.destination?.route
    val tab = when (route) {
        "alarms" -> DashTab.Alarms
        "settings" -> DashTab.Settings
        else -> DashTab.Home
    }

    AgvAlarmTheme {
        DashboardScaffold(
            current = tab,
            onSelect = { next ->
                val dest = when (next) {
                    DashTab.Home -> "home"
                    DashTab.Alarms -> "alarms"
                    DashTab.Settings -> "settings"
                }
                navController.navigate(dest) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
        ) {
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
            ) {
                composable("home") {
                    val vm: HomeViewModel = viewModel(factory = factory)
                    HomeScreen(
                        viewModel = vm,
                        onOpenAlarms = {
                            navController.navigate("alarms") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
                composable("alarms") {
                    val vm: HomeViewModel = viewModel(factory = factory)
                    AlarmsScreen(viewModel = vm)
                }
                composable("settings") {
                    val vm: SettingsViewModel = viewModel(factory = factory)
                    SettingsScreen(viewModel = vm)
                }
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
