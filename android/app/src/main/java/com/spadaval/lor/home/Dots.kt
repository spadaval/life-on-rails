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

/**
 * The notifications that make dots, and whether the listener is connected, under one lock: each
 * write checks the connection and changes the notes in one step, so a write queued before a
 * disconnect can't put a dot back after it (Increment 6's second review, R1). No Android in it.
 */
class DotBook {
    private val notes = HashMap<String, Note>()
    private var connected = false

    /** The keys with a dot now. */
    @get:Synchronized
    val keys: Set<String> get() = dotKeys(notes.values)

    @get:Synchronized
    val size: Int get() = notes.size

    @Synchronized
    fun connect() {
        connected = true
    }

    /** Disconnected: no dots, and every later write is dropped until [connect]. */
    @Synchronized
    fun disconnect() {
        connected = false
        notes.clear()
    }

    /** Each returns whether it changed anything; a write while disconnected changes nothing. */
    @Synchronized
    fun replace(all: List<Note>): Boolean {
        if (!connected) return false
        notes.clear()
        all.forEach { notes[it.key] = it }
        return true
    }

    @Synchronized
    fun put(n: Note): Boolean {
        if (!connected) return false
        notes[n.key] = n
        return true
    }

    @Synchronized
    fun remove(key: String): Boolean = connected && notes.remove(key) != null
}

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
