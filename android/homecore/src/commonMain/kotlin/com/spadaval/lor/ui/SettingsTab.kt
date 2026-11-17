package com.spadaval.lor.ui

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The Settings tab: the typed [server] and [device], the line Save or Test connection left
 * ([result]: "Saved.", "Checking…", or what the test found), whether the system can add the Ask
 * tile, and the app's [version].
 */
data class SettingsUi(
    val server: String = "",
    val device: String = "",
    val result: String? = null,
    val canAddTile: Boolean = true,
    val liveUpdates: LiveUpdatesUi = LiveUpdatesUi(),
    val version: String = "",
)

/** Whether LoR may post notifications ([notify]) and promote them to Live Updates ([promote]). */
data class LiveUpdatesUi(val notify: Boolean = true, val promote: Boolean = true)

class SettingsActions(
    val server: (String) -> Unit = {},
    val device: (String) -> Unit = {},
    val save: () -> Unit = {},
    val test: () -> Unit = {},
    val openT3: () -> Unit = {},
    val addTile: () -> Unit = {},
    val chooseAssistant: () -> Unit = {},
    val allowNotifications: () -> Unit = {},
    val turnOnLiveUpdates: () -> Unit = {},
)

@Composable
fun SettingsTab(ui: SettingsUi, on: SettingsActions) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Section("Server") {
            OutlinedTextField(value = ui.server, onValueChange = on.server, label = { Text("LoR address") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text(
                "Your box over Tailscale. The phone needs Tailscale switched on; LoR checks that requests come from your account.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 6.dp),
            )
            OutlinedTextField(value = ui.device, onValueChange = on.device, label = { Text("This phone's name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("Recorded on everything this phone sends.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(shape = androidx.compose.ui.graphics.RectangleShape, onClick = on.save) { Text("Save") }
                OutlinedButton(shape = androidx.compose.ui.graphics.RectangleShape, onClick = on.test) { Text("Test connection") }
            }
            ui.result?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
        }
        Section("T3 Code") {
            Text("Pair with a T3 Code server and watch its threads. A debug view for the launcher's agent page.", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(shape = androidx.compose.ui.graphics.RectangleShape, onClick = on.openT3, modifier = Modifier.padding(top = 8.dp)) { Text("T3 connection (debug)") }
        }
        Section("Quick send") {
            Text(
                "Send a prompt to T3 from anywhere: the Ask tile in Quick Settings, a home press while on home, " +
                    "or the assistant gesture with LoR as the digital assistant, which attaches the screen.",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                if (ui.canAddTile) OutlinedButton(shape = androidx.compose.ui.graphics.RectangleShape, onClick = on.addTile) { Text("Add the Ask tile") }
                OutlinedButton(shape = androidx.compose.ui.graphics.RectangleShape, onClick = on.chooseAssistant) { Text("Choose the assistant") }
            }
        }
        Section("Live Updates") {
            LiveUpdates(ui.liveUpdates, on)
        }
        Section("About") {
            Text("Life on Rails ${ui.version}", style = MaterialTheme.typography.bodyMedium)
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
private fun LiveUpdates(ui: LiveUpdatesUi, on: SettingsActions) {
    Text(
        "While a T3 turn runs, it shows in the status bar and on the lock screen, silently, until it ends. Tap it to open the thread.",
        style = MaterialTheme.typography.bodySmall,
    )
    Text(
        when {
            !ui.notify -> "Off: LoR may not post notifications."
            !ui.promote -> "Shown as ordinary notifications: Live Updates are off for LoR."
            else -> "On."
        },
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 8.dp),
    )
    if (!ui.notify) OutlinedButton(shape = androidx.compose.ui.graphics.RectangleShape, onClick = on.allowNotifications, modifier = Modifier.padding(top = 8.dp)) {
        Text("Allow notifications")
    } else if (!ui.promote) OutlinedButton(shape = androidx.compose.ui.graphics.RectangleShape, onClick = on.turnOnLiveUpdates, modifier = Modifier.padding(top = 8.dp)) { Text("Turn on Live Updates") }
}
