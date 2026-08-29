package be.kdr.agvalarm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import be.kdr.agvalarm.service.MqttForegroundService
import be.kdr.agvalarm.ui.AgvAlarmRoot
import be.kdr.agvalarm.ui.AppViewModelFactory

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        startMqttService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestRuntimePermissions()
        startMqttService()
        handleHighlight(intent)
        val factory = AppViewModelFactory((application as AgvAlarmApplication).container)
        setContent {
            AgvAlarmRoot(factory = factory)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleHighlight(intent)
    }

    private fun handleHighlight(intent: Intent?) {
        if (intent?.hasExtra(EXTRA_EVENT_ID) == true) {
            val id = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
            if (id >= 0) {
                (application as AgvAlarmApplication).container.mqttManager.highlightEvent(id)
            }
        }
    }

    private fun requestRuntimePermissions() {
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            needed += Manifest.permission.POST_NOTIFICATIONS
            needed += Manifest.permission.NEARBY_WIFI_DEVICES
        } else {
            needed += Manifest.permission.ACCESS_FINE_LOCATION
            needed += Manifest.permission.ACCESS_COARSE_LOCATION
        }
        val missing = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun startMqttService() {
        val intent = Intent(this, MqttForegroundService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    companion object {
        const val EXTRA_EVENT_ID = "event_id"
    }
}
