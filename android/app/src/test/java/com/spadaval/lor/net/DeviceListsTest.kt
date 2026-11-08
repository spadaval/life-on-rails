package com.spadaval.lor.net

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Replies recorded from the server's `pnpm test:db` fixtures in `tests/db/device.test.ts` (lor-lz97):
 * `lor/home-queue.json` (the agent's queue and two flags, one a Google task), `lor/tasks.json`
 * (Due soon's five open tasks and one done today) and `lor/people.json` (one past the cadence, one birthday).
 */
class DeviceListsTest {
    private fun json(name: String) = JSONObject(javaClass.classLoader!!.getResource("lor/$name")!!.readText())

    @Test
    fun readsTheAgentsQueueAndAFlagsGoogle() {
        val home = DeviceHome.parse(json("home-queue.json"))
        assertEquals(listOf("Renew the passport", "Find a plumber", "Draft the lease email"), home.agentQueue.map { it.title })
        assertEquals(Instant.parse("2026-10-14T10:00:00Z"), home.agentQueue[0].queuedAt)
        assertNull(home.agentQueue[0].thread)
        assertNull(home.agentQueue[2].queuedAt)
        assertEquals("thread-42", home.agentQueue[2].thread)
        assertEquals(listOf(false, true), home.needsYou.map { it.google })
        assertNull(home.needsYou[1].note)
    }

    @Test
    fun anOlderHomeWithoutTheQueueHasNone() {
        val o = json("home-queue.json")
        o.remove("agent_queue")
        assertEquals(emptyList<DeviceHome.AgentTask>(), DeviceHome.parse(o).agentQueue)
    }

    @Test
    fun readsTheTasks() {
        val t = DeviceTasks.parse(json("tasks.json"))
        assertEquals(ZoneId.of("America/Los_Angeles"), t.zone)
        assertEquals(LocalDate.parse("2026-10-14"), t.today)
        assertEquals(5, t.open)
        assertEquals(listOf("Pay rent", "Hear back on the lease", "Order boxes", "Pack books", "Undated"), t.tasks.map { it.title })
        val rent = t.tasks[0]
        assertEquals(LocalDate.parse("2026-10-12"), rent.due)
        assertTrue(rent.overdue && rent.flagged && rent.agent)
        assertFalse(rent.google)
        assertEquals("Priya", t.tasks[1].waitingOn)
        assertEquals("House move", t.tasks[3].parent)
        assertNull(t.tasks[4].due)
        assertNull(t.tasks[4].parent)
        assertEquals(listOf("Post the notice"), t.doneToday.map { it.title })
    }

    @Test
    fun readsThePeople() {
        val p = DevicePeople.parse(json("people.json"))
        val priya = p.overdue.single()
        assertEquals("Priya", priya.name)
        assertEquals("person", priya.kind)
        assertEquals("phone", priya.lastVia)
        assertEquals(10, priya.overdueBy)
        assertEquals(30, priya.keepInTouchDays)
        assertNull(priya.birthdayIn)
        assertTrue(priya.lastAt != null)
        val sam = p.birthdays.single()
        assertNull(sam.lastAt)
        assertNull(sam.lastVia)
        assertEquals(5, sam.birthdayIn)
        assertEquals(36, sam.turning)
    }
}
