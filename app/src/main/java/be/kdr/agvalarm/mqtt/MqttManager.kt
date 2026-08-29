package be.kdr.agvalarm.mqtt

import android.content.Context
import android.provider.Settings
import android.util.Log
import be.kdr.agvalarm.data.AppSettings
import be.kdr.agvalarm.data.SettingsRepository
import be.kdr.agvalarm.data.TopicSubscriptions
import be.kdr.agvalarm.model.BrokerReachability
import be.kdr.agvalarm.model.BrokerTarget
import be.kdr.agvalarm.model.ConnectionStatus
import be.kdr.agvalarm.model.ConnectionUiState
import be.kdr.agvalarm.model.MqttEvent
import be.kdr.agvalarm.model.NetworkUiState
import be.kdr.agvalarm.network.NetworkMonitor
import be.kdr.agvalarm.network.WifiSuggestionHelper
import be.kdr.agvalarm.notifications.NotificationHelper
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.MqttClientState
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.lifecycle.MqttDisconnectSource
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class MqttManager(
    context: Context,
    private val settingsRepository: SettingsRepository,
    private val notificationHelper: NotificationHelper,
    private val networkMonitor: NetworkMonitor = NetworkMonitor(context),
    private val wifiSuggestionHelper: WifiSuggestionHelper = WifiSuggestionHelper(context),
    private val brokerSelector: BrokerSelector = BrokerSelector(),
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val started = AtomicBoolean(false)
    private val clientMutex = Mutex()
    private val clientRef = AtomicReference<Mqtt3AsyncClient?>(null)
    private val generation = AtomicLong(0)
    private val eventSeq = AtomicLong(0)
    private val deduplicator = AlarmDeduplicator()
    private val rescan = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val clientId = buildClientId()
    @Volatile private var lastSubscribeKey: String? = null
    @Volatile private var lastAuthUser: String? = null

    private val _connection = MutableStateFlow(ConnectionUiState())
    val connection: StateFlow<ConnectionUiState> = _connection.asStateFlow()

    private val _events = MutableStateFlow<List<MqttEvent>>(emptyList())
    val events: StateFlow<List<MqttEvent>> = _events.asStateFlow()

    private val _network = MutableStateFlow(NetworkUiState())
    val network: StateFlow<NetworkUiState> = _network.asStateFlow()

    private val _highlightedEventId = MutableStateFlow<Long?>(null)
    val highlightedEventId: StateFlow<Long?> = _highlightedEventId.asStateFlow()

    fun start() {
        if (!started.compareAndSet(false, true)) return
        Log.i(TAG, "Starting MQTT manager clientId=$clientId")
        scope.launch {
            settingsRepository.settings.collectLatest { settings ->
                wifiSuggestionHelper.syncSuggestion(settings.stubbeSsid, settings.stubbeWifiPassword)
                rescan.tryEmit(Unit)
            }
        }
        scope.launch {
            networkMonitor.changes.collect {
                _network.value = networkMonitor.currentNetwork()
                rescan.tryEmit(Unit)
            }
        }
        scope.launch {
            while (isActive) {
                val state = _connection.value
                val onHome = state.broker is BrokerTarget.Home
                val disconnected = state.status != ConnectionStatus.CONNECTED
                if (onHome || disconnected) {
                    rescan.tryEmit(Unit)
                }
                delay(PROBE_INTERVAL_MS)
            }
        }
        scope.launch {
            rescan.collect {
                runCatching { applySelection() }
                    .onFailure { Log.w(TAG, "Broker selection failed", it) }
            }
        }
        _network.value = networkMonitor.currentNetwork()
        rescan.tryEmit(Unit)
    }

    fun stop() {
        started.set(false)
        scope.cancel()
        scope.launch { /* cancelled */ }
        runCatching { disconnectClient() }
    }

    fun highlightEvent(eventId: Long?) {
        _highlightedEventId.value = eventId
    }

    fun clearHighlight() {
        _highlightedEventId.value = null
    }

    suspend fun probeBothBrokers(): BrokerReachability {
        val settings = settingsRepository.current()
        return coroutineScope {
            val stubbe = async {
                BrokerProbe.isReachable(settings.stubbeHost, settings.stubbePort)
            }
            val home = async {
                BrokerProbe.isReachable(settings.homeHost, settings.homePort)
            }
            BrokerReachability(
                stubbeReachable = stubbe.await(),
                homeReachable = home.await(),
                stubbeTarget = "${settings.stubbeHost}:${settings.stubbePort}",
                homeTarget = "${settings.homeHost}:${settings.homePort}",
            )
        }
    }

    private suspend fun applySelection() {
        val settings = settingsRepository.current()
        _network.value = networkMonitor.currentNetwork()
        val target = brokerSelector.select(
            settings,
            _network.value.ssid,
            vpnActive = _network.value.vpnActive,
        )
        ensureConnected(target, settings)
    }

    private suspend fun ensureConnected(target: BrokerTarget, settings: AppSettings) {
        clientMutex.withLock {
            val existing = clientRef.get()
            if (existing != null &&
                _connection.value.broker == target &&
                lastSubscribeKey == settings.subscriptionKey() &&
                lastAuthUser == settings.username
            ) {
                when (existing.state) {
                    MqttClientState.CONNECTED -> {
                        if (_connection.value.status != ConnectionStatus.CONNECTED) {
                            _connection.update {
                                it.copy(
                                    status = ConnectionStatus.CONNECTED,
                                    broker = target,
                                    lastError = null,
                                )
                            }
                        }
                        return
                    }
                    MqttClientState.CONNECTING -> return
                    else -> Unit
                }
            }

            val gen = generation.incrementAndGet()
            disconnectLocked()
            lastSubscribeKey = settings.subscriptionKey()
            lastAuthUser = settings.username
            _connection.value = ConnectionUiState(
                status = ConnectionStatus.CONNECTING,
                broker = target,
            )
            val client = buildClient(target, gen)
            clientRef.set(client)
            try {
                val connect = client.connectWith()
                    .keepAlive(AppSettings.KEEP_ALIVE_SECONDS)
                    .cleanSession(true)
                val user = settings.username.trim()
                if (user.isNotEmpty() || settings.password.isNotEmpty()) {
                    connect.simpleAuth()
                        .username(user)
                        .password(settings.password.toByteArray(StandardCharsets.UTF_8))
                        .applySimpleAuth()
                }
                connect.send().get(AppSettings.CONNECT_TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
                if (generation.get() != gen) return
                lastSubscribeKey = settings.subscriptionKey()
                lastAuthUser = settings.username
                _connection.value = ConnectionUiState(
                    status = ConnectionStatus.CONNECTED,
                    broker = target,
                )
                Log.i(TAG, "Connected to ${target.displayName} ${target.hostPort}")
            } catch (e: Exception) {
                if (generation.get() != gen) return
                Log.w(TAG, "Connect failed to ${target.hostPort}: ${e.message}")
                _connection.value = ConnectionUiState(
                    status = ConnectionStatus.OFFLINE,
                    broker = target,
                    lastError = e.message,
                )
            }
        }
    }

    private fun buildClient(target: BrokerTarget, gen: Long): Mqtt3AsyncClient {
        return MqttClient.builder()
            .useMqttVersion3()
            .identifier(clientId)
            .serverHost(target.host)
            .serverPort(target.port)
            .transportConfig()
            .mqttConnectTimeout(AppSettings.CONNECT_TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
            .socketConnectTimeout(AppSettings.CONNECT_TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
            .applyTransportConfig()
            .automaticReconnect()
            .initialDelay(1, TimeUnit.SECONDS)
            .maxDelay(30, TimeUnit.SECONDS)
            .applyAutomaticReconnect()
            .addConnectedListener {
                if (generation.get() != gen) return@addConnectedListener
                _connection.update {
                    it.copy(status = ConnectionStatus.CONNECTED, broker = target, lastError = null)
                }
                scope.launch {
                    val currentSettings = settingsRepository.current()
                    val current = clientRef.get() ?: return@launch
                    runCatching { subscribe(current, currentSettings) }
                }
            }
            .addDisconnectedListener { context ->
                if (generation.get() != gen) {
                    context.reconnector.reconnect(false)
                    return@addDisconnectedListener
                }
                if (context.source == MqttDisconnectSource.USER) return@addDisconnectedListener
                _connection.update {
                    it.copy(
                        status = ConnectionStatus.CONNECTING,
                        broker = target,
                        lastError = context.cause?.message,
                    )
                }
            }
            .buildAsync()
    }

    private fun subscribe(client: Mqtt3AsyncClient, settings: AppSettings) {
        val filters = TopicSubscriptions.filters(settings.extraTopicFilter)
        lastSubscribeKey = settings.subscriptionKey()
        filters.forEach { filter ->
            client.subscribeWith()
                .topicFilter(filter)
                .qos(MqttQos.AT_LEAST_ONCE)
                .callback { publish -> onPublish(publish) }
                .send()
            Log.i(TAG, "Subscribed to $filter")
        }
    }

    private fun onPublish(publish: Mqtt3Publish) {
        val topic = publish.topic.toString()
        if (TopicSubscriptions.isCommandTopic(topic)) return
        val payload = publish.payloadAsBytes.toString(StandardCharsets.UTF_8)
        val isAlarm = AlarmClassifier.isAlarm(topic, payload)
        val event = MqttEvent(
            id = eventSeq.incrementAndGet(),
            timestampMillis = System.currentTimeMillis(),
            topic = topic,
            payload = payload,
            isAlarm = isAlarm,
            agvId = AlarmClassifier.extractAgvId(topic, payload),
        )
        _events.update { current ->
            (listOf(event) + current).take(MAX_EVENTS)
        }
        if (isAlarm) {
            scope.launch {
                val enabled = settingsRepository.current().notificationsEnabled
                if (enabled && deduplicator.shouldNotify(topic, payload)) {
                    notificationHelper.notifyAlarm(event)
                }
            }
        }
    }

    private fun disconnectClient() {
        val client = clientRef.getAndSet(null) ?: return
        runCatching { client.disconnect().get(2, TimeUnit.SECONDS) }
    }

    private fun disconnectLocked() {
        val client = clientRef.getAndSet(null) ?: return
        runCatching { client.disconnect().get(2, TimeUnit.SECONDS) }
    }

    private fun buildClientId(): String {
        val androidId = Settings.Secure.getString(
            appContext.contentResolver,
            Settings.Secure.ANDROID_ID,
        ).orEmpty()
        val suffix = androidId.takeLast(6).ifBlank { "dev" }
        return "KDR_Android_$suffix"
    }

    companion object {
        private const val TAG = "MqttManager"
        private const val MAX_EVENTS = 400
        private const val PROBE_INTERVAL_MS = 5_000L
    }
}
