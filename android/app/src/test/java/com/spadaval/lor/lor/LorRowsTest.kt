package com.spadaval.lor.lor

import com.spadaval.lor.net.DeviceHome
import com.spadaval.lor.net.DevicePeople
import com.spadaval.lor.net.DeviceTasks
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/** What the `lor` panel and LoR's blocks write in their rows, and which rows they show (lor-lz97), against the web's wording. */
class LorRowsTest {
    private val today = LocalDate.parse("2026-10-14") // a Wednesday
    private val tasks = DeviceTasks.parse(JSONObject(javaClass.classLoader!!.getResource("lor/tasks.json")!!.readText()))

    private fun task(title: String, due: String?, flagged: Boolean = false) =
        DeviceTasks.Task(title, title, due?.let(LocalDate::parse), false, null, null, flagged, false, false)

    @Test
    fun aBlockShowsItsFirstRowsAndCountsTheRest() {
        assertEquals(listOf(1, 2, 3) to 2, LorRows.cap(listOf(1, 2, 3, 4, 5), 3))
        assertEquals(listOf(1, 2, 3) to 0, LorRows.cap(listOf(1, 2, 3), 3))
        assertEquals(listOf(1, 2) to 0, LorRows.cap(listOf(1, 2), 5))
        assertEquals(listOf(1, 2, 3, 4) to 0, LorRows.cap(listOf(1, 2, 3, 4), null))
    }

    @Test
    fun datesAsTheWebWritesThem() {
        assertEquals("15 Oct", LorRows.shortDate(LocalDate.parse("2026-10-15")))
        assertEquals("1 Sept", LorRows.shortDate(LocalDate.parse("2026-09-01")))
        assertEquals("Today", LorRows.dueLabel(today, today))
        assertEquals("Tomorrow", LorRows.dueLabel(today.plusDays(1), today))
        assertEquals("Yesterday", LorRows.dueLabel(today.minusDays(1), today))
        assertEquals("Fri", LorRows.dueLabel(today.plusDays(2), today))
        assertEquals("Tue", LorRows.dueLabel(today.plusDays(6), today))
        assertEquals("21 Oct", LorRows.dueLabel(today.plusDays(7), today))
        assertEquals("12 Oct", LorRows.dueLabel(today.minusDays(2), today))
    }

    @Test
    fun dueSoonsLineIsTheWebs() {
        assertEquals(listOf("due 12 Oct", "due 16 Oct · waiting on Priya", "due 20 Oct", "due 20 Oct · House move", ""), tasks.tasks.map(LorRows::dueSoonLine))
        assertEquals("Google Tasks", LorRows.dueSoonLine(task("g", null).copy(google = true)))
        assertEquals("done today · House move", LorRows.dueSoonLine(tasks.tasks[3], done = true))
    }

    /** As the web's Due soon: six open tasks, then today's done ones. */
    @Test
    fun dueSoonShowsSixOpenThenTheDoneToday() {
        val done = DeviceTasks.parse(JSONObject(javaClass.classLoader!!.getResource("lor/tasks-done.json")!!.readText()))
        assertEquals(listOf("Pack books" to false, "Post the notice" to true), LorRows.dueSoon(done).map { (t, d) -> t.title to d })
        val many = tasks.copy(tasks = List(8) { task("t$it", null) })
        assertEquals(List(6) { "t$it" }, LorRows.dueSoon(many).map { it.first.title })
    }

    @Test
    fun theTasksTabsLine() {
        assertEquals(listOf("12 Oct", "Fri · waiting on Priya", "Tue", "Tue · House move", ""), tasks.tasks.map { LorRows.taskLine(it, tasks.today) })
    }

    @Test
    fun theTasksTabGroupsAsTheWebsTasks() {
        val groups = LorRows.groups(tasks.tasks, tasks.today)
        assertEquals(listOf("Needs you", "This week", "No date"), groups.map { it.title })
        assertEquals(listOf("Pay rent"), groups[0].tasks.map { it.title })
        assertEquals(listOf("Hear back on the lease", "Order boxes", "Pack books"), groups[1].tasks.map { it.title })
        val all = LorRows.groups(
            listOf(task("past", "2026-10-13"), task("today", "2026-10-14"), task("week end", "2026-10-20"), task("later", "2026-10-21"), task("none", null)),
            today,
        )
        assertEquals(listOf("Past due", "Today", "This week", "Later on", "No date"), all.map { it.title })
        assertEquals(listOf(listOf("past"), listOf("today"), listOf("week end"), listOf("later"), listOf("none")), all.map { g -> g.tasks.map { it.title } })
        assertEquals(emptyList<LorRows.Group>(), LorRows.groups(emptyList(), today))
    }

    @Test
    fun agoAsTheWebsAgo() {
        val now = Instant.parse("2026-10-14T19:00:00Z")
        assertEquals("never", LorRows.ago(null, now))
        assertEquals("40s ago", LorRows.ago(now.minusSeconds(40), now))
        assertEquals("12m ago", LorRows.ago(now.minusSeconds(12 * 60), now))
        assertEquals("5h ago", LorRows.ago(now.minusSeconds(5 * 3600), now))
        assertEquals("40d ago", LorRows.ago(now.minusSeconds(40 * 86400), now))
    }

    @Test
    fun peoplesLinesAsTheWebsPeople() {
        val p = DevicePeople.parse(JSONObject(javaClass.classLoader!!.getResource("lor/people.json")!!.readText()))
        val priya = p.overdue.single()
        assertEquals("40d ago · call, text or WhatsApp", LorRows.lastTouch(priya, priya.lastAt!!.plusSeconds(40 * 86400)))
        assertEquals("10 days past the cadence", LorRows.overdueLine(priya))
        assertNull(LorRows.birthdayLine(priya))
        val sam = p.birthdays.single()
        assertEquals("Not seen yet", LorRows.lastTouch(sam, Instant.now()))
        assertEquals("in 5 days, turning 36", LorRows.birthdayLine(sam))
        assertEquals("today", LorRows.birthdayLine(sam.copy(birthdayIn = 0, turning = null)))
        assertNull(LorRows.overdueLine(sam))
    }

    @Test
    fun theAgentsQueueSaysWhereEachTaskIs() {
        assertEquals("queued for the next run", LorRows.agentLine(DeviceHome.AgentTask("a", "a", Instant.EPOCH, null)))
        assertEquals("in a T3 thread", LorRows.agentLine(DeviceHome.AgentTask("a", "a", null, "thread-42")))
        assertEquals("given to the agent", LorRows.agentLine(DeviceHome.AgentTask("a", "a", null, null)))
    }
}
