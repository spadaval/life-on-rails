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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** An intent a timer can start under: `/device/state`'s `recent` and `intents`. */
data class TimerIntent(val id: String, val title: String, val type: String = "")

/** A timer that is going: its intent's title (null for none), its own title, when it started and when it was paused, in epoch ms. */
data class RunningTimer(val intent: String?, val title: String?, val startMs: Long, val pausedAtMs: Long? = null)

/** LoR's `/device/state` reply, as the Timer tab reads it. */
data class TimerReply(
    val login: String,
    val running: RunningTimer? = null,
    val recent: List<TimerIntent> = emptyList(),
    val intents: List<TimerIntent> = emptyList(),
)

/**
 * The Timer tab: the last [reply] (null until one comes), the last [error], whether a request is
 * under way ([busy]), the typed [title], whether every intent is listed ([showAll]), and [now].
 */
data class TimerUi(
    val reply: TimerReply? = null,
    val error: String? = null,
    val busy: Boolean = false,
    val title: String = "",
    val showAll: Boolean = false,
    val now: Long = 0,
)

/** What the Timer tab's taps ask for. [start] takes an intent's id, null for none; [pause] true pauses, false resumes. */
class TimerActions(
    val title: (String) -> Unit = {},
    val start: (String?) -> Unit = {},
    val stop: () -> Unit = {},
    val pause: (Boolean) -> Unit = {},
    val showAll: () -> Unit = {},
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimerTab(ui: TimerUi, on: TimerActions) {
    val busy = ui.busy
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        ui.error?.let { ErrorNote(it) }
        val s = ui.reply ?: return@Column Text("Loading…")
        val running = s.running
        if (running != null) {
            val pausedAt = running.pausedAtMs
            Section(if (pausedAt != null) "Paused" else "Running") {
                Text(running.intent ?: "Uncategorized", style = MaterialTheme.typography.headlineSmall)
                running.title?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                Text(elapsed(((pausedAt ?: ui.now) - running.startMs) / 1000), style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(vertical = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(shape = androidx.compose.ui.graphics.RectangleShape, enabled = !busy, onClick = on.stop) { Text("Stop") }
                    OutlinedButton(shape = androidx.compose.ui.graphics.RectangleShape, enabled = !busy, onClick = { on.pause(pausedAt == null) }) {
                        Text(if (pausedAt != null) "Resume" else "Pause")
                    }
                }
            }
        } else {
            Section("Start a timer") {
                OutlinedTextField(
                    value = ui.title,
                    onValueChange = on.title,
                    label = { Text("What are you doing? (optional)") },
                    singleLine = true,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (intent in s.recent) {
                        AssistChip(enabled = !busy, onClick = { on.start(intent.id) }, label = { Text(intent.title) })
                    }
                    AssistChip(enabled = !busy, onClick = { on.start(null) }, label = { Text("Just start") })
                }
                val all = s.intents
                if (all.isNotEmpty()) {
                    TextButton(shape = androidx.compose.ui.graphics.RectangleShape, onClick = on.showAll) { Text(if (ui.showAll) "Hide the rest" else "Something else… (${all.size})") }
                    if (ui.showAll) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (intent in all) {
                                AssistChip(enabled = !busy, onClick = { on.start(intent.id) }, label = { Text("${intent.title} · ${intent.type}") })
                            }
                        }
                    }
                }
            }
        }
        Text(
            "Signed in to LoR as ${s.login}. The timer is also a quick-settings tile: pull down twice, tap the pencil, and drag Timer up.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
