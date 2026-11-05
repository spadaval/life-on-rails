package com.spadaval.lor.t3

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Frames recorded from a real T3 server (a scratch `vp run dev` with Replay threads): a fresh
 * subscribeShell whose snapshot holds an approval, a question, a failure and idle threads, then a
 * Replay "showcase" thread launched while subscribed, running to completion.
 */
class ShellTest {
    private fun frames(name: String): List<String> =
        javaClass.classLoader!!.getResource("t3/$name")!!.readText().lines().filter { it.isNotBlank() }

    /** Every shell item, in order, as the connection hands them to [Shell.apply]. */
    private fun items(name: String): List<JSONObject> = frames(name).flatMap(Rpc::decode).filterIsInstance<Rpc.Chunk>().flatMap { it.values }

    private fun Shell.byTitle(title: String) = threads.single { it.title == title }

    @Test
    fun theSnapshotDecodesEveryState() {
        val chunks = frames("shell-session.ndjson").flatMap(Rpc::decode).filterIsInstance<Rpc.Chunk>()
        // The first chunk is the authoritative snapshot; the second patches project metadata only.
        val shell = chunks.take(2).flatMap { it.values }.fold(Shell(), Shell::apply)

        assertTrue(shell.loaded)
        assertEquals(6, shell.threads.size)
        assertTrue(shell.threads.all { it.projectTitle == "server" })
        assertEquals(ThreadState.WAITING_APPROVAL, shell.byTitle("Supervised approval replay").state)
        assertEquals(ThreadState.WAITING_ANSWER, shell.byTitle("Question replay").state)
        assertEquals(ThreadState.FAILED, shell.byTitle("Broken replay").state)
        assertTrue(shell.byTitle("Broken replay").lastError!!.contains("no-such-scenario"))
        assertEquals(ThreadState.IDLE, shell.byTitle("Showcase replay").state)
        assertEquals(ThreadState.IDLE, shell.byTitle("New thread").state)
    }

    @Test
    fun aLaunchedReplayThreadRunsThenSettles() {
        val seen = mutableListOf<ThreadState>()
        val shell = items("shell-session.ndjson").fold(Shell()) { s, item ->
            s.apply(item).also { next -> next.threads.firstOrNull { it.title == "Live showcase replay" }?.let { seen += it.state } }
        }
        assertEquals(ThreadState.RUNNING, seen.first())
        assertTrue(seen.count { it == ThreadState.RUNNING } > 3)
        assertEquals(ThreadState.IDLE, shell.byTitle("Live showcase replay").state)
        assertEquals(7, shell.threads.size)
        assertEquals("Live showcase replay", shell.threads.first().title) // most recently updated first
    }

    @Test
    fun aNewConnectionsSnapshotReplacesTheList() {
        val before = items("shell-session.ndjson").fold(Shell(), Shell::apply)
        // shell-first-start.ndjson was recorded from the same server before any thread was started.
        val after = items("shell-first-start.ndjson").fold(before.copy(loaded = false), Shell::apply)
        assertTrue(after.loaded)
        assertEquals(listOf("New thread"), after.threads.map { it.title })
    }

    @Test
    fun archivedRemovedAndSubagentThreadsLeaveTheList() {
        val shell = items("shell-session.ndjson").fold(Shell(), Shell::apply)
        val thread = items("shell-session.ndjson").last { it.optString("kind") == "thread.updated" }.getJSONObject("thread")
        val id = thread.getString("id")

        val archived = shell.apply(JSONObject().put("kind", "thread.updated").put("sequence", 1).put("location", "archive").put("thread", thread))
        assertTrue(archived.threads.none { it.id == id })

        val removed = shell.apply(JSONObject().put("kind", "thread.removed").put("sequence", 1).put("location", "active").put("threadId", id))
        assertTrue(removed.threads.none { it.id == id })

        val subagent = JSONObject(thread.toString()).put("id", "child").apply { getJSONObject("lineage").put("relationshipToParent", "subagent") }
        assertNull(Shell.row(subagent))
    }

    @Test
    fun stateRanksRequestsBeforeTheRun() {
        fun t(status: String, activity: String?, request: String?) = JSONObject()
            .put("status", status)
            .put("activityRunStatus", activity ?: JSONObject.NULL)
            .put("pendingRuntimeRequest", request?.let { JSONObject().put("kind", it) } ?: JSONObject.NULL)
        assertEquals(ThreadState.WAITING_APPROVAL, Shell.state(t("running", "running", "file-change")))
        assertEquals(ThreadState.WAITING_ANSWER, Shell.state(t("running", "running", "user_input")))
        assertEquals(ThreadState.RUNNING, Shell.state(t("running", "running", "auth_refresh")))
        assertEquals(ThreadState.RUNNING, Shell.state(t("idle", "preparing", null)))
        assertEquals(ThreadState.IDLE, Shell.state(t("interrupted", null, null)))
    }
}
