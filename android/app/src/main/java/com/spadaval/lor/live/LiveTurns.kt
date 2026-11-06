package com.spadaval.lor.live

import com.spadaval.lor.t3.T3Thread
import com.spadaval.lor.t3.ThreadState
import java.time.OffsetDateTime

/** One Live Update: a T3 turn under way. [threadId] is the notification's tag. */
data class LiveTurn(
    val threadId: String,
    val title: String,
    /** The line under the title: the project and what the turn is doing. */
    val text: String,
    /** The status bar chip's text, or null for the icon alone. */
    val chip: String?,
    /** When the run started, in epoch ms, for the notification's time; null when the shell didn't say. */
    val startedAtMs: Long?,
)

/**
 * Which T3 turns show as Live Updates, from the shell's listed threads. Pure.
 *
 * One per thread with a turn under way: running, or waiting on Sumeet for an approval or an answer,
 * since the turn hasn't ended while it waits, including a side chat's or subagent's request raised onto
 * an idle parent. An idle or failed thread has none, so its Live Update ends when its turn does; so has
 * a thread whose own request outlived its run (a cancelled run still asking). The chip says only what needs him ("Approve", "Answer", each short enough
 * to fit); a turn that is just running shows the icon alone. Most recently updated first, as the shell lists them.
 */
object LiveTurns {
    fun of(threads: List<T3Thread>): List<LiveTurn> = threads.mapNotNull { t ->
        // A child's request (a side chat's, or a live subagent's) means a turn under way even when this thread's own run is idle.
        if (!t.runLive && t.state != ThreadState.RUNNING && t.requests.none { it.threadId != t.id }) return@mapNotNull null
        val (doing, chip) = when (t.state) {
            ThreadState.RUNNING -> "Running" to null
            ThreadState.WAITING_APPROVAL -> "Waiting for your approval" to "Approve"
            ThreadState.WAITING_ANSWER -> "Waiting for your answer" to "Answer"
            ThreadState.FAILED, ThreadState.IDLE -> return@mapNotNull null
        }
        LiveTurn(t.id, t.title.ifBlank { "T3 thread" }, "${t.projectTitle} · $doing", chip, epochMs(t.runStartedAt))
    }

    private fun epochMs(iso: String?): Long? = iso?.let { runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() }
}
