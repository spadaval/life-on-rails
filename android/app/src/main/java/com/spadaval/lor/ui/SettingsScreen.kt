package com.spadaval.lor.ui

import android.Manifest
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
import com.spadaval.lor.BuildInfo
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
        Section("About") {
            Text("Life on Rails ${BuildInfo.VERSION}", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Collects what the phone knows about your day and sends it to LoR on your box. Nothing goes anywhere else. " +
                    "Message text, call audio, and screen contents are never read.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
