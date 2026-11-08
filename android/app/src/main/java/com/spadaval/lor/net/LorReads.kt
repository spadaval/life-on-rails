package com.spadaval.lor.net

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * LoR's reads for the whole app, one reader per process ([com.spadaval.lor.LorApp.reads]): the
 * agent page, the clock block, LoR's blocks and the `lor` panel draw from the same replies and
 * cause one read of each a minute between them, wherever they sit on the grid (lor-leh4, review
 * finding lor-1f2f; lor-lz97 added the lists).
 *
 * Whatever shows LoR calls [hold] while it is composed and home is started. While anything holds
 * it, the reader reads `/device/home` once a minute: at once when the first holder comes and the
 * last read began a minute or more ago, else when that minute is up. `/device/tasks` and
 * `/device/people` ([tasks], [people]) are read the same way, but only while a holder asks for
 * them (`hold(lists = true)`, the `lor` panel). When the last holder lets go the reads stop; each
 * [Kept] keeps its last good reply for the process (not on disk). Every read logs one line
 * ([log], tag `LorReads` on the phone).
 *
 * Call [hold] and [retry] on the main thread; [scope] is the process's ([com.spadaval.lor.LorApp.scope]).
 * [clock] is milliseconds that only go forward (`SystemClock.elapsedRealtime`).
 */
class LorReads(
    private val scope: CoroutineScope,
    fetchHome: suspend () -> DeviceHome,
    fetchTasks: suspend () -> DeviceTasks,
    fetchPeople: suspend () -> DevicePeople,
    private val describe: (Exception) -> String,
    private val clock: () -> Long,
    private val log: (String) -> Unit,
) {
    /**
     * One of LoR's replies as the process last read it: [value] is the last good one, [error] why
     * the last read failed (null after a good one, [value] staying as it was), and [current] that a
     * read has ended since it was last taken up from no holders, or that the kept [value] is less
     * than a minute old. A page shows a reply that isn't current greyed.
     */
    class Kept<T>(val path: String, internal val fetch: suspend () -> T) {
        var value by mutableStateOf<T?>(null)
            internal set
        var error by mutableStateOf<String?>(null)
            internal set
        var current by mutableStateOf(false)
            internal set
        /** When [value] was read, by the phone's clock: for a reply that carries no time of its own. */
        var at by mutableStateOf<java.time.Instant?>(null)
            internal set
        internal var lastStart: Long? = null
        /**
         * The number ([begun]) of the read that gave [value]: a write that ended when fewer reads
         * had begun is in [value] ([com.spadaval.lor.lor.LorWrites]).
         */
        var goodRead by mutableStateOf<Long?>(null)
            internal set
    }

    val homeRead = Kept("/device/home", fetchHome)
    val tasks = Kept("/device/tasks", fetchTasks)
    val people = Kept("/device/people", fetchPeople)

    /** The last good `/device/home` in this process. */
    val home: DeviceHome? get() = homeRead.value
    /** Why the last read of `/device/home` failed, in plain words ([LorClient.describe]); null after a good one. [home] stays as it was. */
    val error: String? get() = homeRead.error
    /** [home] is current ([Kept.current]). */
    val current: Boolean get() = homeRead.current
    /** A read asked for by [retry] is under way. */
    var retrying by mutableStateOf(false)
        private set

    /** How many reads have begun in this process, of any path; each read takes the next number. */
    var begun = 0L
        private set

    private var holders = 0
    private var listHolders = 0
    private var loop: Job? = null
    private val one = Mutex()
    /** Wakes the loop when a holder asks for the lists, so it needn't wait out the minute it was waiting. */
    private val wake = Channel<Unit>(Channel.CONFLATED)

    /** What the holders want read now. */
    private val wanted: List<Kept<*>> get() = if (listHolders > 0) listOf(homeRead, tasks, people) else listOf(homeRead)

    /** Holds the reader until the caller is cancelled; with [lists], `/device/tasks` and `/device/people` too (see the class). */
    suspend fun hold(lists: Boolean = false) {
        holders++
        if (lists) listHolders++
        val now = clock()
        if (holders == 1) stale(homeRead, now)
        if (lists && listHolders == 1) {
            stale(tasks, now)
            stale(people, now)
            wake.trySend(Unit)
        }
        if (holders == 1) {
            loop = scope.launch {
                while (true) {
                    val w = wanted
                    val (due, wait) = plan(w.map { it.lastStart }, clock())
                    if (due.isEmpty()) withTimeoutOrNull(wait) { wake.receive() }
                    else due.forEach { read(w[it]) }
                }
            }
        }
        try {
            awaitCancellation()
        } finally {
            holders--
            if (lists) listHolders--
            if (holders == 0) {
                loop?.cancel()
                loop = null
            }
        }
    }

    /** Reads what is held now instead of at the next minute (the agent page's and the panel's Retry). */
    fun retry() {
        if (retrying) return
        retrying = true
        val reads = wanted
        scope.launch {
            try {
                reads.forEach { read(it) }
            } finally {
                retrying = false
            }
        }
    }

    /**
     * Reads what is held now, after a write (lor-rz50): `/device/home`, and the lists while a
     * holder asks for them. Unlike [retry] it doesn't show "Trying…" on the Retry buttons.
     */
    fun refresh() {
        val reads = wanted
        scope.launch { reads.forEach { read(it) } }
    }

    /** A kept reply a minute old or more isn't current once taken up again. */
    private fun stale(kept: Kept<*>, now: Long) {
        if (dueIn(kept.lastStart, now) == 0L) kept.current = false
    }

    private suspend fun <T> read(kept: Kept<T>) = one.withLock {
        val started = clock()
        val number = ++begun
        val before = kept.lastStart
        kept.lastStart = started
        try {
            kept.value = kept.fetch()
            kept.goodRead = number
            kept.at = java.time.Instant.now()
            kept.error = null
            log("read ${kept.path}: ok in ${clock() - started} ms, $holders holding, $listHolders for lists")
        } catch (e: CancellationException) {
            // Let go mid-read: the next holder reads when it would have without this one.
            kept.lastStart = before
            throw e
        } catch (e: Exception) {
            kept.error = describe(e)
            log("read ${kept.path}: ${e.javaClass.simpleName} in ${clock() - started} ms, $holders holding, $listHolders for lists")
        }
        kept.current = true
    }

    companion object {
        const val EVERY_MS = 60_000L

        /** How long until the next read is due, given when the last began ([lastStart], null for never); 0 when due. */
        fun dueIn(lastStart: Long?, now: Long, every: Long = EVERY_MS): Long =
            if (lastStart == null) 0L else (lastStart + every - now).coerceIn(0L, every)

        /**
         * Of the reads wanted, whose last began at [lastStarts], the indices of those due [now], in
         * order, and with none due, how long until the first is.
         */
        fun plan(lastStarts: List<Long?>, now: Long, every: Long = EVERY_MS): Pair<List<Int>, Long> {
            val waits = lastStarts.map { dueIn(it, now, every) }
            val due = waits.indices.filter { waits[it] == 0L }
            return due to (if (due.isEmpty()) waits.min() else 0L)
        }
    }
}
