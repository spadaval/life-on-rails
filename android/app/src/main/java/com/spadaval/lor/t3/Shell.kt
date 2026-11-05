package com.spadaval.lor.t3

import org.json.JSONObject

enum class ThreadState(val label: String) {
    WAITING_APPROVAL("Waiting for approval"),
    WAITING_ANSWER("Waiting for an answer"),
    RUNNING("Running"),
    FAILED("Failed"),
    IDLE("Idle"),
}

/** A request a thread is blocked on. 2.2 approves or answers it by [threadId] and [requestId]. */
data class PendingRequest(val threadId: String, val requestId: String, val kind: String) {
    val isQuestion: Boolean get() = kind == "user_input"
}

data class T3Thread(
    val id: String,
    val title: String,
    val projectId: String,
    val projectTitle: String,
    val state: ThreadState,
    /** ISO time, as the server sent it. */
    val updatedAt: String,
    val lastError: String?,
    /** What this thread waits on Sumeet for: its own request, then those of its hidden subagents and side chats. */
    val requests: List<PendingRequest>,
)

/**
 * The thread list from `orchestration.subscribeShell` items (T3's `OrchestrationV2ShellStreamItem`).
 * Immutable: [apply] returns the next state. Archived and deleted threads are dropped. Subagents and
 * side chats are kept but not listed, as in T3's own lists; their requests raise the listed parent
 * (T3's `pendingSubagentRequestsByThread`).
 */
data class Shell(
    val projects: Map<String, String> = emptyMap(),
    private val rows: Map<String, Row> = emptyMap(),
    /** True once an authoritative snapshot has arrived. */
    val loaded: Boolean = false,
) {
    data class Row(
        val projectId: String,
        val title: String,
        /** RUNNING, FAILED or IDLE, from the run alone. */
        val run: ThreadState,
        val updatedAt: String,
        val lastError: String?,
        /** Its pending request, unless none or `auth_refresh`. */
        val request: PendingRequest?,
        val parentId: String?,
        val subagent: Boolean,
        val sideChat: Boolean,
        /** A provider-native subagent asks on its parent itself, so its own request is not rolled up. */
        val providerNative: Boolean,
        /** Whether a subagent's run is still live; once it ends its parent no longer waits on it. */
        val runLive: Boolean,
    ) {
        val listed: Boolean get() = !subagent && !sideChat
    }

    /** Listed threads, most recently updated first. */
    val threads: List<T3Thread> by lazy {
        val rolled = rolledUp()
        rows.filterValues { it.listed }.map { (id, r) ->
            val requests = listOfNotNull(r.request) + rolled[id].orEmpty()
            val state = when {
                requests.any { !it.isQuestion } -> ThreadState.WAITING_APPROVAL
                requests.isNotEmpty() -> ThreadState.WAITING_ANSWER
                else -> r.run
            }
            T3Thread(id, r.title, r.projectId, projects[r.projectId] ?: r.projectId, state, r.updatedAt, r.lastError, requests)
        }.sortedByDescending { it.updatedAt }
    }

    /**
     * Child requests by the thread they raise. A side chat raises its parent with an approval or a
     * question. A live subagent's approval raises every ancestor up the subagent and side-chat chain;
     * its questions are skipped, since the coordinator answers them.
     */
    private fun rolledUp(): Map<String, List<PendingRequest>> {
        val out = HashMap<String, MutableList<PendingRequest>>()
        for ((id, child) in rows) {
            val request = child.request ?: continue
            val parentId = child.parentId ?: continue
            if (child.sideChat) {
                out.getOrPut(parentId) { mutableListOf() } += request
                continue
            }
            if (!child.subagent || child.providerNative || !child.runLive || request.isQuestion) continue
            val seen = hashSetOf(id)
            var ancestor: String? = parentId
            while (ancestor != null && seen.add(ancestor)) {
                out.getOrPut(ancestor) { mutableListOf() } += request
                val parent = rows[ancestor]
                ancestor = if (parent != null && (parent.subagent || parent.sideChat)) parent.parentId else null
            }
        }
        return out
    }

    fun apply(item: JSONObject): Shell = when (item.optString("kind")) {
        "snapshot" -> {
            val snapshot = item.getJSONObject("snapshot")
            val projectArray = snapshot.getJSONArray("projects")
            val snapshotProjects = List(projectArray.length()) { projectArray.getJSONObject(it) }.associate { it.getString("id") to it.getString("title") }
            // A snapshot naming resolvedRepositoryIdentityRoots only patches those projects' metadata.
            if (item.has("resolvedRepositoryIdentityRoots")) {
                copy(projects = projects + snapshotProjects)
            } else {
                val threadArray = snapshot.getJSONArray("threads")
                val next = HashMap<String, Row>()
                for (i in 0 until threadArray.length()) {
                    val t = threadArray.getJSONObject(i)
                    row(t)?.let { next[t.getString("id")] = it }
                }
                Shell(snapshotProjects, next, loaded = true)
            }
        }
        "thread.updated" -> {
            val t = item.getJSONObject("thread")
            val id = t.getString("id")
            val row = if (item.optString("location") == "active") row(t) else null
            copy(rows = if (row == null) rows - id else rows + (id to row))
        }
        "thread.removed" -> copy(rows = rows - item.getString("threadId"))
        "project.updated" -> item.getJSONObject("project").let { copy(projects = projects + (it.getString("id") to it.getString("title"))) }
        "project.removed" -> copy(projects = projects - item.getString("projectId"))
        else -> this // "synchronized", and kinds newer than this client
    }

    companion object {
        private val LIVE = setOf("preparing", "queued", "starting", "running", "waiting")
        /** T3's `isChildRunLive` fallback for servers without `activityRunStatus`. */
        private val CHILD_LIVE = setOf("preparing", "starting", "running", "waiting")

        /** Null when the thread is archived or deleted. */
        fun row(t: JSONObject): Row? {
            if (str(t, "deletedAt") != null || str(t, "archivedAt") != null) return null
            val id = t.getString("id")
            val lineage = t.getJSONObject("lineage")
            val subagent = str(lineage, "relationshipToParent") == "subagent"
            val request = t.optJSONObject("pendingRuntimeRequest")
                ?.takeIf { str(it, "kind") != null && str(it, "kind") != "auth_refresh" }
                ?.let { PendingRequest(id, it.getString("id"), it.getString("kind")) }
            val runLive = if (t.has("activityRunStatus")) !t.isNull("activityRunStatus") else str(t, "status") in CHILD_LIVE
            return Row(
                projectId = t.getString("projectId"),
                title = t.getString("title"),
                run = runState(t),
                updatedAt = t.getString("updatedAt"),
                lastError = str(t, "lastError"),
                request = request,
                parentId = str(lineage, "parentThreadId"),
                subagent = subagent,
                sideChat = lineage.optBoolean("sideChat"),
                providerNative = subagent && str(t, "creationSource") == "provider",
                runLive = runLive,
            )
        }

        private fun runState(t: JSONObject): ThreadState {
            val status = str(t, "activityRunStatus") ?: str(t, "status")
            return when {
                status in LIVE -> ThreadState.RUNNING
                status == "failed" -> ThreadState.FAILED
                else -> ThreadState.IDLE
            }
        }

        /** A string field, or null when absent or JSON null (Android's optString turns null into "null"). */
        private fun str(o: JSONObject, key: String): String? = if (o.isNull(key)) null else o.optString(key)
    }
}
