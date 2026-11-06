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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.spadaval.lor.BuildInfo
import com.spadaval.lor.R
import com.spadaval.lor.tile.AskTileService
import com.spadaval.lor.data.Prefs
import com.spadaval.lor.net.LorClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Section("Server") {
            OutlinedTextField(value = server, onValueChange = { server = it }, label = { Text("LoR address") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text(
                "Your box over Tailscale. The phone needs Tailscale switched on; LoR checks that requests come from your account.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 6.dp),
            )
            OutlinedTextField(value = device, onValueChange = { device = it }, label = { Text("This phone's name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("Recorded on everything this phone sends.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    prefs.serverUrl = server
                    prefs.deviceName = device
                    server = prefs.serverUrl
                    device = prefs.deviceName
                    result = "Saved."
                }) { Text("Save") }
                OutlinedButton(onClick = {
                    scope.launch {
                        result = "Checking…"
                        result = withContext(Dispatchers.IO) {
                            runCatching { LorClient(server.trim()).state(device) }
                                .fold({ "Connected. LoR sees you as ${it.optString("login")}." }, { "Cannot connect: ${it.message}" })
                        }
                    }
                }) { Text("Test connection") }
            }
            result?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
        }
        Section("T3 Code") {
            Text("Pair with a T3 Code server and watch its threads. A debug view for the launcher's agent page.", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { showT3 = true }, modifier = Modifier.padding(top = 8.dp)) { Text("T3 connection (debug)") }
        }
        Section("Quick send") {
            Text(
                "Send a prompt to T3 from anywhere: the Ask tile in Quick Settings, a home press while on home, " +
                    "or the assistant gesture with LoR as the digital assistant, which attaches the screen.",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) OutlinedButton(onClick = {
                    context.getSystemService(StatusBarManager::class.java).requestAddTileService(
                        ComponentName(context, AskTileService::class.java),
                        context.getString(R.string.ask_tile_label),
                        Icon.createWithResource(context, R.drawable.ic_tile_ask),
                        context.mainExecutor,
                    ) {}
                }) { Text("Add the Ask tile") }
                OutlinedButton(onClick = {
                    runCatching { context.startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)) }
                }) { Text("Choose the assistant") }
            }
        }
        Section("Live Updates") {
            LiveUpdatesSettings()
        }
        Section("About") {
            Text("Life on Rails ${BuildInfo.VERSION}", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Collects what the phone knows about your day and sends it to LoR on your box. Nothing goes anywhere else. " +
                    "Call audio is never read, and the screen only when you call LoR as the assistant, to send it with your prompt to T3.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** Whether running T3 turns can show as Live Updates, with a way to fix what's off. */
@Composable
private fun LiveUpdatesSettings() {
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
    Text(
        "While a T3 turn runs, it shows in the status bar and on the lock screen, silently, until it ends. Tap it to open the thread.",
        style = MaterialTheme.typography.bodySmall,
    )
    Text(
        when {
            !notify -> "Off: LoR may not post notifications."
            !promote -> "Shown as ordinary notifications: Live Updates are off for LoR."
            else -> "On."
        },
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 8.dp),
    )
    if (!notify) OutlinedButton(onClick = { ask.launch(Manifest.permission.POST_NOTIFICATIONS) }, modifier = Modifier.padding(top = 8.dp)) {
        Text("Allow notifications")
    } else if (!promote && Build.VERSION.SDK_INT >= 36) OutlinedButton(onClick = {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
        }
    }, modifier = Modifier.padding(top = 8.dp)) { Text("Turn on Live Updates") }
}
