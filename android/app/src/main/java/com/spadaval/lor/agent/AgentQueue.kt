package com.spadaval.lor.agent

import com.spadaval.lor.t3.PendingRequest
import com.spadaval.lor.t3.T3Thread
import com.spadaval.lor.t3.ThreadState
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime

/** One T3 card in the queue: what the thread waits on Sumeet for, and the request to act on. */
data class T3Card(val kind: Kind, val thread: T3Thread, val request: PendingRequest?) {
    enum class Kind(val label: String) { APPROVAL("Approval"), QUESTION("Question"), FAILED("Failed") }

    val key: String get() = request?.requestId ?: "failed:${thread.id}"
}

/**
 * What the agent page lists from T3's shell. Pure.
 *
 * The queue is approvals, then questions, then failures, each most recently updated first (the
 * shell's order). A thread is one card, for its first open request; requests the page has already
 * answered are skipped, so a card clears as soon as its call succeeds instead of waiting for the shell.
 * A failure shows for [FAILED_FOR] after its thread last changed, so an old one doesn't sit in the
 * queue for good; the T3 app still lists it.
 */
object AgentQueue {
    val FAILED_FOR: Duration = Duration.ofHours(24)

    fun cards(threads: List<T3Thread>, answered: Set<String>, now: Instant): List<T3Card> {
        val cards = threads.mapNotNull { t ->
            val open = t.requests.filter { it.requestId !in answered }
            val approval = open.firstOrNull { !it.isQuestion }
            val question = open.firstOrNull { it.isQuestion }
            when {
                approval != null -> T3Card(T3Card.Kind.APPROVAL, t, approval)
                question != null -> T3Card(T3Card.Kind.QUESTION, t, question)
                t.state == ThreadState.FAILED && recent(t, now) -> T3Card(T3Card.Kind.FAILED, t, null)
                else -> null
            }
        }
        return cards.sortedBy { it.kind.ordinal }
    }

    /** Threads with a turn under way and nothing waiting on Sumeet. */
    fun running(threads: List<T3Thread>): List<T3Thread> = threads.filter { it.state == ThreadState.RUNNING }

    private fun recent(t: T3Thread, now: Instant): Boolean =
        runCatching { OffsetDateTime.parse(t.updatedAt).toInstant() }.getOrNull()?.let { Duration.between(it, now) <= FAILED_FOR } ?: true
}
