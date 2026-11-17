package com.spadaval.lor.agent

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
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spadaval.lor.t3.T3Thread
import com.spadaval.lor.t3.ThreadMessage
import com.spadaval.lor.t3.ThreadState
import com.spadaval.lor.ui.Bad
import com.spadaval.lor.ui.BodyText
import com.spadaval.lor.ui.Cy
import com.spadaval.lor.ui.Dim
import com.spadaval.lor.ui.Failed
import com.spadaval.lor.ui.Field
import com.spadaval.lor.ui.LocalClock
import com.spadaval.lor.ui.Or
import com.spadaval.lor.ui.Pill
import com.spadaval.lor.ui.SectionHeader
import com.spadaval.lor.ui.SmallText
import com.spadaval.lor.ui.Txt
import com.spadaval.lor.ui.Type
import com.spadaval.lor.ui.Unseen
import com.spadaval.lor.ui.selection

/*
 * The T3 panel's wide form (lor-iabt): on the inner screen the agent page's column sits on the left
 * with a Threads section under it, and the chosen thread on the right, with its last messages, what
 * it waits on, and a reply box. The app reads the thread ([PanelActions.readThread]) when it is
 * shown and again when its shell row's updatedAt changes (decision note on lor-iabt); messages are
 * plain text.
 */

/** Which thread the wide form shows, and which threads its Threads section lists. Pure. */
object ThreadChoice {
    const val MAX_OTHERS = 20

    /** The thread to show: [chosen] while it is listed, else the first T3 card's, else the most recently updated. */
    fun shown(threads: List<T3Thread>, cards: List<T3Card>, chosen: String?): T3Thread? =
        threads.firstOrNull { it.id == chosen } ?: cards.firstOrNull()?.thread ?: threads.firstOrNull()

    /** The listed threads not already in Running or Needs you, most recently updated first, at most [MAX_OTHERS]. */
    fun others(threads: List<T3Thread>, cards: List<T3Card>): List<T3Thread> {
        val above = cards.map { it.thread.id }.toSet() + AgentQueue.running(threads).map { it.id }
        return threads.filter { it.id !in above }.take(MAX_OTHERS)
    }
}

/** The Threads section of the wide form's left column: the [others], each choosing its thread. */
@Composable
internal fun Threads(others: List<T3Thread>, chosen: String?, lor: LorUi, choose: (T3Thread) -> Unit) {
    if (others.isEmpty()) return
    SectionHeader("Threads", others.size.toString())
    others.forEach { t -> key(t.id) { ThreadRow(t, t.id == chosen, lor) { choose(t) } } }
}

/** A thread in the wide form's left column: a tap shows it on the right; the [selected] one carries the selection. */
@Composable
internal fun ThreadRow(thread: T3Thread, selected: Boolean, lor: LorUi, onClick: () -> Unit) {
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
            Text(thread.title, style = BodyText.copy(fontSize = 13.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${thread.projectTitle} · ${thread.state.label}", style = SmallText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(at(thread.updatedAt, lor), style = SmallText, modifier = Modifier.padding(start = 8.dp))
    }
}

private fun stateColor(state: ThreadState): Color = when (state) {
    ThreadState.RUNNING -> Cy
    ThreadState.FAILED -> Bad
    ThreadState.WAITING_APPROVAL, ThreadState.WAITING_ANSWER -> Or
    ThreadState.IDLE -> Dim
}

/** An ISO time as the clock today, else the date, by the phone's clock and zone. */
private fun at(iso: String, lor: LorUi): String {
    val time = LocalClock.parse(iso) ?: return ""
    return if (LocalClock.date(time, lor.zone) == LocalClock.date(lor.now, lor.zone)) LocalClock.time(time, lor.zone) else LocalClock.dayMonth(time, lor.zone)
}

/**
 * The wide form's right column: [thread]'s title, project and state, its last messages, what it
 * waits on ([card], with its actions), and the reply box at the bottom. Nothing to show when T3
 * lists no thread.
 */
@Composable
internal fun ThreadColumn(ui: PanelUi, on: PanelActions, thread: T3Thread?, card: T3Card?, modifier: Modifier, grid: InGrid) {
    val live = ui.t3.live
    Column(modifier) {
        if (thread == null) {
            SectionHeader("Thread")
            Unseen(if (live) "T3 has no threads yet. Quick send starts one." else "The chosen thread shows here once T3 is connected.")
            return@Column
        }
        // The read, while T3 is live, and again whenever the thread, its updatedAt or the connection changes.
        val read = on.readThread(thread, live)
        ThreadHead(on, thread)
        val scroll = rememberScrollState()
        // Anchored at its end (reverse scrolling), so the newest message stays in view when the
        // column shrinks for the keyboard or a message arrives.
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(grid.scrolls(scroll, true))
                .verticalScroll(scroll, reverseScrolling = true)
                .padding(bottom = 8.dp),
        ) {
            val current = read?.takeIf { it.threadId == thread.id }
            val messages = current?.messages
            when {
                messages == null && current?.error == null -> Unseen("Reading the thread…")
                messages != null && messages.isEmpty() -> Unseen("No messages yet.")
            }
            messages?.forEach { m -> key(m.id) { Message(m, ui.lor) } }
            current?.error?.let { Failed(it, Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) }
            if (card != null) {
                SectionHeader("Waits on you")
                T3QueueCard(card, ui, on, expanded = true)
            }
        }
        ReplyBox(ui.replies, on, thread)
    }
}

@Composable
private fun ThreadHead(on: PanelActions, thread: T3Thread) {
    Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 10.dp, top = 12.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(thread.title, style = BodyText.copy(fontSize = 17.sp, lineHeight = 22.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                "${thread.projectTitle} · ${thread.state.label}",
                style = SmallText.copy(color = stateColor(thread.state).takeUnless { thread.state == ThreadState.IDLE } ?: SmallText.color),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Pill("Open in T3") { on.openThread(thread.id, Tap.ICON) }
    }
}

/** One message: who wrote it and when, then its text as written (Markdown marks show as they are). */
@Composable
private fun Message(m: ThreadMessage, lor: LorUi) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp)) {
        Text(
            buildString {
                append(if (m.fromUser) "YOU" else "AGENT")
                at(m.createdAt, lor).takeIf { it.isNotEmpty() }?.let { append(" · "); append(it) }
                if (m.streaming) append(" · WRITING")
            },
            style = Type.label.copy(color = if (m.fromUser) Txt.copy(alpha = 0.7f) else Cy),
        )
        // Trailing blank lines, which a provider's message often ends with, would leave a gap.
        Text(m.text.trimEnd(), style = BodyText.copy(lineHeight = 20.sp), modifier = Modifier.padding(top = 2.dp))
    }
}

/** The reply box: sends to [thread]; while a send runs it can't send again; a failure shows under it and the text stays. */
@Composable
private fun ReplyBox(replies: RepliesUi, on: PanelActions, thread: T3Thread) {
    val draft = replies.drafts[thread.id].orEmpty()
    val sending = thread.id in replies.sending
    Column(Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Field(draft, "Reply to this thread", Modifier.weight(1f).heightIn(max = 160.dp)) { on.editReply(thread.id, it) }
            Spacer(Modifier.width(6.dp))
            Pill(if (sending) "Sending…" else "Send", primary = true, enabled = !sending && draft.isNotBlank()) { on.sendReply(thread.id) }
        }
        replies.failures[thread.id]?.let { Failed(it, Modifier.padding(start = 8.dp, top = 4.dp)) }
    }
}
