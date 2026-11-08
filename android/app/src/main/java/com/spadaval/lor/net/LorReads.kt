package com.spadaval.lor.net

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * LoR's `/device/home` for the whole app, one per process ([com.spadaval.lor.LorApp.reads]): the
 * agent page and the clock block draw from the same reply and cause one read a minute between
 * them, wherever they sit on the grid (lor-leh4, review finding lor-1f2f).
 *
 * Whatever shows LoR calls [hold] while it is composed and home is started. While anything holds
 * it, the reader reads once a minute: at once when the first holder comes and the last read began
 * a minute or more ago, else when that minute is up. When the last holder lets go the reads stop;
 * [home] keeps the last good reply for the process (not on disk). Every read logs one line ([log],
 * tag `LorReads` on the phone).
 *
 * Call [hold] and [retry] on the main thread; [scope] is the process's ([com.spadaval.lor.LorApp.scope]).
 * [clock] is milliseconds that only go forward (`SystemClock.elapsedRealtime`).
 */
class LorReads(
    private val scope: CoroutineScope,
    private val fetch: suspend () -> DeviceHome,
    private val describe: (Exception) -> String,
    private val clock: () -> Long,
    private val log: (String) -> Unit,
) {
    /** The last good reply in this process. */
    var home by mutableStateOf<DeviceHome?>(null)
        private set
    /** Why the last read failed, in plain words ([LorClient.describe]); null after a good one. [home] stays as it was. */
    var error by mutableStateOf<String?>(null)
        private set
    /**
     * [home] is current: a read has ended since the reader was last taken up from no holders, or the
     * reply kept from before is less than a minute old. A page shows a reply that isn't greyed.
     */
    var current by mutableStateOf(false)
        private set
    /** A read asked for by [retry] is under way. */
    var retrying by mutableStateOf(false)
        private set

    private var holders = 0
    private var loop: Job? = null
    private var lastStart: Long? = null
    private val one = Mutex()

    /** Holds the reader until the caller is cancelled (see the class). */
    suspend fun hold() {
        holders++
        if (holders == 1) {
            if (dueIn(lastStart, clock()) == 0L) current = false
            loop = scope.launch {
                while (true) {
                    val wait = dueIn(lastStart, clock())
                    if (wait > 0) delay(wait) else read()
                }
            }
        }
        try {
            awaitCancellation()
        } finally {
            holders--
            if (holders == 0) {
                loop?.cancel()
                loop = null
            }
        }
    }

    /** Reads now instead of at the next minute (the agent page's Retry). */
    fun retry() {
        if (retrying) return
        retrying = true
        scope.launch {
            try {
                read()
            } finally {
                retrying = false
            }
        }
    }

    private suspend fun read() = one.withLock {
        val started = clock()
        val before = lastStart
        lastStart = started
        try {
            home = fetch()
            error = null
            log("read /device/home: ok in ${clock() - started} ms, $holders holding")
        } catch (e: CancellationException) {
            // Let go mid-read: the next holder reads when it would have without this one.
            lastStart = before
            throw e
        } catch (e: Exception) {
            error = describe(e)
            log("read /device/home: ${e.javaClass.simpleName} in ${clock() - started} ms, $holders holding")
        }
        current = true
    }

    companion object {
        const val EVERY_MS = 60_000L

        /** How long until the next read is due, given when the last began ([lastStart], null for never); 0 when due. */
        fun dueIn(lastStart: Long?, now: Long, every: Long = EVERY_MS): Long =
            if (lastStart == null) 0L else (lastStart + every - now).coerceIn(0L, every)
    }
}
