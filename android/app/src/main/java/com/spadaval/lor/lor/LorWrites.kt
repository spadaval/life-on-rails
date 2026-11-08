package com.spadaval.lor.lor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.spadaval.lor.home.UndoMessage
import com.spadaval.lor.net.DeviceHome
import com.spadaval.lor.net.DeviceState
import com.spadaval.lor.net.DeviceTask
import com.spadaval.lor.net.DeviceTasks
import com.spadaval.lor.net.LorReads
import com.spadaval.lor.net.TaskAction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * The panels' writes to LoR (lor-rz50, decision lor-dofd), one for the process
 * ([com.spadaval.lor.LorApp.writes]): a task done or not, a flag Seen, a task given to the agent
 * or taken back (`POST /device/task`), and the timer (`POST /device/timer`).
 *
 * A write shows at once: the task's state as the write will leave it is [held] over the reader's
 * last replies, and each place that draws a task reads it through [home] and [tasks]. The hold
 * gives way to the server's own word reply by reply: once a read of that reply began after the
 * write ended ([LorReads.Kept.goodRead]), the reply already has it. On success the reader reads
 * again at once ([LorReads.refresh]); on failure the hold is taken back, so the row is as it was,
 * and [failures] says why under it.
 *
 * Each row takes one write at a time: its key (the task's id, or [TIMER]) is in [busy] while its
 * call is under way, and its controls are disabled. The calls run in the process's scope, so
 * leaving the page never cancels one (lor-ozj2's F4, as [com.spadaval.lor.agent.CardCalls]).
 * What a write says on home's Undo bar, with its Undo, is [bar]. Call everything on the main thread.
 */
class LorWrites(
    private val scope: CoroutineScope,
    private val reads: LorReads,
    private val send: suspend (id: String, action: TaskAction) -> DeviceTask,
    private val sendTimer: suspend (action: String, intentId: String?) -> DeviceState,
    private val describe: (Exception) -> String,
    private val log: (String) -> Unit,
) {
    /** The tasks written from this process, by id, as each write left (or will leave) them. */
    var held by mutableStateOf(emptyMap<String, Held>())
        private set
    /** What the timer's last write left running, until `/device/home` has it. */
    var timer by mutableStateOf<HeldTimer?>(null)
        private set
    /** Rows (a task's id, or [TIMER]) with a write under way. */
    var busy by mutableStateOf(emptySet<String>())
        private set
    /** Why a row's last write failed, by its key, in plain words. */
    var failures by mutableStateOf(emptyMap<String, String>())
        private set
    /** The last write's line for home's Undo bar; home shows each new one. */
    var bar by mutableStateOf<UndoMessage?>(null)
        private set

    /** `/device/home` with the writes it doesn't have yet. */
    fun home(h: DeviceHome?): DeviceHome? {
        h ?: return null
        val read = reads.homeRead.goodRead
        val tasks = held.filterValues { it.over(read) }
        val t = timer?.takeIf { it.over(read) }
        if (tasks.isEmpty() && t == null) return h
        return Held.home(h, tasks).let { if (t != null) it.copy(day = it.day.copy(running = t.running)) else it }
    }

    /** `/device/tasks` with the writes it doesn't have yet: flags as they are now; [done] says which rows are done. */
    fun tasks(t: DeviceTasks): DeviceTasks = Held.tasks(t, tasksHeld())

    /** Whether a row of `/device/tasks` is done: as the reply has it ([wasDone]), unless a write it doesn't have changed that. */
    fun done(id: String, wasDone: Boolean): Boolean = tasksHeld()[id]?.done ?: wasDone

    private fun tasksHeld(): Map<String, Held> {
        val read = reads.tasks.goodRead
        return held.filterValues { it.over(read) }
    }

    /**
     * Sends [action] for the task [id] ([title] for the bar) unless one is under way for it, holding
     * the state it will leave at once. [say] is the bar's line, with its Undo when [undo] isn't null:
     * the inverse write, sent once this one has ended well.
     */
    fun task(id: String, title: String, action: TaskAction, say: String? = null, undo: TaskAction? = null, thread: String? = null) {
        if (id in busy) return
        val before = held[id]
        val ended = CompletableDeferred<Boolean>()
        busy = busy + id
        failures = failures - id
        held = held + (id to Held.predict(action, title, Instant.now(), thread, before))
        val message = say?.let { text ->
            if (undo == null) UndoMessage(text, undoable = false)
            else UndoMessage(text) { scope.launch { if (ended.await()) task(id, title, undo) } }
        }
        if (message != null) bar = message
        scope.launch {
            try {
                val reply = send(id, action)
                held = held + (id to Held.of(reply, reads.begun, thread))
                log("${action.wire} $id: ok")
                ended.complete(true)
                reads.refresh()
            } catch (e: CancellationException) {
                ended.complete(false)
                throw e
            } catch (e: Exception) {
                held = if (before == null) held - id else held + (id to before)
                failures = failures + (id to describe(e))
                if (message != null && bar === message) bar = null
                log("${action.wire} $id: ${e.javaClass.simpleName}")
                ended.complete(false)
            } finally {
                busy = busy - id
            }
        }
    }

    /**
     * Sends the timer's [action] (`start` with [intentId], null for none; `pause`, `resume`, `stop`)
     * unless one is under way, and shows what the reply says is running. No Undo, as on the web.
     */
    fun timer(action: String, intentId: String? = null) {
        if (TIMER in busy) return
        busy = busy + TIMER
        failures = failures - TIMER
        scope.launch {
            try {
                val reply = sendTimer(action, intentId)
                timer = HeldTimer(reply.running?.let { DeviceHome.Running(it.name, it.start, it.paused) }, reads.begun)
                log("timer $action: ok")
                reads.refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failures = failures + (TIMER to describe(e))
                log("timer $action: ${e.javaClass.simpleName}")
            } finally {
                busy = busy - TIMER
            }
        }
    }

    /** A task as a write left it: [done], [flagged], [agent] (given to the agent) and [queued], null where the write says nothing of it. */
    data class Held(
        val title: String,
        val done: Boolean?,
        val flagged: Boolean?,
        val agent: Boolean?,
        val queuedAt: Instant?,
        /** Its T3 thread, kept when it stays with the agent. */
        val thread: String?,
        /** How many reads had begun ([LorReads.begun]) when the write ended; null while it is under way. */
        val endedAt: Long?,
    ) {
        /** Whether the reply that read number [goodRead] gave lacks this write: that read began before it ended. */
        fun over(goodRead: Long?): Boolean = endedAt == null || goodRead == null || goodRead <= endedAt

        companion object {
            /** The state [action] will leave, before the reply says. [before] is what an earlier write left. */
            fun predict(action: TaskAction, title: String, now: Instant, thread: String?, before: Held?): Held = when (action) {
                // completeTask clears the flag and takes the task out of the agent's queue.
                TaskAction.DONE -> Held(title, done = true, flagged = false, agent = before?.agent, queuedAt = null, thread = thread, endedAt = null)
                // reopenTask doesn't put back the flag Done cleared.
                TaskAction.REOPEN -> Held(title, done = false, flagged = false, agent = before?.agent, queuedAt = before?.queuedAt, thread = thread, endedAt = null)
                TaskAction.SEEN -> Held(title, done = before?.done, flagged = false, agent = before?.agent, queuedAt = before?.queuedAt, thread = thread, endedAt = null)
                TaskAction.QUEUE -> Held(title, done = false, flagged = before?.flagged, agent = true, queuedAt = now, thread = thread, endedAt = null)
                // assignTask(…, "owner") clears the thread and the queue.
                TaskAction.UNQUEUE -> Held(title, done = before?.done, flagged = before?.flagged, agent = false, queuedAt = null, thread = null, endedAt = null)
            }

            /** The state the server's reply says, the write having ended at [endedAt]. */
            fun of(reply: DeviceTask, endedAt: Long, thread: String?): Held =
                Held(reply.title, reply.done, reply.flagged, reply.agent, reply.queuedAt, if (reply.agent) thread else null, endedAt)

            /**
             * [h] with [held] over it: a task done or no longer flagged leaves Needs you; one done or
             * taken back leaves the agent's queue; one given to the agent joins it after the tasks
             * already queued (which run oldest first), before those a run is working.
             */
            fun home(h: DeviceHome, held: Map<String, Held>): DeviceHome {
                if (held.isEmpty()) return h
                val needs = h.needsYou.filterNot { n -> held[n.id]?.let { it.done == true || it.flagged == false } == true }
                val kept = h.agentQueue.filterNot { t -> held[t.id]?.let { it.done == true || it.agent == false } == true }
                val joined = held.filter { (id, x) -> x.agent == true && x.done != true && kept.none { it.id == id } }
                    .map { (id, x) -> DeviceHome.AgentTask(id, x.title, x.queuedAt, x.thread) }
                val at = kept.indexOfFirst { it.queuedAt == null }.let { if (it < 0) kept.size else it }
                val queue = kept.take(at) + joined.filter { it.queuedAt != null } + kept.drop(at) + joined.filter { it.queuedAt == null }
                return h.copy(needsYou = needs, agentQueue = queue)
            }

            /** [t]'s rows with the flag [held] says: Seen moves a task out of the Tasks tab's Needs you at once. */
            fun tasks(t: DeviceTasks, held: Map<String, Held>): DeviceTasks {
                if (held.isEmpty()) return t
                fun row(r: DeviceTasks.Task) = held[r.id]?.flagged?.let { r.copy(flagged = it) } ?: r
                return t.copy(tasks = t.tasks.map(::row), doneToday = t.doneToday.map(::row))
            }
        }
    }

    /** What the timer's last write left running (null for nothing), and how many reads had begun when it ended. */
    data class HeldTimer(val running: DeviceHome.Running?, val endedAt: Long) {
        fun over(goodRead: Long?): Boolean = goodRead == null || goodRead <= endedAt
    }

    companion object {
        /** The timer's key in [busy] and [failures]. */
        const val TIMER = "timer"
    }
}
