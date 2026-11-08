package com.spadaval.lor.agent

import com.spadaval.lor.t3.PendingRequest
import com.spadaval.lor.t3.T3Thread
import com.spadaval.lor.t3.ThreadState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/** The wide form's choice of thread, its Threads section, and what the reply box says on failure (lor-iabt). */
class ThreadChoiceTest {
    private fun thread(id: String, state: ThreadState = ThreadState.IDLE, at: String = "2026-11-08T09:00:00Z", requests: List<PendingRequest> = emptyList()) =
        T3Thread(id, "Thread $id", "p", "project", state, at, null, requests)

    private val now = Instant.parse("2026-11-08T10:00:00Z")

    // The shell's order: most recently updated first.
    private val idleNewest = thread("idle-newest", at = "2026-11-08T09:50:00Z")
    private val approval = thread("approval", ThreadState.WAITING_APPROVAL, "2026-11-08T09:40:00Z", listOf(PendingRequest("approval", "r1", "command_execution")))
    private val running = thread("running", ThreadState.RUNNING, "2026-11-08T09:30:00Z")
    private val idle = (1..25).map { thread("idle-$it", at = "2026-11-07T%02d:00:00Z".format(24 - it)) }
    private val threads = listOf(idleNewest, approval, running) + idle
    private val cards = AgentQueue.cards(threads, emptySet(), now)

    @Test
    fun theChosenThreadIsShownWhileItIsListed() {
        assertEquals("idle-3", ThreadChoice.shown(threads, cards, chosen = "idle-3")?.id)
        // Gone from the list (archived): the default again.
        assertEquals("approval", ThreadChoice.shown(threads, cards, chosen = "archived")?.id)
    }

    @Test
    fun withNothingChosenTheFirstCardsThreadElseTheNewest() {
        assertEquals("approval", ThreadChoice.shown(threads, cards, chosen = null)?.id)
        assertEquals("idle-newest", ThreadChoice.shown(threads, emptyList(), chosen = null)?.id)
        assertNull(ThreadChoice.shown(emptyList(), emptyList(), chosen = null))
    }

    @Test
    fun theThreadsSectionListsTheOthersNewestFirstAtMostTwenty() {
        val others = ThreadChoice.others(threads, cards)
        assertEquals(ThreadChoice.MAX_OTHERS, others.size)
        assertEquals(listOf("idle-newest", "idle-1", "idle-2"), others.take(3).map { it.id })
        // Not the card's thread nor the running one, which the sections above list.
        assertEquals(emptyList<String>(), others.map { it.id }.filter { it == "approval" || it == "running" })
    }

    @Test
    fun aReplyIsCalledNotSentOnlyWhenNoSocketWasOpen() {
        assertEquals("Not sent: Not connected to T3.", Replies.failure("Not connected to T3."))
        // A timeout or a drop: T3 may still take it (seen on the scratch server), so not "Not sent".
        val timeout = Replies.failure("T3 did not answer within 30 s.")
        assertEquals("Not confirmed: T3 did not answer within 30 s. It may still reach the thread, so check it before sending again.", timeout)
    }
}
