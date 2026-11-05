package com.spadaval.lor.agent

import com.spadaval.lor.t3.PendingRequest
import com.spadaval.lor.t3.T3Thread
import com.spadaval.lor.t3.ThreadState
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class AgentQueueTest {
    private val now = Instant.parse("2026-11-05T21:00:00Z")

    private fun thread(id: String, state: ThreadState, updatedAt: String = "2026-11-05T20:00:00.000Z", vararg requests: PendingRequest) =
        T3Thread(id, "title $id", "p", "project", state, updatedAt, null, requests.toList())

    @Test
    fun approvalsThenQuestionsThenRecentFailures() {
        val threads = listOf(
            thread("failed", ThreadState.FAILED),
            thread("question", ThreadState.WAITING_ANSWER, requests = arrayOf(PendingRequest("question", "q1", "user_input"))),
            thread("old-failure", ThreadState.FAILED, "2026-11-04T20:00:00.000Z"),
            thread("approval", ThreadState.WAITING_APPROVAL, requests = arrayOf(PendingRequest("approval", "a1", "command"))),
            thread("running", ThreadState.RUNNING),
            thread("idle", ThreadState.IDLE),
        )
        val cards = AgentQueue.cards(threads, emptySet(), now)
        assertEquals(listOf("approval", "question", "failed"), cards.map { it.thread.id })
        assertEquals(listOf(T3Card.Kind.APPROVAL, T3Card.Kind.QUESTION, T3Card.Kind.FAILED), cards.map { it.kind })
        assertEquals(listOf("running"), AgentQueue.running(threads).map { it.id })
    }

    @Test
    fun anAnsweredRequestClearsItsCardAndTheNextOneShows() {
        // A thread waiting on its own approval and on a side chat's question, rolled up.
        val t = thread("t", ThreadState.WAITING_APPROVAL, requests = arrayOf(PendingRequest("t", "a1", "command"), PendingRequest("side", "q1", "user_input")))
        assertEquals("a1", AgentQueue.cards(listOf(t), emptySet(), now).single().request!!.requestId)
        val next = AgentQueue.cards(listOf(t), setOf("a1"), now).single()
        assertEquals(T3Card.Kind.QUESTION, next.kind)
        assertEquals("side", next.request!!.threadId)
        assertEquals(emptyList<T3Card>(), AgentQueue.cards(listOf(t), setOf("a1", "q1"), now))
    }
}
