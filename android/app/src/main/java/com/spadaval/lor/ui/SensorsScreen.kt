package com.spadaval.lor.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.spadaval.lor.collect.AppUsageCollector
import com.spadaval.lor.collect.CallCollector
import com.spadaval.lor.collect.HealthCollector
import com.spadaval.lor.collect.LocationCollector
import com.spadaval.lor.collect.SmsCollector
import com.spadaval.lor.collect.WhatsAppBackup
import com.spadaval.lor.data.Prefs
import com.spadaval.lor.data.Queue
import com.spadaval.lor.data.Sensor
import com.spadaval.lor.sync.Sync
import kotlinx.coroutines.launch

/** What a sensor needs before it can collect, in words. */
private data class Access(val ready: Boolean, val status: String, val action: String?)

@Composable
fun SensorsScreen() {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val scope = rememberCoroutineScope()
    var tick by remember { mutableIntStateOf(0) } // bump to re-read everything
    var syncing by remember { mutableStateOf(false) }
    var access by remember { mutableStateOf<Map<Sensor, Access>>(emptyMap()) }

    val apps = remember { AppUsageCollector(context, prefs) }
    val calls = remember { CallCollector(context, prefs) }
    val texts = remember { SmsCollector(context, prefs) }
    val health = remember { HealthCollector(context, prefs) }
    val location = remember { LocationCollector(context, prefs) }
    val whatsapp = remember { WhatsAppBackup(context, prefs) }

    LifecycleResumeEffect(Unit) {
        tick++
        onPauseOrDispose {}
    }
    LaunchedEffect(tick) {
        val healthAccess = if (!health.available()) {
            Access(false, "Health Connect is not available on this phone.", "Open Health Connect")
        } else {
            val granted = health.granted()
            when {
                !granted.containsAll(HealthCollector.DATA_PERMISSIONS) -> Access(false, "Needs permission to read sleep, exercise, and steps.", "Allow")
                HealthCollector.BACKGROUND_PERMISSION !in granted -> Access(true, "Allowed while the app is open. Allow background reading too, or it only syncs when you open the app.", "Allow in background")
                else -> Access(true, "Allowed, including in the background.", null)
            }
        }
        access = mapOf(
            Sensor.APPS to if (apps.permitted()) Access(true, "Allowed.", null) else Access(false, "Needs Usage access: find Life on Rails in the list and switch it on.", "Open Usage access"),
            Sensor.CALLS to if (calls.permitted()) Access(true, "Allowed.", null) else Access(false, "Needs permission to read the call log.", "Allow"),
            Sensor.TEXTS to if (texts.permitted()) Access(true, "Allowed.", null) else Access(false, "Needs permission to read texts.", "Allow"),
            Sensor.HEALTH to healthAccess,
            Sensor.WHATSAPP to when {
                !whatsapp.permitted() -> Access(false, "Needs All files access, to read WhatsApp's backup in Android/media.", "Allow")
                !whatsapp.file().isFile -> Access(false, "Allowed, but there is no backup yet. In WhatsApp: Settings > Chats > Chat backup > End-to-end encrypted backup, choose the 64-digit key, then Back up.", null)
                else -> Access(true, "Allowed. Last backup ${ago(whatsapp.file().lastModified())}.", null)
            },
            Sensor.LOCATION to when {
                !location.foregroundPermitted() -> Access(false, "Needs location permission.", "Allow")
                !location.backgroundPermitted() -> Access(true, "Allowed while the app is open. Choose \"Allow all the time\" so it works in the background.", "Allow all the time")
                else -> Access(true, "Allowed all the time.", null)
            },
        )
    }

    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { tick++ }
    val healthPermissions = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { tick++ }

    fun request(sensor: Sensor) {
        when (sensor) {
            Sensor.APPS -> {
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:${context.packageName}"))
                try {
                    context.startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            }
            Sensor.CALLS -> permissions.launch(arrayOf(Manifest.permission.READ_CALL_LOG))
            Sensor.WHATSAPP -> context.startActivity(
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}")),
            )
            Sensor.TEXTS -> permissions.launch(arrayOf(Manifest.permission.READ_SMS))
            Sensor.HEALTH -> if (health.available()) {
                healthPermissions.launch(HealthCollector.ALL_PERMISSIONS)
            } else {
                runCatching { context.startActivity(Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS")) }
            }
            // Android asks for background location separately, after the foreground grant.
            Sensor.LOCATION -> if (!location.foregroundPermitted()) {
                permissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            } else {
                permissions.launch(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
            }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        key(tick) {
            val pending = remember(tick) { Queue(context).count() }
            Section("Sync") {
                Text("Last sync ${ago(prefs.lastSyncAt)}. ${prefs.lastSyncSummary}")
                Text(
                    "Uploaded ${prefs.uploadedTotal} so far; last upload ${ago(prefs.lastUploadAt)}." + if (pending > 0) " $pending waiting." else "",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (prefs.lastSyncErrors.isNotBlank()) {
                    Text(prefs.lastSyncErrors, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                }
                Button(
                    enabled = !syncing,
                    modifier = Modifier.padding(top = 8.dp),
                    onClick = {
                        scope.launch {
                            syncing = true
                            Sync.run(context)
                            syncing = false
                            tick++
                        }
                    },
                ) { Text(if (syncing) "Syncing…" else "Sync now") }
                Text("Runs by itself about every 15 minutes.", style = MaterialTheme.typography.bodySmall)
            }
        }

        for (sensor in Sensor.entries) {
            val a = access[sensor]
            var on by remember(sensor) { mutableStateOf(prefs.enabled(sensor)) }
            Section {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(sensor.label, style = MaterialTheme.typography.titleMedium)
                        Text(sensor.description, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = on, onCheckedChange = {
                        on = it
                        prefs.setEnabled(sensor, it)
                    })
                }
                if (on && a != null) {
                    Text(
                        a.status,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (a.ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    if (a.action != null) {
                        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End) {
                            OutlinedButton(onClick = { request(sensor) }) { Text(a.action) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun key(value: Int, content: @Composable () -> Unit) = androidx.compose.runtime.key(value) { content() }
