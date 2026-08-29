package be.kdr.agvalarm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import be.kdr.agvalarm.AppContainer
import be.kdr.agvalarm.data.AppSettings
import be.kdr.agvalarm.data.SettingsRepository
import be.kdr.agvalarm.model.BrokerReachability
import be.kdr.agvalarm.mqtt.MqttManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val mqttManager: MqttManager,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val connection = mqttManager.connection
    val events = mqttManager.events
    val network = mqttManager.network
    val highlightedEventId = mqttManager.highlightedEventId
    val notificationsEnabled: StateFlow<Boolean> = settingsRepository.settings
        .map { it.notificationsEnabled }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setNotificationsEnabled(enabled) }
    }

    fun clearHighlight() {
        mqttManager.clearHighlight()
    }
}

class SettingsViewModel(
    private val mqttManager: MqttManager,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _probeResult = MutableStateFlow<BrokerReachability?>(null)
    val probeResult: StateFlow<BrokerReachability?> = _probeResult.asStateFlow()

    private val _probing = MutableStateFlow(false)
    val probing: StateFlow<Boolean> = _probing.asStateFlow()

    fun save(settings: AppSettings) {
        viewModelScope.launch { settingsRepository.update { settings } }
    }

    fun testConnection() {
        viewModelScope.launch {
            _probing.value = true
            _probeResult.value = runCatching { mqttManager.probeBothBrokers() }.getOrNull()
            _probing.value = false
        }
    }

    fun fireTestAgvAlarm() {
        mqttManager.fireTestAgvAlarm()
    }
}

class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(container.mqttManager, container.settingsRepository) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(container.mqttManager, container.settingsRepository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel ${modelClass.name}")
        }
    }
}
