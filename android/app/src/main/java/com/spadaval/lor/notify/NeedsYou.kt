package com.spadaval.lor.notify

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.spadaval.lor.R
import org.json.JSONArray

/**
 * Tasks the agent flagged as needing you. The server hands each one over once
 * (`POST /device/notifications`); tapping it opens Today, where it waits under
 * "Needs you" until you mark it done or seen.
 */
object NeedsYou {
    private const val CHANNEL = "needs_you"

    fun permitted(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun show(context: Context, serverUrl: String, items: JSONArray) {
        if (items.length() == 0) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Needs you", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Something the agent thinks needs your reply or decision"
            },
        )
        val open = PendingIntent.getActivity(
            context, 0, Intent(Intent.ACTION_VIEW, Uri.parse("$serverUrl/")), PendingIntent.FLAG_IMMUTABLE,
        )
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            val note = item.optString("note")
            val notification = Notification.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_tile_timer)
                .setContentTitle(item.optString("title"))
                .setContentText(note)
                .setStyle(Notification.BigTextStyle().bigText(note))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            manager.notify(item.optString("id").hashCode(), notification)
        }
    }
}
