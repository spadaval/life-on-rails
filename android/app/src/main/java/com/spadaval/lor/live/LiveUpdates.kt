package com.spadaval.lor.live

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import com.spadaval.lor.R
import com.spadaval.lor.agent.T3App
import com.spadaval.lor.t3.T3Connection
import com.spadaval.lor.t3.T3Links
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Running T3 turns as Live Updates (T3 Launcher, Increment 6): one ongoing notification per turn
 * under way ([LiveTurns]), promoted on Android 16 to the status bar chip and the top of the shade
 * and lock screen, and cancelled when the turn ends. A tap opens the thread in the T3 app.
 *
 * It holds the shared [T3Connection] for the life of the process, so a turn started from another
 * client shows while home is in the background. Calm: a low-importance channel (no sound, no
 * vibration, no heads-up), no progress bar (a turn has no measurable progress, and the system's
 * indeterminate bar animates without end), and a notification is reposted only when what it says changes.
 *
 * While the connection is retrying, what's shown stays; the next snapshot corrects it. Unpaired,
 * stopped or rejected, everything is cleared.
 */
class LiveUpdates private constructor(private val context: Context) {
    private val connection = T3Connection.get(context)
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    /** What is posted now, by thread id, with the environment id its tap opens (null: no tap). Touched only from the collector. */
    private val posted = HashMap<String, Pair<LiveTurn, String?>>()

    private fun start() {
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Running T3 turns", NotificationManager.IMPORTANCE_LOW).apply {
                description = "A T3 turn under way, in the status bar until it ends."
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            },
        )
        // Anything left from an earlier process is stale until the first snapshot says otherwise.
        manager.activeNotifications.filter { it.notification.channelId == CHANNEL }.forEach { manager.cancel(it.tag, it.id) }
        connection.start()
        scope.launch {
            combine(connection.status, connection.shell) { status, shell -> status to shell }.collect { (status, shell) ->
                when (status) {
                    T3Connection.Status.Live -> show(LiveTurns.of(shell.threads))
                    T3Connection.Status.Connecting, is T3Connection.Status.Retrying -> Unit
                    else -> show(emptyList())
                }
            }
        }
    }

    private suspend fun show(turns: List<LiveTurn>) {
        val next = turns.associateBy { it.threadId }
        val before = posted.keys.toSet()
        for (id in before - next.keys) {
            manager.cancel(id, ID)
            posted -= id
        }
        if (next.isEmpty()) {
            if (before.isNotEmpty()) Log.i(TAG, "live updates: 0")
            return
        }
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        // Cached by the connection per paired server, so a new pairing gets its own.
        val env = try {
            connection.environmentId()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null // Post without a tap target; the next change tries again.
        }
        for (turn in next.values) {
            if (posted[turn.threadId] == turn to env) continue
            manager.notify(turn.threadId, ID, build(turn, env))
            posted[turn.threadId] = turn to env
        }
        if (posted.keys != before) Log.i(TAG, "live updates: ${next.size} (promotable=${Build.VERSION.SDK_INT < 36 || manager.canPostPromotedNotifications()})")
    }

    private fun build(turn: LiveTurn, env: String?): Notification {
        val b = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_live_turn)
            .setContentTitle(turn.title)
            .setContentText(turn.text)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setLocalOnly(true)
        if (turn.startedAtMs != null) b.setWhen(turn.startedAtMs).setShowWhen(true) else b.setShowWhen(false)
        if (Build.VERSION.SDK_INT >= 36) {
            b.setRequestPromotedOngoing(true)
            turn.chip?.let { b.setShortCriticalText(it) }
        }
        env?.let { open(turn.threadId, it) }?.let { b.setContentIntent(it) }
        return b.build()
    }

    /** The T3 app at the thread, as the agent page's cards open it ([T3App]). */
    private fun open(threadId: String, env: String): PendingIntent? {
        val app = T3App.installed(context) ?: return null
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(T3Links.thread(app, env, threadId)))
            .setPackage(app.packageName)
            .addFlags(T3App.flags(context))
        return PendingIntent.getActivity(context, threadId.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    companion object {
        const val CHANNEL = "running_turns"
        private const val ID = 6
        private const val TAG = "LorLive"
        @Volatile private var instance: LiveUpdates? = null

        /** Starts Live Updates for the process; later calls do nothing. */
        fun start(context: Context) {
            if (instance != null) return
            synchronized(this) {
                if (instance == null) instance = LiveUpdates(context.applicationContext).also { it.start() }
            }
        }
    }
}
