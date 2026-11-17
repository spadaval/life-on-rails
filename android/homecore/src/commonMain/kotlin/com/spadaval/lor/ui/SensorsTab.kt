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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spadaval.lor.data.Sensor

/** What a sensor needs before it can collect, in words: whether it can, what it says, and the button that fixes it. */
data class SensorAccess(val ready: Boolean, val status: String, val action: String?)

/** The Sync card: the last sync and upload (epoch ms, 0 for never), the summary and errors it left, and what waits. */
data class SyncUi(
    val lastSyncAt: Long = 0,
    val summary: String = "Never synced.",
    val uploadedTotal: Long = 0,
    val lastUploadAt: Long = 0,
    val pending: Long = 0,
    /** One per line; blank for none. */
    val errors: String = "",
    val syncing: Boolean = false,
)

/** The Sensors tab: the Sync card, each sensor's switch, and its access once it has been read (absent until then). */
data class SensorsUi(
    val sync: SyncUi = SyncUi(),
    val on: Map<Sensor, Boolean> = emptyMap(),
    val access: Map<Sensor, SensorAccess> = emptyMap(),
    val now: Long = 0,
)

class SensorsActions(
    val sync: () -> Unit = {},
    val turn: (Sensor, Boolean) -> Unit = { _, _ -> },
    /** The sensor's [SensorAccess.action]: a permission, or a page of the system's settings. */
    val request: (Sensor) -> Unit = {},
)

@Composable
fun SensorsTab(ui: SensorsUi, on: SensorsActions) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        val sync = ui.sync
        Section("Sync") {
            Text("Last sync ${ago(sync.lastSyncAt, ui.now)}. ${sync.summary}")
            Text(
                "Uploaded ${sync.uploadedTotal} so far; last upload ${ago(sync.lastUploadAt, ui.now)}." + if (sync.pending > 0) " ${sync.pending} waiting." else "",
                style = MaterialTheme.typography.bodySmall,
            )
            if (sync.errors.isNotBlank()) {
                Text(sync.errors, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }
            Button(
                shape = androidx.compose.ui.graphics.RectangleShape,
                enabled = !sync.syncing,
                modifier = Modifier.padding(top = 8.dp),
                onClick = on.sync,
            ) { Text(if (sync.syncing) "Syncing…" else "Sync now") }
            Text("Runs by itself about every 15 minutes.", style = MaterialTheme.typography.bodySmall)
        }

        for (sensor in Sensor.entries) {
            val a = ui.access[sensor]
            val enabled = ui.on[sensor] ?: sensor.defaultOn
            Section {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(sensor.label, style = MaterialTheme.typography.titleMedium)
                        Text(sensor.description, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = enabled, onCheckedChange = { on.turn(sensor, it) })
                }
                if (enabled && a != null) {
                    Text(
                        a.status,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (a.ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    if (a.action != null) {
                        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End) {
                            OutlinedButton(shape = androidx.compose.ui.graphics.RectangleShape, onClick = { on.request(sensor) }) { Text(a.action) }
                        }
                    }
                }
            }
        }
    }
}
