package com.spadaval.lor.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.spadaval.lor.data.Prefs
import com.spadaval.lor.net.LorClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** A quick-settings tile: tap to start a timer (Uncategorized) or stop the running one. */
class TimerTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartListening() {
        scope.launch { render(call { it.state(Prefs(this@TimerTileService).deviceName) }) }
    }

    override fun onClick() {
        qsTile?.apply {
            subtitle = "…"
            updateTile()
        }
        scope.launch { render(call { it.timer("toggle", Prefs(this@TimerTileService).deviceName) }) }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun call(block: (LorClient) -> JSONObject): Result<JSONObject> = withContext(Dispatchers.IO) {
        runCatching { block(LorClient(Prefs(this@TimerTileService).serverUrl)) }
    }

    private fun render(result: Result<JSONObject>) {
        val tile = qsTile ?: return
        val state = result.getOrNull()
        val running = state?.optJSONObject("running")
        when {
            state == null -> {
                tile.state = Tile.STATE_UNAVAILABLE
                tile.subtitle = "Offline"
            }
            running != null -> {
                tile.state = Tile.STATE_ACTIVE
                tile.subtitle = (running.optString("intent").takeIf { it.isNotBlank() && it != "null" } ?: "Running") +
                    if (running.optBoolean("paused")) " (paused)" else ""
            }
            else -> {
                tile.state = Tile.STATE_INACTIVE
                tile.subtitle = "Off"
            }
        }
        tile.updateTile()
    }
}
