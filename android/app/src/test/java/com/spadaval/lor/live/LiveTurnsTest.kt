package com.spadaval.lor.live

import com.spadaval.lor.t3.PendingRequest
import com.spadaval.lor.t3.Rpc
import com.spadaval.lor.t3.Shell
import com.spadaval.lor.t3.T3Thread
import com.spadaval.lor.t3.ThreadState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveTurnsTest {
    private fun thread(id: String, state: ThreadState, started: String? = "2026-11-05T20:00:00.000Z", vararg requests: PendingRequest, live: Boolean = state != ThreadState.IDLE && state != ThreadState.FAILED) =
        T3Thread(id, "title $id", "p", "launcher", state, "2026-11-05T20:01:00.000Z", null, requests.toList(), live, started)

    @Test
    fun oneUpdatePerTurnUnderWayAndNoneForIdleOrFailed() {
        val turns = LiveTurns.of(
            listOf(
                thread("running", ThreadState.RUNNING),
                thread("approval", ThreadState.WAITING_APPROVAL, requests = arrayOf(PendingRequest("approval", "a1", "command"))),
                thread("question", ThreadState.WAITING_ANSWER, requests = arrayOf(PendingRequest("question", "q1", "user_input"))),
                thread("idle", ThreadState.IDLE, null),
                thread("failed", ThreadState.FAILED, null),
            ),
        )
        assertEquals(listOf("running", "approval", "question"), turns.map { it.threadId })
        assertEquals(listOf("launcher · Running", "launcher · Waiting for your approval", "launcher · Waiting for your answer"), turns.map { it.text })
        // The chip names only what needs Sumeet, in a word short enough for the 96 dp chip.
        assertEquals(listOf(null, "Approve", "Answer"), turns.map { it.chip })
        assertTrue(turns.mapNotNull { it.chip }.all { it.length <= 7 })
        assertEquals(1793908800000L, turns.first().startedAtMs)
    }

    @Test
    fun aQuestionLeftFromACancelledRunHasNone() {
        val stale = thread("stale", ThreadState.WAITING_ANSWER, null, PendingRequest("stale", "q1", "user_input"), live = false)
        assertEquals(emptyList<LiveTurn>(), LiveTurns.of(listOf(stale)))
    }

    @Test
    fun aSideChatsRequestOnAnIdleParentShows() {
        val parent = thread("parent", ThreadState.WAITING_APPROVAL, null, PendingRequest("side", "a1", "command"), live = false)
        assertEquals("Approve", LiveTurns.of(listOf(parent)).single().chip)
    }

    @Test
    fun aMissingOrBadStartTimeLeavesTheTimeOut() {
        assertNull(LiveTurns.of(listOf(thread("a", ThreadState.RUNNING, null))).single().startedAtMs)
        assertNull(LiveTurns.of(listOf(thread("a", ThreadState.RUNNING, "soon"))).single().startedAtMs)
    }

    @Test
    fun aBlankTitleStillHasATitle() {
        val t = thread("a", ThreadState.RUNNING).copy(title = " ")
        assertEquals("T3 thread", LiveTurns.of(listOf(t)).single().title)
    }

    /** The recorded Replay run from ShellTest: its update appears with the run and ends with it. */
    @Test
    fun aRecordedRunShowsAndThenEnds() {
        val items = javaClass.classLoader!!.getResource("t3/shell-session.ndjson")!!.readText().lines()
            .filter { it.isNotBlank() }.flatMap(Rpc::decode).filterIsInstance<Rpc.Chunk>().flatMap { it.values }
        val seen = mutableListOf<LiveTurn?>()
        val shell = items.fold(Shell()) { s, item ->
            s.apply(item).also { next ->
                val t = next.threads.firstOrNull { it.title == "Live showcase replay" } ?: return@also
                seen += LiveTurns.of(next.threads).firstOrNull { it.threadId == t.id }
            }
        }
        val first = seen.first()
        assertNotNull(first)
        assertNotNull("the shell carries the run's start", first!!.startedAtMs)
        assertNull("it ends with the run", seen.last())
        assertTrue(LiveTurns.of(shell.threads).none { it.title == "Live showcase replay" })
    }
}
