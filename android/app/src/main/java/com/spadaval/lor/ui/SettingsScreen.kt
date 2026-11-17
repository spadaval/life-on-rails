package com.spadaval.lor.ui

import android.Manifest
import android.app.NotificationManager
import android.app.StatusBarManager
import android.content.pm.PackageManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.spadaval.lor.BuildInfo
import com.spadaval.lor.R
import com.spadaval.lor.tile.AskTileService
import com.spadaval.lor.data.Prefs
import com.spadaval.lor.net.LorClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The Settings tab: the server, quick send and Live Updates, and the way into [T3DebugScreen]; [SettingsTab] draws it. */
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val scope = rememberCoroutineScope()
    var server by remember { mutableStateOf(prefs.serverUrl) }
    var device by remember { mutableStateOf(prefs.deviceName) }
    var result by remember { mutableStateOf<String?>(null) }
    var showT3 by remember { mutableStateOf(false) }

    if (showT3) {
        T3DebugScreen(onBack = { showT3 = false })
        return
    }

    val live = rememberLiveUpdates()
    SettingsTab(
        SettingsUi(server, device, result, canAddTile = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU, live.ui, BuildInfo.VERSION),
        SettingsActions(
            server = { server = it },
            device = { device = it },
            save = {
                prefs.serverUrl = server
                prefs.deviceName = device
                server = prefs.serverUrl
                device = prefs.deviceName
                result = "Saved."
            },
            test = {
                scope.launch {
                    result = "Checking…"
                    result = withContext(Dispatchers.IO) {
                        runCatching { LorClient(server.trim()).state(device) }
                            .fold({ "Connected. LoR sees you as ${it.optString("login")}." }, { "Cannot connect: ${it.message}" })
                    }
                }
            },
            openT3 = { showT3 = true },
            addTile = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) context.getSystemService(StatusBarManager::class.java).requestAddTileService(
                    ComponentName(context, AskTileService::class.java),
                    context.getString(R.string.ask_tile_label),
                    Icon.createWithResource(context, R.drawable.ic_tile_ask),
                    context.mainExecutor,
                ) {}
            },
            chooseAssistant = { runCatching { context.startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)) } },
            allowNotifications = live.allow,
            turnOnLiveUpdates = {
                if (Build.VERSION.SDK_INT >= 36) runCatching {
                    context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                }
            },
        ),
    )
}

private class LiveUpdatesAccess(val ui: LiveUpdatesUi, val allow: () -> Unit)

/** Whether running T3 turns can show as Live Updates, read again on each resume, and the ask for notifications. */
@Composable
private fun rememberLiveUpdates(): LiveUpdatesAccess {
    val context = LocalContext.current
    val manager = remember { context.getSystemService(NotificationManager::class.java) }
    fun allowed() = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    fun promoted() = Build.VERSION.SDK_INT < 36 || manager.canPostPromotedNotifications()
    var notify by remember { mutableStateOf(allowed()) }
    var promote by remember { mutableStateOf(promoted()) }
    // Both can change in the system's settings while this screen is away.
    LifecycleResumeEffect(Unit) {
        notify = allowed()
        promote = promoted()
        onPauseOrDispose {}
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notify = it }
    return LiveUpdatesAccess(LiveUpdatesUi(notify, promote)) { ask.launch(Manifest.permission.POST_NOTIFICATIONS) }
}
