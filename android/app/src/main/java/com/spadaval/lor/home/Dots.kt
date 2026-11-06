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
 * Notification dots (launcher polish 6.6), with the notification access Sumeet gives LoR in
 * Settings. After Launcher3's `NotificationListener` (Apache-2.0, via Lawnchair): which
 * notifications count (`notificationIsValidForUI`), kept by package and user, built off the main
 * thread; and Kvaesitso's `NotificationService` (GPL-3.0, ideas only), a listener that fills a
 * repository the UI reads. Only each notification's key, package and user are kept, in memory:
 * never its text, never on disk, never in collect/'s queue, never sent to LoR or T3.
 */

/** What [counts] needs to know of a notification. */
data class Note(
    val key: String,
    val packageName: String,
    /** As `UserHandle.toString()` writes it, which is how [HomeApp.key] writes its user. */
    val user: String,
    val groupSummary: Boolean,
    val ongoing: Boolean,
    val defaultChannel: Boolean,
    val canShowBadge: Boolean,
    val hasText: Boolean,
)

/** Launcher3's rule: a badge-able, titled, non-summary notification, and not an ongoing one on the legacy default channel. */
fun counts(n: Note): Boolean = n.canShowBadge && !n.groupSummary && n.hasText && !(n.ongoing && n.defaultChannel)

/** The `package#user` keys with a dot, from the notifications that count. */
fun dotKeys(notes: Collection<Note>): Set<String> = notes.filter(::counts).mapTo(HashSet()) { "${it.packageName}#${it.user}" }

/** The key [Dots.withDots] holds for [app]. */
val HomeApp.dotKey: String get() = "$packageName#${info.user}"

/** The process's notifications that make dots, filled by [DotListener]. */
object Dots {
    private val notes = HashMap<String, Note>()
    private val _withDots = MutableStateFlow<Set<String>>(emptySet())
    val withDots: StateFlow<Set<String>> = _withDots

    @Synchronized
    internal fun replace(all: List<Note>) {
        notes.clear()
        all.forEach { notes[it.key] = it }
        publish()
    }

    @Synchronized
    internal fun put(n: Note) {
        notes[n.key] = n
        publish()
    }

    @Synchronized
    internal fun remove(key: String) {
        if (notes.remove(key) != null) publish()
    }

    private fun publish() {
        val now = dotKeys(notes.values)
        if (now != _withDots.value) Log.i("LorDots", "${now.size} apps with dots from ${notes.size} notifications")
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
        connected = true
        scope.launch {
            val all = try { activeNotifications.orEmpty() } catch (e: SecurityException) { emptyArray() }
            val ranking = currentRanking
            Dots.replace(all.map { note(it, ranking) })
        }
    }

    /** Between connect and disconnect; a post queued on [scope] that runs after the clear must not put a dot back. */
    @Volatile private var connected = false

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap?) {
        scope.launch { if (connected) Dots.put(note(sbn, rankingMap)) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification, rankingMap: RankingMap?) {
        scope.launch { Dots.remove(sbn.key) }
    }

    override fun onNotificationRankingUpdate(rankingMap: RankingMap?) {
        scope.launch {
            if (!connected) return@launch
            val all = try { activeNotifications.orEmpty() } catch (e: SecurityException) { emptyArray() }
            Dots.replace(all.map { note(it, rankingMap) })
        }
    }

    // Synchronously: access taken away unbinds and destroys the service at once, cancelling the scope
    // before a launched clear would run (seen on the emulator), which left the dots up.
    override fun onListenerDisconnected() {
        connected = false
        Dots.replace(emptyList())
    }

    override fun onDestroy() {
        connected = false
        scope.cancel()
        Dots.replace(emptyList())
        super.onDestroy()
    }
}
