package com.spadaval.lor.agent

import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spadaval.lor.LorApp
import com.spadaval.lor.collect.LaunchFrom
import com.spadaval.lor.home.listTakes
import com.spadaval.lor.home.scrollsInGrid
import com.spadaval.lor.t3.Commands
import com.spadaval.lor.t3.T3Connection
import com.spadaval.lor.t3.T3Thread
import com.spadaval.lor.t3.ThreadMessage
import com.spadaval.lor.t3.ThreadState
import com.spadaval.lor.ui.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/*
 * The T3 panel's wide form (lor-iabt): on the inner screen the agent page's column sits on the left
 * with a Threads section under it, and the chosen thread on the right, with its last messages, what
 * it waits on, and a reply box. The thread is read with `getThreadProjection` when it is shown and
 * again when its shell row's updatedAt changes (decision note on lor-iabt); messages are plain text.
 */

/** Which thread the wide form shows, and which threads its Threads section lists. */
object ThreadChoice {
    /** The thread chosen in the wide form, kept for the process, so a fold and an unfold keep it. */
    var chosen by mutableStateOf<String?>(null)

    const val MAX_OTHERS = 20

    /** The thread to show: [chosen] while it is listed, else the first T3 card's, else the most recently updated. */
    fun shown(threads: List<T3Thread>, cards: List<T3Card>, chosen: String? = this.chosen): T3Thread? =
        threads.firstOrNull { it.id == chosen } ?: cards.firstOrNull()?.thread ?: threads.firstOrNull()

    /** The listed threads not already in Running or Needs you, most recently updated first, at most [MAX_OTHERS]. */
    fun others(threads: List<T3Thread>, cards: List<T3Card>): List<T3Thread> {
        val above = cards.map { it.thread.id }.toSet() + AgentQueue.running(threads).map { it.id }
        return threads.filter { it.id !in above }.take(MAX_OTHERS)
    }
}

/**
 * Replies typed in the thread column, by thread, held for the process so a fold or a trip away
 * keeps a draft. A send runs in the process's scope, so leaving the panel doesn't cancel it; at
 * most one runs per thread. On success the draft clears (unless it was edited meanwhile); on
 * failure it stays, with the reason ([failure]).
 */
object Replies {
    var drafts by mutableStateOf(emptyMap<String, String>())
        private set
    var sending by mutableStateOf(emptySet<String>())
        private set
    var failures by mutableStateOf(emptyMap<String, String>())
        private set

    /**
     * What the box says when a send fails. Only with no open socket is it known not to have gone;
     * after a timeout or a dropped connection T3 may still take it (seen on the scratch server: a
     * reply timed out while the server was paused and arrived when it resumed), so the box says
     * so rather than inviting a second copy.
     */
    fun failure(reason: String): String =
        if (reason == NOT_CONNECTED) "Not sent: $reason"
        else "Not confirmed: $reason It may still reach the thread, so check it before sending again."

    /** [T3Connection.call]'s refusal when no socket is open: nothing was sent. */
    private const val NOT_CONNECTED = "Not connected to T3."

    fun edit(threadId: String, text: String) {
        drafts = drafts + (threadId to text)
    }

    fun send(work: CoroutineScope, connection: T3Connection, threadId: String) {
        val draft = drafts[threadId].orEmpty()
        val text = draft.trim()
        if (text.isEmpty() || threadId in sending) return
        sending = sending + threadId
        failures = failures - threadId
        work.launch {
            try {
                connection.send(threadId, text)
                if (drafts[threadId] == draft) drafts = drafts - threadId
                Log.i(TAG, "reply sent to $threadId")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val reason = T3Connection.describe(e)
                failures = failures + (threadId to failure(reason))
                Log.i(TAG, "reply to $threadId failed: $reason")
            } finally {
                sending = sending - threadId
            }
        }
    }
}

private const val TAG = "T3Thread"

/** What one read of the thread column is for: the thread and the updatedAt its shell row had. */
internal data class ReadKey(val threadId: String, val updatedAt: String)

/** The thread the column shows, and whether the connection is live. */
internal data class Shown(val key: ReadKey, val live: Boolean)

/**
 * The reads the thread column makes, from what it shows: one per thread and updatedAt, made only
 * while T3 is live, one at a time, and a change during a read gives one more read after it
 * (conflate keeps only the latest). A read waits for the connection, since without a socket it
 * fails at once; and the thread is read again when the connection comes back, because the
 * reconnect's snapshot carries the same updatedAt and a read that failed while it was down would
 * otherwise stay the column's last word (finding lor-wc8z).
 */
internal fun threadReads(shown: Flow<Shown>): Flow<ReadKey> =
    shown.map { if (it.live) it.key else null }.distinctUntilChanged().filterNotNull().conflate()

/** One read of a thread's end: its last messages, or why they couldn't be read. */
private class ThreadRead(val threadId: String, val messages: List<ThreadMessage>?, val error: String?)

/** Reads [threadId]'s projection and decodes its last messages; logs one `T3Thread` line per read. */
private suspend fun readThread(connection: T3Connection, threadId: String, updatedAt: String, last: ThreadRead?): ThreadRead {
    val started = SystemClock.elapsedRealtime()
    return try {
        val reply = connection.call(Commands.PROJECTION, JSONObject().put("threadId", threadId))
        val (messages, size) = withContext(Dispatchers.Default) { Commands.messages(reply) to reply.toString().length }
        Log.i(TAG, "read $threadId (updatedAt $updatedAt): ${messages.size} messages, reply $size chars, ${SystemClock.elapsedRealtime() - started} ms")
        ThreadRead(threadId, messages, null)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.i(TAG, "read $threadId (updatedAt $updatedAt) failed: ${T3Connection.describe(e)}")
        // The last good messages stay, with the reason.
        ThreadRead(threadId, last?.takeIf { it.threadId == threadId }?.messages, "Can't read the thread: ${T3Connection.describe(e)}")
    }
}

/** The Threads section of the wide form's left column: the [others], each choosing its thread. */
@Composable
internal fun Threads(others: List<T3Thread>, chosen: String?, choose: (T3Thread) -> Unit) {
    if (others.isEmpty()) return
    SectionHeader("Threads", others.size.toString())
    others.forEach { t -> key(t.id) { ThreadRow(t, t.id == chosen) { choose(t) } } }
}

/** A thread in the wide form's left column: a tap shows it on the right; the [selected] one carries the selection. */
@Composable
internal fun ThreadRow(thread: T3Thread, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 10.dp, vertical = 1.dp)
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (selected) Modifier.selection() else Modifier)
            .clickable(onClickLabel = "Show the thread", onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).background(stateColor(thread.state), CircleShape))
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(thread.title, style = Body.copy(fontSize = 13.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${thread.projectTitle} · ${thread.state.label}", style = Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(at(thread.updatedAt), style = Small, modifier = Modifier.padding(start = 8.dp))
    }
}

private fun stateColor(state: ThreadState): Color = when (state) {
    ThreadState.RUNNING -> Cy
    ThreadState.FAILED -> Bad
    ThreadState.WAITING_APPROVAL, ThreadState.WAITING_ANSWER -> Or
    ThreadState.IDLE -> Dim
}

private val DayMonth = DateTimeFormatter.ofPattern("d MMM")

/** An ISO time as the clock today, else the date. */
private fun at(iso: String): String {
    val zone = ZoneId.systemDefault()
    val time = runCatching { OffsetDateTime.parse(iso).toInstant().atZone(zone) }.getOrNull() ?: return ""
    return if (time.toLocalDate() == Instant.now().atZone(zone).toLocalDate()) time.format(Clock) else time.format(DayMonth)
}

/**
 * The wide form's right column: [thread]'s title, project and state, its last messages, what it
 * waits on ([card], with its actions), and the reply box at the bottom. Nothing to show when T3
 * lists no thread.
 */
@Composable
internal fun ThreadColumn(model: AgentModel, thread: T3Thread?, card: T3Card?, modifier: Modifier) {
    val status by model.connection.status.collectAsState()
    val live = status == T3Connection.Status.Live
    Column(modifier) {
        if (thread == null) {
            SectionHeader("Thread")
            Unseen(if (live) "T3 has no threads yet. Quick send starts one." else "The chosen thread shows here once T3 is connected.")
            return@Column
        }
        // The read, while T3 is live, and again whenever the thread, its updatedAt or the connection
        // changes ([threadReads]).
        var read by remember { mutableStateOf<ThreadRead?>(null) }
        val shown by rememberUpdatedState(Shown(ReadKey(thread.id, thread.updatedAt), live))
        LaunchedEffect(Unit) {
            threadReads(snapshotFlow { shown }).collect { k -> read = readThread(model.connection, k.threadId, k.updatedAt, read) }
        }
        ThreadHead(model, thread)
        val scroll = rememberScrollState()
        // Anchored at its end (reverse scrolling), so the newest message stays in view when the
        // column shrinks for the keyboard or a message arrives.
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .scrollsInGrid { way -> listTakes(way, scroll.canScrollForward, scroll.canScrollBackward, reversed = true) }
                .verticalScroll(scroll, reverseScrolling = true)
                .padding(bottom = 8.dp),
        ) {
            val current = read?.takeIf { it.threadId == thread.id }
            val messages = current?.messages
            when {
                messages == null && current?.error == null -> Unseen("Reading the thread…")
                messages != null && messages.isEmpty() -> Unseen("No messages yet.")
            }
            messages?.forEach { m -> key(m.id) { Message(m) } }
            current?.error?.let { Failed(it, Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) }
            if (card != null) {
                SectionHeader("Waits on you")
                T3QueueCard(card, model, live, expanded = true)
            }
        }
        ReplyBox(model, thread)
    }
}

@Composable
private fun ThreadHead(model: AgentModel, thread: T3Thread) {
    Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 10.dp, top = 12.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(thread.title, style = Body.copy(fontSize = 17.sp, lineHeight = 22.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                "${thread.projectTitle} · ${thread.state.label}",
                style = Small.copy(color = stateColor(thread.state).takeUnless { thread.state == ThreadState.IDLE } ?: Small.color),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Pill("Open in T3") { model.openThread(thread.id, LaunchFrom.AGENT_ICON) }
    }
}

/** One message: who wrote it and when, then its text as written (Markdown marks show as they are). */
@Composable
private fun Message(m: ThreadMessage) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp)) {
        Text(
            buildString {
                append(if (m.fromUser) "YOU" else "AGENT")
                at(m.createdAt).takeIf { it.isNotEmpty() }?.let { append(" · "); append(it) }
                if (m.streaming) append(" · WRITING")
            },
            style = Type.label.copy(color = if (m.fromUser) Txt.copy(alpha = 0.7f) else Cy),
        )
        // Trailing blank lines, which a provider's message often ends with, would leave a gap.
        Text(m.text.trimEnd(), style = Body.copy(lineHeight = 20.sp), modifier = Modifier.padding(top = 2.dp))
    }
}

/**
 * The reply box: sends to [thread] with [T3Connection.send] (`message.dispatch`), in the process's
 * scope ([Replies]). While a send runs it can't send again; a failure shows under it and the text stays.
 */
@Composable
private fun ReplyBox(model: AgentModel, thread: T3Thread) {
    val work = (LocalContext.current.applicationContext as LorApp).scope
    val draft = Replies.drafts[thread.id].orEmpty()
    val sending = thread.id in Replies.sending
    Column(Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Field(draft, "Reply to this thread", Modifier.weight(1f).heightIn(max = 160.dp)) { Replies.edit(thread.id, it) }
            Spacer(Modifier.width(6.dp))
            Pill(if (sending) "Sending…" else "Send", primary = true, enabled = !sending && draft.isNotBlank()) {
                Replies.send(work, model.connection, thread.id)
            }
        }
        Replies.failures[thread.id]?.let { Failed(it, Modifier.padding(start = 8.dp, top = 4.dp)) }
    }
}
