package com.spadaval.lor.t3

import org.json.JSONObject
import org.junit.Assert.assertEquals
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
        val withSubagent = shell.apply(updated(subagent))
        assertTrue(withSubagent.threads.none { it.id == "child" })
        assertEquals(shell.threads.size, withSubagent.threads.size)
    }

    /** The recorded snapshot, and a copy of one of its threads made a child of [parentId]. */
    private val snapshot = items("shell-session.ndjson").take(2).fold(Shell(), Shell::apply)
    private val recorded: List<JSONObject> = items("shell-session.ndjson").first().getJSONObject("snapshot").getJSONArray("threads")
        .let { a -> List(a.length()) { a.getJSONObject(it) } }

    private fun child(fromTitle: String, id: String, parentId: String, subagent: Boolean = true, sideChat: Boolean = false) =
        JSONObject(recorded.single { it.getString("title") == fromTitle }.toString()).put("id", id).apply {
            getJSONObject("lineage").put("parentThreadId", parentId).put("relationshipToParent", if (subagent) "subagent" else JSONObject.NULL).put("sideChat", sideChat)
        }

    private fun updated(t: JSONObject, location: String = "active") =
        JSONObject().put("kind", "thread.updated").put("sequence", 1).put("location", location).put("thread", t)

    private val parent get() = snapshot.byTitle("Showcase replay")

    @Test
    fun aSubagentsApprovalRaisesItsAncestors() {
        assertEquals(ThreadState.IDLE, parent.state)
        val approval = recorded.single { it.getString("title") == "Supervised approval replay" }.getJSONObject("pendingRuntimeRequest").getString("id")
        // parent <- sub <- grandchild, and only the grandchild asks.
        val sub = child("Showcase replay", "sub", parent.id)
        val grandchild = child("Supervised approval replay", "grandchild", "sub")
        val shell = snapshot.apply(updated(sub)).apply(updated(grandchild))

        assertEquals(6, shell.threads.size)
        val raised = shell.byTitle("Showcase replay")
        assertEquals(ThreadState.WAITING_APPROVAL, raised.state)
        assertEquals(listOf(PendingRequest("grandchild", approval, "command")), raised.requests)

        // Archiving the child clears it.
        assertEquals(ThreadState.IDLE, shell.apply(updated(grandchild, "archive")).byTitle("Showcase replay").state)
    }

    @Test
    fun aSubagentsQuestionEndedRunOrNativeProviderDoesNotRaiseTheParent() {
        val question = child("Question replay", "q", parent.id)
        val ended = child("Supervised approval replay", "ended", parent.id).put("activityRunStatus", JSONObject.NULL).put("status", "completed")
        val native = child("Supervised approval replay", "native", parent.id).put("creationSource", "provider")
        for (c in listOf(question, ended, native)) {
            val raised = snapshot.apply(updated(c)).byTitle("Showcase replay")
            assertEquals(c.getString("id"), ThreadState.IDLE, raised.state)
            assertTrue(raised.requests.isEmpty())
        }
    }

    @Test
    fun aSideChatsQuestionOrApprovalRaisesItsParent() {
        val question = child("Question replay", "side", parent.id, subagent = false, sideChat = true)
        val asked = snapshot.apply(updated(question))
        assertEquals(6, asked.threads.size)
        assertEquals(ThreadState.WAITING_ANSWER, asked.byTitle("Showcase replay").state)
        assertEquals("side", asked.byTitle("Showcase replay").requests.single().threadId)

        val approval = child("Supervised approval replay", "side", parent.id, subagent = false, sideChat = true)
        assertEquals(ThreadState.WAITING_APPROVAL, snapshot.apply(updated(approval)).byTitle("Showcase replay").state)
    }

    @Test
    fun aThreadsOwnRequestIsReachable() {
        val own = snapshot.byTitle("Question replay").requests.single()
        assertEquals(snapshot.byTitle("Question replay").id, own.threadId)
        assertEquals("user_input", own.kind)
        assertTrue(snapshot.byTitle("Broken replay").requests.isEmpty())
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
