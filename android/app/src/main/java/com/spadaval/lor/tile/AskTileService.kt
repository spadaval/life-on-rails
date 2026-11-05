package com.spadaval.lor.tile

import android.app.PendingIntent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.spadaval.lor.quicksend.QuickSend

/** A quick-settings tile that collapses the shade and opens the quick-send sheet over the app in front. */
class AskTileService : TileService() {
    override fun onStartListening() {
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        val intent = QuickSend.intent(this, QuickSend.From.TILE)
        // Behind the lock screen, unlock first: the sheet sends to T3 as Sumeet.
        unlockAndRun {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        }
    }
}
