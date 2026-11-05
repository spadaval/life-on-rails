package com.spadaval.lor.t3

import org.json.JSONObject

enum class ThreadState(val label: String) {
    WAITING_APPROVAL("Waiting for approval"),
    WAITING_ANSWER("Waiting for an answer"),
    RUNNING("Running"),
    FAILED("Failed"),
    IDLE("Idle"),
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
)

/**
 * The thread list from `orchestration.subscribeShell` items (T3's `OrchestrationV2ShellStreamItem`).
 * Immutable: [apply] returns the next state. Only active, listed threads are kept: archived and
 * deleted ones, subagents and side chats are left out, as T3's own thread lists do.
 */
data class Shell(
    val projects: Map<String, String> = emptyMap(),
    private val rows: Map<String, Row> = emptyMap(),
    /** True once an authoritative snapshot has arrived. */
    val loaded: Boolean = false,
) {
    data class Row(val projectId: String, val title: String, val state: ThreadState, val updatedAt: String, val lastError: String?)

    /** Most recently updated first. */
    val threads: List<T3Thread>
        get() = rows.map { (id, r) -> T3Thread(id, r.title, r.projectId, projects[r.projectId] ?: r.projectId, r.state, r.updatedAt, r.lastError) }
            .sortedByDescending { it.updatedAt }

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

        /** Null when the thread is not one T3 would list. */
        fun row(t: JSONObject): Row? {
            val lineage = t.getJSONObject("lineage")
            if (str(lineage, "relationshipToParent") == "subagent" || lineage.optBoolean("sideChat")) return null
            if (str(t, "deletedAt") != null || str(t, "archivedAt") != null) return null
            return Row(t.getString("projectId"), t.getString("title"), state(t), t.getString("updatedAt"), str(t, "lastError"))
        }

        /** As T3's thread lists rank it (`resolveThreadListV2Status`): a pending request first, then the run. */
        fun state(t: JSONObject): ThreadState {
            val request = t.optJSONObject("pendingRuntimeRequest")?.let { str(it, "kind") }
            if (request == "user_input") return ThreadState.WAITING_ANSWER
            if (request != null && request != "auth_refresh") return ThreadState.WAITING_APPROVAL
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
