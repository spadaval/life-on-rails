package com.spadaval.lor.t3

/*
 * What T3's shell and threads hold, as the agent page and the T3 debug screen read them. The app's
 * connection (`Shell`, `Commands`) reads them off the wire; the lab writes them in fixtures.
 */

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
    /** Whether its own run is under way (a request can outlive a cancelled run). */
    val runLive: Boolean = state == ThreadState.RUNNING,
    /** ISO time the live run started (`activityRunStartedAt`), or null when no run is live. */
    val runStartedAt: String? = null,
)

/** One question of a pending `user_input` request (T3's `OrchestrationV2UserInputQuestion`). */
data class Question(
    val id: String,
    val header: String,
    val question: String,
    val options: List<Option>,
    val multiSelect: Boolean,
    /** False only when the server says so; otherwise free text is accepted. */
    val allowCustomAnswer: Boolean,
    /** Text the answer field starts with; when set, typed text is sent untrimmed (T3's `initialAnswer`). */
    val initialAnswer: String? = null,
) {
    /** [value] is null when the provider gave none; [answer] is what the server takes: the value, else the label. */
    data class Option(val label: String, val description: String, val value: String? = null) {
        val answer: String get() = value ?: label
    }
}

/**
 * A user or assistant message of a thread (T3's `OrchestrationV2ConversationMessage`), as the T3
 * panel's thread column shows it: plain text, with who wrote it ([fromUser]) and when ([createdAt], ISO).
 */
data class ThreadMessage(val id: String, val fromUser: Boolean, val text: String, val createdAt: String, val streaming: Boolean)
