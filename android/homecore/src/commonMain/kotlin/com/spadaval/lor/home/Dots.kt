package com.spadaval.lor.home



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
val HomeApp.dotKey: String get() = "$packageName#$profile"

/**
 * The notifications that make dots, and whether the listener is connected, under one lock: each
 * write checks the connection and changes the notes in one step, so a write queued before a
 * disconnect can't put a dot back after it (Increment 6's second review, R1). No Android in it.
 */
class DotBook {
    private val notes = HashMap<String, Note>()
    private var connected = false

    /** The keys with a dot now. */
    @get:JvmSynchronized
    val keys: Set<String> get() = dotKeys(notes.values)

    @get:JvmSynchronized
    val size: Int get() = notes.size

    @JvmSynchronized
    fun connect() {
        connected = true
    }

    /** Disconnected: no dots, and every later write is dropped until [connect]. */
    @JvmSynchronized
    fun disconnect() {
        connected = false
        notes.clear()
    }

    /** Each returns whether it changed anything; a write while disconnected changes nothing. */
    @JvmSynchronized
    fun replace(all: List<Note>): Boolean {
        if (!connected) return false
        notes.clear()
        all.forEach { notes[it.key] = it }
        return true
    }

    @JvmSynchronized
    fun put(n: Note): Boolean {
        if (!connected) return false
        notes[n.key] = n
        return true
    }

    @JvmSynchronized
    fun remove(key: String): Boolean = connected && notes.remove(key) != null
}
