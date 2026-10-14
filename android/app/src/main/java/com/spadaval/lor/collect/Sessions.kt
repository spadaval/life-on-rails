package com.spadaval.lor.collect

/** One usage event, reduced to what session building needs. */
data class UsageEvent(val pkg: String, val type: Int, val time: Long)

/** An app on screen from start to end. */
data class Session(val pkg: String, val start: Long, val end: Long)

/**
 * Turns the system's foreground/background events into app sessions. Pure, so
 * it is tested on the JVM.
 */
object Sessions {
    // android.app.usage.UsageEvents.Event constants, copied so tests need no Android.
    const val ACTIVITY_RESUMED = 1
    const val ACTIVITY_PAUSED = 2
    const val SCREEN_NON_INTERACTIVE = 16
    const val KEYGUARD_SHOWN = 17
    const val DEVICE_SHUTDOWN = 26

    data class Result(val closed: List<Session>, val openSince: Long?, val openPkg: String?)

    /**
     * Closed sessions, plus the start of the one still open (if any) so the next
     * run can begin there. Back-to-back sessions of the same app within
     * `mergeGapMs` are one session; anything shorter than `minMs` is dropped,
     * and so is anything in `ignore` (the launcher, the system UI).
     */
    fun build(
        events: List<UsageEvent>,
        ignore: Set<String> = emptySet(),
        mergeGapMs: Long = 30_000,
        minMs: Long = 3_000,
    ): Result {
        val raw = mutableListOf<Session>()
        var current: String? = null
        var currentStart = 0L
        fun close(at: Long) {
            val pkg = current ?: return
            if (at > currentStart) raw += Session(pkg, currentStart, at)
            current = null
        }
        for (e in events.sortedBy { it.time }) {
            when (e.type) {
                ACTIVITY_RESUMED -> if (current != e.pkg) {
                    close(e.time)
                    current = e.pkg
                    currentStart = e.time
                }
                ACTIVITY_PAUSED -> if (current == e.pkg) close(e.time)
                SCREEN_NON_INTERACTIVE, KEYGUARD_SHOWN, DEVICE_SHUTDOWN -> close(e.time)
            }
        }
        val merged = mutableListOf<Session>()
        for (s in raw) {
            val last = merged.lastOrNull()
            if (last != null && last.pkg == s.pkg && s.start - last.end <= mergeGapMs) {
                merged[merged.size - 1] = last.copy(end = maxOf(last.end, s.end))
            } else {
                merged += s
            }
        }
        val openPkg = current
        // An open session continues one that just closed (same app, short gap): resume from the earlier start.
        var openSince: Long? = if (openPkg != null) currentStart else null
        if (openPkg != null && merged.isNotEmpty()) {
            val last = merged.last()
            if (last.pkg == openPkg && currentStart - last.end <= mergeGapMs) {
                openSince = last.start
                merged.removeAt(merged.size - 1)
            }
        }
        return Result(
            closed = merged.filter { it.pkg !in ignore && it.end - it.start >= minMs },
            openSince = openSince,
            openPkg = openPkg,
        )
    }
}
