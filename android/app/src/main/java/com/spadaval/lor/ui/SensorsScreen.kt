package com.spadaval.lor.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
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

/** The Sensors tab: reads each sensor's access and the last sync, and asks for what is missing; [SensorsTab] draws it. */
@Composable
fun SensorsScreen() {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val scope = rememberCoroutineScope()
    var tick by remember { mutableIntStateOf(0) } // bump to re-read everything
    var syncing by remember { mutableStateOf(false) }
    var access by remember { mutableStateOf<Map<Sensor, SensorAccess>>(emptyMap()) }
    /** Switches flipped while the tab is open; the rest read from [Prefs]. */
    val on = remember { mutableStateMapOf<Sensor, Boolean>() }

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
            SensorAccess(false, "Health Connect is not available on this phone.", "Open Health Connect")
        } else {
            val granted = health.granted()
            when {
                !granted.containsAll(HealthCollector.DATA_PERMISSIONS) -> SensorAccess(false, "Needs permission to read sleep, exercise, and steps.", "Allow")
                HealthCollector.BACKGROUND_PERMISSION !in granted -> SensorAccess(true, "Allowed while the app is open. Allow background reading too, or it only syncs when you open the app.", "Allow in background")
                else -> SensorAccess(true, "Allowed, including in the background.", null)
            }
        }
        access = mapOf(
            Sensor.APPS to if (apps.permitted()) SensorAccess(true, "Allowed.", null) else SensorAccess(false, "Needs Usage access: find Life on Rails in the list and switch it on.", "Open Usage access"),
            Sensor.CALLS to if (calls.permitted()) SensorAccess(true, "Allowed.", null) else SensorAccess(false, "Needs permission to read the call log.", "Allow"),
            Sensor.TEXTS to if (texts.permitted()) SensorAccess(true, "Allowed.", null) else SensorAccess(false, "Needs permission to read texts.", "Allow"),
            Sensor.HEALTH to healthAccess,
            Sensor.WHATSAPP to when {
                !whatsapp.permitted() -> SensorAccess(false, "Needs All files access, to read WhatsApp's backup in Android/media.", "Allow")
                !whatsapp.file().isFile -> SensorAccess(false, "Allowed, but there is no backup yet. In WhatsApp: Settings > Chats > Chat backup > End-to-end encrypted backup, choose the 64-digit key, then Back up.", null)
                else -> SensorAccess(true, "Allowed. Last backup ${ago(whatsapp.file().lastModified(), System.currentTimeMillis())}.", null)
            },
            Sensor.LOCATION to when {
                !location.foregroundPermitted() -> SensorAccess(false, "Needs location permission.", "Allow")
                !location.backgroundPermitted() -> SensorAccess(true, "Allowed while the app is open. Choose \"Allow all the time\" so it works in the background.", "Allow all the time")
                else -> SensorAccess(true, "Allowed all the time.", null)
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
            Sensor.LAUNCHES -> {} // needs no permission: the launcher records its own taps
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

    val pending = remember(tick) { Queue(context).count() }
    val now = System.currentTimeMillis()
    SensorsTab(
        SensorsUi(
            SyncUi(prefs.lastSyncAt, prefs.lastSyncSummary, prefs.uploadedTotal, prefs.lastUploadAt, pending, prefs.lastSyncErrors, syncing),
            on = Sensor.entries.associateWith { on[it] ?: prefs.enabled(it) },
            access = access,
            now = now,
        ),
        SensorsActions(
            sync = {
                scope.launch {
                    syncing = true
                    Sync.run(context)
                    syncing = false
                    tick++
                }
            },
            turn = { sensor, it ->
                on[sensor] = it
                prefs.setEnabled(sensor, it)
            },
            request = ::request,
        ),
    )
}
