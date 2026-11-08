package com.spadaval.lor.net

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The process's LoR reader (lor-leh4, lor-lz97): when the next read is due, and which. */
@OptIn(ExperimentalCoroutinesApi::class)
class LorReadsTest {
    @Test
    fun dueAtOnceWhenNeverRead() = assertEquals(0L, LorReads.dueIn(null, 5_000L))

    @Test
    fun dueAMinuteAfterTheLastBegan() {
        assertEquals(40_000L, LorReads.dueIn(100_000L, 120_000L))
        assertEquals(0L, LorReads.dueIn(100_000L, 160_000L))
        assertEquals(0L, LorReads.dueIn(100_000L, 500_000L))
    }

    @Test
    fun neverMoreThanAMinuteAway() = assertEquals(60_000L, LorReads.dueIn(100_000L, 90_000L))

    /** `/device/home` alone, then with the lists a panel asks for: each due on its own minute. */
    @Test
    fun plansEachReadOnItsOwnMinute() {
        assertEquals(listOf(0) to 0L, LorReads.plan(listOf(null), 0L))
        assertEquals(emptyList<Int>() to 30_000L, LorReads.plan(listOf(100_000L), 130_000L))
        // A panel comes 30 s after home's read: the lists are read at once, home at its minute.
        assertEquals(listOf(1, 2) to 0L, LorReads.plan(listOf(100_000L, null, null), 130_000L))
        assertEquals(emptyList<Int>() to 29_900L, LorReads.plan(listOf(100_000L, 130_000L, 130_100L), 130_100L))
        assertEquals(listOf(0) to 0L, LorReads.plan(listOf(100_000L, 130_000L, 130_100L), 160_000L))
        // The lists kept from a minute ago are read again with home.
        assertEquals(listOf(0, 1, 2) to 0L, LorReads.plan(listOf(100_000L, 90_000L, 95_000L), 200_000L))
    }

    private fun json(name: String) = JSONObject(javaClass.classLoader!!.getResource("lor/$name")!!.readText())
    private val home = DeviceHome.parse(json("home-queue.json"))
    private val tasks = DeviceTasks.parse(json("tasks.json"))
    private val people = DevicePeople.parse(json("people.json"))

    /** A reader on [TestScope]'s virtual time whose reads are logged as "<second> <path>"; [failHome] makes `/device/home` fail. */
    private fun TestScope.reader(log: MutableList<String>, failHome: () -> Boolean = { false }) = LorReads(
        backgroundScope,
        fetchHome = { if (failHome()) error("LoR is down") else home },
        fetchTasks = { tasks },
        fetchPeople = { people },
        describe = { it.message ?: "?" },
        clock = { testScheduler.currentTime },
        log = { log += "${testScheduler.currentTime / 1000} ${it.substringBefore(":")}" },
    )

    /**
     * The agent page and a `today` block hold it, then a `lor` panel comes and goes: one
     * `/device/home` read a minute throughout, and one `/device/tasks` and `/device/people` read a
     * minute only while the panel holds it, the first at once (decision lor-tbqc, lor-lz97).
     */
    @Test
    fun oneReadOfEachAMinuteWhateverHoldsIt() = runTest {
        val log = mutableListOf<String>()
        val r = reader(log)
        val page = launch { r.hold() }
        val block = launch { r.hold() }
        runCurrent()
        assertEquals(listOf("0 read /device/home"), log)
        assertTrue(r.current)
        assertEquals(home, r.home)

        advanceTimeBy(30_000)
        val panel = launch { r.hold(lists = true) }
        advanceTimeBy(95_000)
        runCurrent()
        assertEquals(
            listOf(
                "0 read /device/home", "30 read /device/tasks", "30 read /device/people",
                "60 read /device/home", "90 read /device/tasks", "90 read /device/people", "120 read /device/home",
            ),
            log,
        )
        assertEquals(tasks, r.tasks.value)
        assertEquals(people, r.people.value)

        // The panel goes: the lists stop, /device/home keeps its minute.
        log.clear()
        panel.cancel()
        advanceTimeBy(120_000)
        runCurrent()
        assertEquals(listOf("180 read /device/home", "240 read /device/home"), log)

        // Nothing holds it: no reads, and what was read is kept.
        log.clear()
        page.cancel()
        block.cancel()
        advanceTimeBy(300_000)
        runCurrent()
        assertEquals(emptyList<String>(), log)
        assertEquals(tasks, r.tasks.value)

        // A panel alone, minutes later: everything a minute old is read at once.
        val again = launch { r.hold(lists = true) }
        runCurrent()
        assertEquals(listOf("545 read /device/home", "545 read /device/tasks", "545 read /device/people"), log)
        again.cancel()
    }

    /** A failed read keeps the last good reply, says why, and the next good one clears it (launcher polish 3.3). */
    @Test
    fun aFailedReadKeepsTheLastGoodReply() = runTest {
        val log = mutableListOf<String>()
        var down = false
        val r = reader(log) { down }
        val holder = launch { r.hold() }
        runCurrent()
        down = true
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(home, r.home)
        assertEquals("LoR is down", r.error)
        assertEquals("60 read /device/home", log.last())
        down = false
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(null, r.error)
        assertNotNull(r.homeRead.at)
        holder.cancel()
        assertFalse(log.isEmpty())
    }
}
