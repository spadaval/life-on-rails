package com.spadaval.lor.agent

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * When the thread column reads its thread (lor-iabt, finding lor-wc8z): only while T3 is live, again
 * when the connection comes back, once per change of thread or updatedAt, one read at a time.
 */
class ThreadReadsTest {
    private val k1 = ReadKey("t1", "2026-11-08T10:00:00Z")
    private val k2 = ReadKey("t1", "2026-11-08T10:00:01Z")
    private val k3 = ReadKey("t1", "2026-11-08T10:00:02Z")
    private val k4 = ReadKey("t1", "2026-11-08T10:00:03Z")

    @Test
    fun readsWaitForTheConnectionAndComeAgainAfterAReconnect() = runTest {
        // The column composes while T3 is still connecting: the shell keeps its last list, so a
        // thread is shown, but a read now would fail with "Not connected to T3.".
        val shown = MutableStateFlow(Shown(k1, live = false))
        val reads = mutableListOf<ReadKey>()
        backgroundScope.launch { threadReads(shown).collect { reads += it; delay(100) } }
        runCurrent()
        assertEquals("no read before the connection is live", emptyList<ReadKey>(), reads)

        shown.value = Shown(k1, live = true)
        runCurrent()
        assertEquals(listOf(k1), reads)

        // The socket drops (Doze) and comes back; the new snapshot has the same updatedAt. The thread
        // is read again, so a read that failed while it was down doesn't stay the column's last word.
        advanceTimeBy(200)
        shown.value = Shown(k1, live = false)
        runCurrent()
        shown.value = Shown(k1, live = true)
        runCurrent()
        assertEquals(listOf(k1, k1), reads)
    }

    @Test
    fun oneReadAtATimeAndOneMoreForAChangeDuringIt() = runTest {
        val shown = MutableStateFlow(Shown(k1, live = true))
        val reads = mutableListOf<ReadKey>()
        backgroundScope.launch { threadReads(shown).collect { reads += it; delay(100) } }
        runCurrent()
        advanceTimeBy(200)
        // Three changes while the read of k2 runs: one more read after it, for the latest.
        shown.value = Shown(k2, live = true)
        runCurrent()
        shown.value = Shown(k3, live = true)
        runCurrent()
        shown.value = Shown(k4, live = true)
        runCurrent()
        assertEquals(listOf(k1, k2), reads)
        advanceTimeBy(150)
        assertEquals(listOf(k1, k2, k4), reads)
        // Nothing changed: no more reads.
        advanceTimeBy(1000)
        assertEquals(listOf(k1, k2, k4), reads)
    }
}
