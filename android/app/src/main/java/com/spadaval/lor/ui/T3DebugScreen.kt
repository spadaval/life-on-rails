package com.spadaval.lor.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spadaval.lor.t3.Pairing
import com.spadaval.lor.t3.T3Connection
import com.spadaval.lor.t3.T3Store
import com.spadaval.lor.t3.ThreadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Debug view of the T3 connection: pair, then watch the thread list. Connected only while open. */
@Composable
fun T3DebugScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val connection = remember { T3Connection.get(context) }
    val store = remember { T3Store(context) }
    val scope = rememberCoroutineScope()
    val status by connection.status.collectAsState()
    val shell by connection.shell.collectAsState()
    var server by remember { mutableStateOf(store.lastServer) }
    var code by remember { mutableStateOf("") }
    var pairing by remember { mutableStateOf<String?>(null) }

    BackHandler(onBack = onBack)
    DisposableEffect(Unit) {
        connection.start()
        onDispose { connection.stop() }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        TextButton(onClick = onBack) { Text("‹ Settings") }
        Section("Pair with T3 Code") {
            OutlinedTextField(value = server, onValueChange = { server = it }, label = { Text("Server address") }, placeholder = { Text("http://10.0.2.2:16021") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Pairing token or link") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            Text(
                "From `t3 pair` or T3's Connections settings. The address above, if set, wins over the link's host.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 6.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    scope.launch {
                        pairing = "Pairing…"
                        // pair() never starts the connection; this scope is cancelled with the screen,
                        // so a pairing that finishes after it closes leaves nothing running.
                        val result = withContext(Dispatchers.IO) { runCatching { connection.pair(Pairing.parse(server, code)) } }
                        pairing = result.fold({ code = ""; null }, { "Cannot pair: ${T3Connection.describe(it)}" })
                        connection.start()
                    }
                }) { Text("Pair") }
                OutlinedButton(onClick = { connection.forget() }) { Text("Forget") }
            }
            pairing?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
        }
        Section("Connection") {
            Text(
                when (val s = status) {
                    T3Connection.Status.NotPaired -> "Not paired."
                    T3Connection.Status.Stopped -> "Not connected."
                    T3Connection.Status.Connecting -> "Connecting to ${store.lastServer}…"
                    T3Connection.Status.Live -> "Live: ${store.lastServer}"
                    is T3Connection.Status.Retrying -> "Disconnected (${s.error}). Retrying in ${s.retryInMs / 1000}s."
                    is T3Connection.Status.Rejected -> "The server refused this phone (${s.error}). Pair again."
                },
            )
        }
        Section("Threads (${shell.threads.size})") {
            if (shell.threads.isEmpty()) Text("None yet.", style = MaterialTheme.typography.bodySmall)
            for (thread in shell.threads) {
                Column(Modifier.padding(vertical = 6.dp)) {
                    Text(thread.title, fontWeight = FontWeight.Medium)
                    Text("${thread.projectTitle} · ${thread.state.label}", style = MaterialTheme.typography.bodySmall)
                    thread.lastError?.takeIf { thread.state == ThreadState.FAILED }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}
