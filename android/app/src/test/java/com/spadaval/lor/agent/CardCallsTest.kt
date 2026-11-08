package com.spadaval.lor.agent

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** F4 (T3 Launcher 3.3): a card's call outlives the page that started it. */
class CardCallsTest {
    @Test
    fun aCallCompletesAfterThePageThatStartedItIsGone() = runBlocking {
        val process = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val page = CoroutineScope(Job() + Dispatchers.Default)
        val calls = CardCalls(process) { it.message ?: "failed" }
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var sent = false
        var job: Job? = null
        // The page starts the call, as a tap on Approve does.
        page.launch {
            job = calls.act("card", "req-1") {
                started.complete(Unit)
                release.await()
                sent = true
            }
        }.join()
        started.await()
        assertTrue("busy while it runs", "card" in calls.busy)
        // The page goes (a move two cells away, a recreated home) mid-call.
        page.cancel()
        release.complete(Unit)
        withTimeout(5_000) { job!!.join() }
        assertTrue(sent)
        assertEquals(setOf("req-1"), calls.answered)
        assertFalse("card" in calls.busy)
        process.cancel()
    }

    @Test
    fun aSecondTapWhileBusyRunsNothingAndAFailureIsShown() = runBlocking {
        val process = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val calls = CardCalls(process) { it.message ?: "failed" }
        val release = CompletableDeferred<Unit>()
        val first = calls.act("card", "req-1") {
            release.await()
            throw IllegalStateException("T3 is gone")
        }
        assertEquals(null, calls.act("card", "req-1") { error("ran twice") })
        release.complete(Unit)
        withTimeout(5_000) { first!!.join() }
        assertEquals(mapOf("card" to "T3 is gone"), calls.failures)
        assertEquals(emptySet<String>(), calls.answered)
        process.cancel()
    }

    @Test
    fun aPageBuiltAgainSeesTheCallUnderWayAndDoesNotSendItTwice() = runBlocking {
        // lor-0fbs: the process holds one CardCalls; the page left (a move two cells away) and the
        // page built on return both read it.
        val process = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val held = CardCalls(process) { it.message ?: "failed" }
        val release = CompletableDeferred<Unit>()
        var sends = 0
        val first = held.act("card", "req-1") {
            sends++
            release.await()
        }
        // The second page, built while the first page's call is in flight.
        val again = held
        assertTrue("card" in again.busy)
        assertEquals("a second tap sends nothing", null, again.act("card", "req-1") { sends++ })
        release.complete(Unit)
        withTimeout(5_000) { first!!.join() }
        assertEquals(1, sends)
        assertEquals(setOf("req-1"), again.answered)
        process.cancel()
    }
}
