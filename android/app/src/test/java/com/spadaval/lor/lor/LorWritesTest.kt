package com.spadaval.lor.lor

import com.spadaval.lor.net.DeviceHome
import com.spadaval.lor.net.DevicePeople
import com.spadaval.lor.net.DeviceState
import com.spadaval.lor.net.DeviceTask
import com.spadaval.lor.net.DeviceTasks
import com.spadaval.lor.net.LorReads
import com.spadaval.lor.net.TaskAction
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** The panels' writes (lor-rz50): held over the reader's replies until a read has them, put back when they fail. */
@OptIn(ExperimentalCoroutinesApi::class)
class LorWritesTest {
    private fun json(name: String) = JSONObject(javaClass.classLoader!!.getResource("lor/$name")!!.readText())
    private val home = DeviceHome.parse(json("home-queue.json"))
    private val tasks = DeviceTasks.parse(json("tasks.json"))
    private val people = DevicePeople.parse(json("people.json"))
    private val mum = home.needsYou[0]
    private val passport = home.agentQueue[0]
    private val lease = home.agentQueue[2]
    private val boxes = tasks.tasks[2]
    private val now = Instant.parse("2026-10-14T18:00:00Z")

    private fun held(action: TaskAction, title: String, before: LorWrites.Held? = null, thread: String? = null) =
        LorWrites.Held.predict(action, title, now, thread, before)

    @Test
    fun doneAndSeenTakeAFlagOutOfNeedsYou() {
        val done = LorWrites.Held.home(home, mapOf(mum.id to held(TaskAction.DONE, mum.title)))
        assertEquals(listOf("Sign the form"), done.needsYou.map { it.title })
        val seen = LorWrites.Held.home(home, mapOf(mum.id to held(TaskAction.SEEN, mum.title)))
        assertEquals(listOf("Sign the form"), seen.needsYou.map { it.title })
        // Undo of Done reopens the task without its flag: it stays out of Needs you.
        val undone = LorWrites.Held.home(home, mapOf(mum.id to held(TaskAction.REOPEN, mum.title, before = held(TaskAction.DONE, mum.title))))
        assertEquals(listOf("Sign the form"), undone.needsYou.map { it.title })
    }

    /** A task given to the agent joins its queue after those already queued, before the one a run is working. */
    @Test
    fun aTaskGivenToTheAgentJoinsAfterTheQueued() {
        val q = LorWrites.Held.home(home, mapOf(boxes.id to held(TaskAction.QUEUE, boxes.title)))
        assertEquals(listOf("Renew the passport", "Find a plumber", "Order boxes", "Draft the lease email"), q.agentQueue.map { it.title })
        assertEquals(now, q.agentQueue[2].queuedAt)
        assertNull(q.agentQueue[2].thread)
        // Taken back, and done: out of the queue.
        val back = LorWrites.Held.home(home, mapOf(passport.id to held(TaskAction.UNQUEUE, passport.title), lease.id to held(TaskAction.DONE, lease.title)))
        assertEquals(listOf("Find a plumber"), back.agentQueue.map { it.title })
        // The Undo of a Take back keeps the task where it was.
        val again = LorWrites.Held.home(home, mapOf(passport.id to held(TaskAction.QUEUE, passport.title, before = held(TaskAction.UNQUEUE, passport.title))))
        assertEquals(home.agentQueue.map { it.title }, again.agentQueue.map { it.title })
    }

    /** A hold gives way only to a reply whose read began after the write ended: reads are numbered as they begin. */
    @Test
    fun aHoldGivesWayToALaterRead() {
        val ended = held(TaskAction.DONE, mum.title).copy(endedAt = 7)
        assertTrue(held(TaskAction.DONE, mum.title).over(100))
        assertTrue(ended.over(null))
        assertTrue(ended.over(6))
        assertTrue(ended.over(7))
        assertFalse(ended.over(8))
    }

    @Test
    fun seenUnflagsATaskInTheLists() {
        val rent = tasks.tasks[0]
        assertTrue(rent.flagged)
        val t = LorWrites.Held.tasks(tasks, mapOf(rent.id to held(TaskAction.SEEN, rent.title)))
        assertFalse(t.tasks[0].flagged)
        assertEquals(tasks.tasks.drop(1), t.tasks.drop(1))
    }

    /** A reader whose replies are [home] and [tasks], on virtual time; each read is logged. */
    private fun TestScope.reader(log: MutableList<String>, homeNow: () -> DeviceHome = { home }) = LorReads(
        backgroundScope,
        fetchHome = { homeNow() },
        fetchTasks = { tasks },
        fetchPeople = { people },
        describe = { it.message ?: "?" },
        clock = { testScheduler.currentTime },
        log = { log += "${testScheduler.currentTime} ${it.substringBefore(":")}" },
    )

    private fun TestScope.writes(r: LorReads, send: suspend (String, TaskAction) -> DeviceTask, timer: suspend (String, String?) -> DeviceState = { _, _ -> error("no timer") }) =
        LorWrites(backgroundScope, r, send, timer, describe = { it.message ?: "?" }, log = {})

    private fun reply(id: String, title: String, done: Boolean = false, agent: Boolean = false, flagged: Boolean = false) =
        DeviceTask(id, title, done, agent, null, flagged)

    /**
     * A tick shows at once, holds through a read that began before the write ended, reads again
     * when it ends, and gives way to the reply that has it. Its Undo sends the inverse write.
     */
    @Test
    fun aWriteShowsAtOnceAndGivesWayToTheNextRead() = runTest {
        val log = mutableListOf<String>()
        val r = reader(log)
        val holder = launch { r.hold(lists = true) }
        runCurrent()
        val sent = mutableListOf<String>()
        val answer = CompletableDeferred<Unit>()
        val w = writes(r, { id, a -> sent += "${a.wire} $id"; answer.await(); reply(id, boxes.title, done = a == TaskAction.DONE) })

        w.task(boxes.id, boxes.title, TaskAction.DONE, say = "Done: Order boxes", undo = TaskAction.REOPEN)
        assertTrue(w.done(boxes.id, wasDone = false))
        assertTrue(boxes.id in w.busy)
        assertEquals("Done: Order boxes", w.bar?.text)
        // A second tap while it is under way sends nothing.
        w.task(boxes.id, boxes.title, TaskAction.REOPEN)
        runCurrent()
        assertEquals(listOf("done ${boxes.id}"), sent)

        advanceTimeBy(1_000)
        log.clear()
        answer.complete(Unit)
        runCurrent()
        assertFalse(boxes.id in w.busy)
        // Read again at once: home and the lists.
        assertEquals(listOf("1000 read /device/home", "1000 read /device/tasks", "1000 read /device/people"), log)
        // The reply read after the write is the server's word: this fixture still has it open.
        assertFalse(w.done(boxes.id, wasDone = false))

        // Undo sends reopen.
        w.bar!!.undo!!.invoke()
        runCurrent()
        assertEquals(listOf("done ${boxes.id}", "reopen ${boxes.id}"), sent)
        holder.cancel()
    }

    /** A write that fails puts the row back as it was, says why under it, and takes back its bar. */
    @Test
    fun aFailedWritePutsTheRowBack() = runTest {
        val r = reader(mutableListOf())
        val holder = launch { r.hold() }
        runCurrent()
        val w = writes(r, { _, _ -> throw java.io.IOException("409: this task lives in Google Tasks") })
        w.task(mum.id, mum.title, TaskAction.DONE, say = "Done: Call mum", undo = TaskAction.REOPEN)
        assertEquals(listOf("Sign the form"), w.home(r.home)!!.needsYou.map { it.title })
        runCurrent()
        assertEquals(listOf("Call mum", "Sign the form"), w.home(r.home)!!.needsYou.map { it.title })
        assertEquals("409: this task lives in Google Tasks", w.failures[mum.id])
        assertNull(w.bar)
        assertTrue(w.busy.isEmpty())
        holder.cancel()
    }

    /** An Undo whose write fails leaves the row as the first write left it. */
    @Test
    fun aFailedUndoKeepsTheFirstWrite() = runTest {
        val r = reader(mutableListOf())
        val holder = launch { r.hold() }
        runCurrent()
        var fail = false
        // The server keeps answering with the old reply, so the first write's reply holds.
        val w = writes(r, { id, a -> if (fail) error("LoR didn't answer.") else reply(id, passport.title, agent = a == TaskAction.QUEUE) })
        // Taken back; the reader's next read still has the task queued, as if it began before the write ended.
        w.task(passport.id, passport.title, TaskAction.UNQUEUE, say = "Taken back", undo = TaskAction.QUEUE)
        runCurrent()
        fail = true
        val held = w.held[passport.id]
        w.task(passport.id, passport.title, TaskAction.QUEUE)
        runCurrent()
        assertEquals(held, w.held[passport.id])
        assertEquals("LoR didn't answer.", w.failures[passport.id])
        holder.cancel()
    }

    /** The timer shows what its reply says is running until a read of `/device/home` begun after it ends. */
    @Test
    fun theTimerShowsItsReply() = runTest {
        var slow = 0L
        val r = LorReads(
            backgroundScope, fetchHome = { delay(slow); home }, fetchTasks = { tasks }, fetchPeople = { people },
            describe = { it.message ?: "?" }, clock = { testScheduler.currentTime }, log = {},
        )
        val holder = launch { r.hold() }
        runCurrent()
        assertNull(r.home!!.day.running)
        val paused = DeviceState(DeviceState.Running("Write", null, now, paused = true), emptyList())
        val w = writes(r, { _, _ -> error("no task") }, { _, _ -> paused })
        slow = 5_000
        w.timer("pause")
        assertTrue(LorWrites.TIMER in w.busy)
        runCurrent()
        assertFalse(LorWrites.TIMER in w.busy)
        // The reply shows while the read after it is under way.
        assertEquals(DeviceHome.Running("Write", now, paused = true), w.home(r.home)!!.day.running)
        advanceTimeBy(5_001)
        // That read has ended: home is the server's word (this fixture has no timer).
        assertNull(w.home(r.home)!!.day.running)
        holder.cancel()
    }
}
