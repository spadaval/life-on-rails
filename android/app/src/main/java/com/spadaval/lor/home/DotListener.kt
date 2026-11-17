package com.spadaval.lor.home

import android.app.Notification
import android.app.NotificationChannel
import android.content.ComponentName
import android.content.Context
import android.app.NotificationManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/*
 * The Android half of Dots.kt (launcher polish 6.6): the listener Android binds, and the process's
 * dots it fills. Which notifications count is decided in homecore.
 */

/** The process's notifications that make dots, filled by [DotListener]. */
object Dots {
    private val book = DotBook()
    private val _withDots = MutableStateFlow<Set<String>>(emptySet())
    val withDots: StateFlow<Set<String>> = _withDots

    internal fun connect() = book.connect()

    internal fun disconnect() {
        book.disconnect()
        publish()
    }

    internal fun replace(all: List<Note>) {
        if (book.replace(all)) publish()
    }

    internal fun put(n: Note) {
        if (book.put(n)) publish()
    }

    internal fun remove(key: String) {
        if (book.remove(key)) publish()
    }

    @Synchronized
    private fun publish() {
        val now = book.keys
        if (now != _withDots.value) Log.i("LorDots", "${now.size} apps with dots from ${book.size} notifications")
        _withDots.value = now
    }

    /** LoR's listener, as Settings names it. */
    fun component(context: Context) = ComponentName(context, DotListener::class.java)

    fun granted(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java).isNotificationListenerAccessGranted(component(context))
}

/** The listener Android binds once Sumeet allows LoR under Notification access. */
class DotListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))

    private fun note(sbn: StatusBarNotification, ranking: RankingMap?): Note {
        val r = Ranking()
        val ranked = ranking?.getRanking(sbn.key, r) == true
        val n = sbn.notification
        val title = n.extras.getCharSequence(Notification.EXTRA_TITLE)
        val text = n.extras.getCharSequence(Notification.EXTRA_TEXT)
        return Note(
            key = sbn.key,
            packageName = sbn.packageName,
            user = sbn.user.toString(),
            groupSummary = n.flags and Notification.FLAG_GROUP_SUMMARY != 0,
            ongoing = n.flags and Notification.FLAG_ONGOING_EVENT != 0,
            defaultChannel = ranked && r.channel?.id == NotificationChannel.DEFAULT_CHANNEL_ID,
            canShowBadge = !ranked || r.canShowBadge(),
            hasText = !title.isNullOrEmpty() || !text.isNullOrEmpty(),
        )
    }

    override fun onListenerConnected() {
        Dots.connect()
        scope.launch {
            val all = try { activeNotifications.orEmpty() } catch (e: SecurityException) { emptyArray() }
            val ranking = currentRanking
            Dots.replace(all.map { note(it, ranking) })
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap?) {
        scope.launch { Dots.put(note(sbn, rankingMap)) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification, rankingMap: RankingMap?) {
        scope.launch { Dots.remove(sbn.key) }
    }

    override fun onNotificationRankingUpdate(rankingMap: RankingMap?) {
        scope.launch {
            val all = try { activeNotifications.orEmpty() } catch (e: SecurityException) { emptyArray() }
            Dots.replace(all.map { note(it, rankingMap) })
        }
    }

    // Synchronously: access taken away unbinds and destroys the service at once, cancelling the scope
    // before a launched clear would run (seen on the emulator), which left the dots up.
    override fun onListenerDisconnected() {
        Dots.disconnect()
    }

    override fun onDestroy() {
        scope.cancel()
        Dots.disconnect()
        super.onDestroy()
    }
}
