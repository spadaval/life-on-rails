package com.spadaval.lor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.spadaval.lor.data.Prefs
import com.spadaval.lor.net.LorClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimerScreen() {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<JSONObject?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var showAll by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    fun act(block: (LorClient) -> JSONObject) {
        scope.launch {
            busy = true
            val result = withContext(Dispatchers.IO) { runCatching { block(LorClient(prefs.serverUrl)) } }
            result.onSuccess { state = it; error = null }.onFailure { error = "Cannot reach LoR: ${it.message}" }
            busy = false
        }
    }

    fun start(intentId: String?) {
        val what = title // read now: the request runs after the field is cleared
        title = ""
        showAll = false
        act { it.timer("start", prefs.deviceName, intentId, what) }
    }

    LaunchedEffect(Unit) {
        while (true) {
            act { it.state(prefs.deviceName) }
            delay(30_000)
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        error?.let { ErrorNote(it) }
        val s = state ?: return@Column Text("Loading…")
        val running = s.optJSONObject("running")
        if (running != null) {
            val start = Instant.parse(running.getString("t_start")).toEpochMilli()
            val pausedAt = running.optString("paused_at").takeIf { it.isNotBlank() && it != "null" }?.let { Instant.parse(it).toEpochMilli() }
            Section(if (pausedAt != null) "Paused" else "Running") {
                Text(running.optString("intent").takeUnless { it.isBlank() || it == "null" } ?: "Uncategorized", style = MaterialTheme.typography.headlineSmall)
                running.optString("title").takeUnless { it.isBlank() || it == "null" }?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                Text(elapsed(((pausedAt ?: now) - start) / 1000), style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(vertical = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = !busy, onClick = { act { it.timer("stop", prefs.deviceName) } }) { Text("Stop") }
                    OutlinedButton(enabled = !busy, onClick = { act { it.timer(if (pausedAt != null) "resume" else "pause", prefs.deviceName) } }) {
                        Text(if (pausedAt != null) "Resume" else "Pause")
                    }
                }
            }
        } else {
            Section("Start a timer") {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("What are you doing? (optional)") },
                    singleLine = true,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val recent = s.optJSONArray("recent")
                    for (i in 0 until (recent?.length() ?: 0)) {
                        val intent = recent!!.getJSONObject(i)
                        AssistChip(enabled = !busy, onClick = { start(intent.getString("id")) }, label = { Text(intent.getString("title")) })
                    }
                    AssistChip(enabled = !busy, onClick = { start(null) }, label = { Text("Just start") })
                }
                val all = s.optJSONArray("intents")
                if (all != null && all.length() > 0) {
                    TextButton(onClick = { showAll = !showAll }) { Text(if (showAll) "Hide the rest" else "Something else… (${all.length()})") }
                    if (showAll) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (i in 0 until all.length()) {
                                val intent = all.getJSONObject(i)
                                AssistChip(enabled = !busy, onClick = { start(intent.getString("id")) }, label = { Text("${intent.getString("title")} · ${intent.getString("type")}") })
                            }
                        }
                    }
                }
            }
        }
        Text(
            "Signed in to LoR as ${s.optString("login")}. The timer is also a quick-settings tile: pull down twice, tap the pencil, and drag Timer up.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
