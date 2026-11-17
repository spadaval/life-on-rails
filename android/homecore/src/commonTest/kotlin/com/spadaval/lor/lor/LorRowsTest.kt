package com.spadaval.lor.lor

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** What the `lor` panel and LoR's blocks write in their rows, and which rows they show (lor-lz97), against the web's wording. */
class LorRowsTest {
    private val today = LocalDate.parse("2026-10-14") // a Wednesday
    private val tasks = LorReplies.tasks(read(TASKS))

    private fun task(title: String, due: String?, flagged: Boolean = false) =
        LorTasks.Task(title, title, due?.let(LocalDate::parse), false, null, null, flagged, false, false)

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
        assertEquals("Tomorrow", LorRows.dueLabel(today.plus(1, DateTimeUnit.DAY), today))
        assertEquals("Yesterday", LorRows.dueLabel(today.minus(1, DateTimeUnit.DAY), today))
        assertEquals("Fri", LorRows.dueLabel(today.plus(2, DateTimeUnit.DAY), today))
        assertEquals("Tue", LorRows.dueLabel(today.plus(6, DateTimeUnit.DAY), today))
        assertEquals("21 Oct", LorRows.dueLabel(today.plus(7, DateTimeUnit.DAY), today))
        assertEquals("12 Oct", LorRows.dueLabel(today.minus(2, DateTimeUnit.DAY), today))
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
        val done = LorReplies.tasks(read(TASKS_DONE))
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
        assertEquals("40s ago", LorRows.ago(now - 40.seconds, now))
        assertEquals("12m ago", LorRows.ago(now - 12.minutes, now))
        assertEquals("5h ago", LorRows.ago(now - 5.hours, now))
        assertEquals("40d ago", LorRows.ago(now - 40.days, now))
    }

    @Test
    fun peoplesLinesAsTheWebsPeople() {
        val p = LorReplies.people(read(PEOPLE))
        val priya = p.overdue.single()
        assertEquals("40d ago · call, text or WhatsApp", LorRows.lastTouch(priya, priya.lastAt!! + 40.days))
        assertEquals("10 days past the cadence", LorRows.overdueLine(priya))
        assertNull(LorRows.birthdayLine(priya))
        val sam = p.birthdays.single()
        assertEquals("Not seen yet", LorRows.lastTouch(sam, Clock.System.now()))
        assertEquals("in 5 days, turning 36", LorRows.birthdayLine(sam))
        assertEquals("today", LorRows.birthdayLine(sam.copy(birthdayIn = 0, turning = null)))
        assertNull(LorRows.overdueLine(sam))
    }

    @Test
    fun theAgentsQueueSaysWhereEachTaskIs() {
        assertEquals("queued for the next run", LorRows.agentLine(LorHome.AgentTask("a", "a", Instant.fromEpochSeconds(0), null)))
        assertEquals("in a T3 thread", LorRows.agentLine(LorHome.AgentTask("a", "a", null, "thread-42")))
        assertEquals("given to the agent", LorRows.agentLine(LorHome.AgentTask("a", "a", null, null)))
    }

    private fun read(text: String) = Json.parseToJsonElement(text).jsonObject

    /** The app's test replies (`app/src/test/resources/lor/`), which DeviceListsTest reads with the app's parser. */
    private val TASKS = """
{"now":"2026-10-14T19:00:00.000Z","timezone":"America/Los_Angeles","today":"2026-10-14","open":5,"tasks":[{"id":"01a1ba01-9913-7b6e-b631-344c5891162f","title":"Pay rent","due":"2026-10-12","overdue":true,"waiting_on":null,"parent":null,"flagged":true,"agent":true,"google":false},{"id":"01a1ba01-9916-73f1-9cc3-5889cf50c25c","title":"Hear back on the lease","due":"2026-10-16","overdue":false,"waiting_on":"Priya","parent":null,"flagged":false,"agent":false,"google":false},{"id":"01a1ba01-990e-736c-b5e5-44fcb7f5c912","title":"Order boxes","due":"2026-10-20","overdue":false,"waiting_on":null,"parent":null,"flagged":false,"agent":false,"google":false},{"id":"01a1ba01-990a-706b-9997-6edd78fefa2b","title":"Pack books","due":"2026-10-20","overdue":false,"waiting_on":null,"parent":"House move","flagged":false,"agent":false,"google":false},{"id":"01a1ba01-9906-76fa-833d-e8ababb62f62","title":"Undated","due":null,"overdue":false,"waiting_on":null,"parent":null,"flagged":false,"agent":false,"google":false}],"done_today":[]}"""

    private val TASKS_DONE = """
{"now":"2026-11-08T07:13:49.441Z","timezone":"America/Los_Angeles","today":"2026-11-07","open":1,"tasks":[{"id":"01a1ba01-9972-7a6f-a589-7d3d6f49f581","title":"Pack books","due":null,"overdue":false,"waiting_on":null,"parent":null,"flagged":false,"agent":false,"google":false}],"done_today":[{"id":"01a1ba01-9969-7471-b301-e4f4cbf145bc","title":"Post the notice","due":null,"overdue":false,"waiting_on":null,"parent":null,"flagged":false,"agent":false,"google":false}]}"""

    private val PEOPLE = """
{"today":"2026-11-07","overdue":[{"id":"01a1ba01-9b3a-7196-9359-941d67249edd","name":"Priya","kind":"person","last_at":"2026-09-28T19:00:00.000Z","last_via":"phone","overdue_by":10,"keep_in_touch_days":30,"birthday_in":null,"turning":null}],"birthdays":[{"id":"01a1ba01-9b4b-7825-a80a-0358f7c5a16b","name":"Sam","kind":"person","last_at":null,"last_via":null,"overdue_by":null,"keep_in_touch_days":null,"birthday_in":5,"turning":36}]}"""
}
