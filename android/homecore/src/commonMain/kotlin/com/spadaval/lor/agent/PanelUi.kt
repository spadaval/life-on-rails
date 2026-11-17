package com.spadaval.lor.agent

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.spadaval.lor.lor.LorHome
import com.spadaval.lor.lor.LorPeople
import com.spadaval.lor.lor.LorTab
import com.spadaval.lor.lor.LorTasks
import com.spadaval.lor.t3.PendingRequest
import com.spadaval.lor.t3.Question
import com.spadaval.lor.t3.T3Thread
import com.spadaval.lor.t3.ThreadMessage
import kotlin.time.Instant

/*
 * What the agent page, the LoR panel and LoR's blocks draw from (lor-rgx4), and what their taps ask
 * for. The app builds a [PanelUi] from LoR's reader, its writes and the T3 connection
 * (`agent/PanelBuild.kt`) and carries the [PanelActions] out; the lab builds both from a fixture.
 * Nothing here reads LoR or T3.
 */

/** T3's connection as the panels say it: Stopped draws as [CONNECTING]. */
enum class T3Link { NOT_PAIRED, CONNECTING, LIVE, RETRYING, REJECTED }

/** What T3 shows: the connection, its threads, and the cards' calls (`CardCalls`). */
data class T3Ui(
    val link: T3Link = T3Link.NOT_PAIRED,
    /** Why T3 refused this phone, for [T3Link.REJECTED]. */
    val error: String? = null,
    /** The shell's first snapshot has arrived. */
    val loaded: Boolean = false,
    /** The listed threads, most recently updated first. */
    val threads: List<T3Thread> = emptyList(),
    /** Requests answered from the page, hidden before the shell catches up. */
    val answered: Set<String> = emptySet(),
    /** Card keys with a call under way. */
    val busy: Set<String> = emptySet(),
    /** Why a card's last call failed, by card key. */
    val failures: Map<String, String> = emptyMap(),
) {
    val live: Boolean get() = link == T3Link.LIVE
    /** Paired, whether or not the connection is up just now; a refused session needs pairing again. */
    val paired: Boolean get() = link != T3Link.NOT_PAIRED && link != T3Link.REJECTED
}

/** One of LoR's list reads (`LorReads.Kept`): its last reply, why the last read failed, whether [value] is current, and when it was read. */
data class Kept<T>(val value: T? = null, val error: String? = null, val current: Boolean = false, val at: Instant? = null)

/** What LoR shows: its replies with the panels' writes held over them (`LorWrites`), and the writes' state. */
data class LorUi(
    /** `/device/home`'s last good reply; null until one arrives. */
    val home: LorHome? = null,
    /** Why the last read of `/device/home` failed, in plain words. */
    val error: String? = null,
    /** [home] is current; until then a kept one is drawn greyed. */
    val current: Boolean = false,
    /** A read asked for by Retry is under way. */
    val retrying: Boolean = false,
    val tasks: Kept<LorTasks> = Kept(),
    val people: Kept<LorPeople> = Kept(),
    /** Task ids, and [TIMER], with a write under way. */
    val busy: Set<String> = emptySet(),
    /** Why a write failed, by task id or [TIMER]. */
    val failures: Map<String, String> = emptyMap(),
    /** Whether a task is done, by id, where a write since the reply says so. */
    val done: Map<String, Boolean> = emptyMap(),
    /** The phone's clock and zone: for what LoR's replies carry no time for. */
    val now: Instant = Instant.fromEpochSeconds(0),
    val zone: String = "UTC",
) {
    /** Whether [id] is done, given whether the reply had it done. */
    fun done(id: String, wasDone: Boolean): Boolean = done[id] ?: wasDone

    companion object {
        /** The timer's key in [busy] and [failures]. */
        const val TIMER = "timer"
    }
}

/** Replies typed in the thread column, by thread: the drafts, the sends under way, and why one failed. */
data class RepliesUi(
    val drafts: Map<String, String> = emptyMap(),
    val sending: Set<String> = emptySet(),
    val failures: Map<String, String> = emptyMap(),
)

/** Everything the agent page, the LoR panel and the blocks draw. */
data class PanelUi(
    val lor: LorUi = LorUi(),
    val t3: T3Ui = T3Ui(),
    /** One line about the last thing a panel couldn't do outside a card, such as opening T3. */
    val notice: String? = null,
    /** The thread chosen in the agent page's wide form. */
    val chosen: String? = null,
    val replies: RepliesUi = RepliesUi(),
    /** The LoR panel's wide form's tab. */
    val tab: LorTab = LorTab.TASKS,
)

/** Where a tap opened something from: a card's icon, or the card itself (`LaunchFrom.AGENT_ICON`, `AGENT_CARD`). */
enum class Tap { ICON, CARD }

/** A write to a task (`TaskAction`). */
enum class TaskWrite { DONE, REOPEN, SEEN, QUEUE, UNQUEUE }

/** An intent the timer's Start menu offers (`DeviceState.Intent`). */
data class StartIntent(val id: String, val title: String)

/** A read under way is null; a finished one has its [value] or why it failed ([error]). */
data class Loaded<T>(val value: T? = null, val error: String? = null)

/** One read of a thread's end: its last messages, or why they couldn't be read (the last good ones stay, with the reason). */
data class ThreadRead(val threadId: String, val messages: List<ThreadMessage>?, val error: String?)

/** Pairing with T3: [scan] reads T3's QR code and gives what it found or why not; [pair] pairs with a link and gives why it failed, or null. */
class PairCalls(
    val scan: (found: (String) -> Unit, failed: (String) -> Unit) -> Unit = { _, _ -> },
    val pair: (link: String, done: (String?) -> Unit) -> Unit = { _, _ -> },
)

/**
 * What the panels' taps ask for. The composable ones are the reads a part makes while it is shown
 * (a question's choices, the Start menu's intents, the thread column's messages): the app reads T3
 * or LoR in them, the lab answers from its fixture. Each returns null while its read is under way.
 */
class PanelActions(
    /** Reads LoR now instead of at the next minute. */
    val retry: () -> Unit = {},
    /** `POST /device/timer`: start (with an intent id, or null for Untitled), pause, resume, stop. */
    val timer: (action: String, intentId: String?) -> Unit = { _, _ -> },
    /** The intents timed most recently, read when the Start menu opens. */
    val recentIntents: @Composable () -> Loaded<List<StartIntent>>? = { null },
    /** A write to a task, with what the Undo bar says and the write Undo sends; [thread] is a run's T3 thread. */
    val task: (id: String, title: String, action: TaskWrite, say: String?, undo: TaskWrite?, thread: String?) -> Unit = { _, _, _, _, _, _ -> },
    /** Opens LoR's dashboard at a path. */
    val openLor: (path: String, from: Tap) -> Unit = { _, _ -> },
    /** Opens the T3 app at a thread. */
    val openThread: (threadId: String, from: Tap) -> Unit = { _, _ -> },
    val approve: (card: T3Card, approve: Boolean) -> Unit = { _, _ -> },
    val answer: (card: T3Card, answers: Map<String, Any>) -> Unit = { _, _ -> },
    /** A question's questions, read while its choices are open and again when T3 comes back. */
    val questions: @Composable (request: PendingRequest, live: Boolean) -> Loaded<List<Question>>? = { _, _ -> null },
    /** Chooses the thread the wide form's right column shows. */
    val choose: (threadId: String) -> Unit = {},
    /** The shown thread's last messages, read while it is shown and again when it changes. */
    val readThread: @Composable (thread: T3Thread, live: Boolean) -> ThreadRead? = { _, _ -> null },
    val editReply: (threadId: String, text: String) -> Unit = { _, _ -> },
    val sendReply: (threadId: String) -> Unit = {},
    /** Opens the quick-send sheet. */
    val quickSend: () -> Unit = {},
    val pair: PairCalls = PairCalls(),
    /** Picks the LoR panel's wide tab. */
    val tab: (LorTab) -> Unit = {},
)

/**
 * What home's grid asks of a panel in it (lor-qdmy): [insets] keeps the panel off the system bars on
 * the cell's open sides, and [scrolls] tells the grid a column takes a vertical swipe until its edge
 * ([reversed] for one anchored at its end). Outside home, nothing.
 */
class InGrid(
    val insets: Modifier = Modifier,
    val scrolls: (ScrollState, reversed: Boolean) -> Modifier = { _, _ -> Modifier },
)
