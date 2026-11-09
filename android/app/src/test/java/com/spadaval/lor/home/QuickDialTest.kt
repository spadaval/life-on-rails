package com.spadaval.lor.home

import com.spadaval.lor.net.DeviceHome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class QuickDialTest {
    private val zone = ZoneId.of("America/Los_Angeles")
    /** A Tuesday. */
    private val today = LocalDate.of(2026, 11, 10)

    private fun at(day: LocalDate, time: String) = day.atTime(LocalTime.parse(time)).atZone(zone).toInstant().toEpochMilli()

    private fun id(pkg: String) = AppId(pkg, "$pkg.Main")

    /** Opens of each app, `pkg to times`, counted in time order. */
    private fun opens(vararg each: Pair<String, List<Long>>): AppOpens =
        each.flatMap { (pkg, times) -> times.map { pkg to it } }.sortedBy { it.second }
            .fold(AppOpens()) { o, (pkg, t) -> o.opened(pkg, t, zone) }

    /** [time] on each of the [days] days before [today] that [on] takes. */
    private fun daily(time: String, days: Int = 21, on: (LocalDate) -> Boolean = { true }) =
        (1..days).map { today.minusDays(it.toLong()) }.filter(on).map { at(it, time) }

    private fun rank(o: AppOpens, now: Long, apps: List<String>) = rankForNow(apps.map(::id), { it }, o, now, zone).map { it.packageName }

    @Test
    fun anAppOpenedEveryMorningRanksFirstAt8AndNotAt20() {
        val o = opens("united" to daily("07:30"), "whatsapp" to daily("20:15"), "music" to daily("13:00"))
        val apps = listOf("music", "united", "whatsapp")
        assertEquals("united", rank(o, at(today, "08:00"), apps).first())
        val evening = rank(o, at(today, "20:00"), apps)
        assertEquals("whatsapp", evening.first())
        assertTrue(evening.indexOf("united") > 0)
    }

    @Test
    fun aWeekendOnlyAppRanksOnSaturdayAndNotOnTuesday() {
        val weekend = { d: LocalDate -> d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY }
        // Eight apps opened on weekday mornings fill Tuesday's eight.
        val weekdayApps = (1..8).map { "work$it" }
        val o = opens("hike" to daily("09:00", 28, weekend), *weekdayApps.map { it to daily("09:30", 28) { d -> !weekend(d) } }.toTypedArray())
        val apps = weekdayApps + "hike"
        val saturday = today.with(java.time.temporal.TemporalAdjusters.next(DayOfWeek.SATURDAY))
        assertEquals("hike", rank(o, at(saturday, "09:00"), apps).first())
        val tuesday = rank(o, at(today, "09:00"), apps)
        assertEquals(QUICK_APPS, tuesday.size)
        assertFalse("hike" in tuesday)
    }

    @Test
    fun anAppOpenedOnceRanksBelowADailyOne() {
        val yesterday = today.minusDays(1)
        // Opened once, in the very slot asked about, against a daily app of another hour.
        val o = opens("once" to listOf(at(yesterday, "08:00")), "daily" to daily("13:00", 14))
        assertEquals(listOf("daily", "once"), rank(o, at(today, "08:00"), listOf("daily", "once")))
        // And against a daily app of the same hour.
        val same = opens("once" to listOf(at(yesterday, "08:00")), "daily" to daily("08:10", 14))
        assertEquals(listOf("daily", "once"), rank(same, at(today, "08:00"), listOf("daily", "once")))
    }

    @Test
    fun appsNeverOpenedAreLeftOutAndWorkAppsCountUnderTheirOwnKey() {
        val o = opens("work:com.slack" to daily("10:00", 3))
        val apps = listOf(AppId("com.slack", "com.slack.Main"), AppId("com.slack", "com.slack.Main", work = true))
        assertEquals(listOf(apps[1]), rankForNow(apps, { it }, o, at(today, "10:00"), zone))
        assertEquals("work:com.slack", opensKey(apps[1]))
    }

    @Test
    fun usageEventsCountAChangeOfAppAfterTheCursorExceptLorAndHome() {
        val t = at(today, "08:00")
        val start = AppOpens().readTo(t, null)
        val events = listOf(
            Resumed("maps", t - 1000), // before the cursor: read last time
            Resumed("maps", t + 1000),
            Resumed("maps", t + 2000), // still in front: not a new open
            Resumed("com.spadaval.lor", t + 3000),
            Resumed("maps", t + 4000), // back from home: a new open
            Resumed("mail", t + 5000),
        )
        val o = start.withUsage(events, setOf("com.spadaval.lor"), t + 6000, zone)
        assertEquals(2.0, o.counts.getValue("maps").sum(), 1e-3)
        assertEquals(1.0, o.counts.getValue("mail").sum(), 1e-3)
        assertFalse("com.spadaval.lor" in o.counts)
        assertEquals(t + 6000, o.cursor)
        assertEquals("mail", o.foreground)
        // The next read continues from the foreground app: mail again isn't a new open.
        val next = o.withUsage(listOf(Resumed("mail", t + 7000)), emptySet(), t + 8000, zone)
        assertEquals(1.0, next.counts.getValue("mail").sum(), 1e-3)
    }

    @Test
    fun countsHalveInTwoWeeksAndSurviveTheFile() {
        val t = at(today, "08:00")
        val o = AppOpens().opened("maps", t, zone).opened("mail", t + AppOpens.HALF_LIFE_MS, zone)
        assertEquals(0.5, o.counts.getValue("maps").sum(), 1e-6)
        val back = AppOpens.parse(o.readTo(t + 5, "mail").toJson())
        assertEquals(o.at, back.at)
        assertEquals("mail", back.foreground)
        assertEquals(o.last, back.last)
        assertEquals(o.score("maps", t + 20, zone), back.score("maps", t + 20, zone), 1e-9)
        assertEquals(0, AppOpens.parse("not json").counts.size)
    }

    @Test
    fun bandsCoverTheDayAndNightWraps() {
        assertEquals(Band.NIGHT, Band.of(23))
        assertEquals(Band.NIGHT, Band.of(5))
        assertEquals(Band.MORNING, Band.of(6))
        assertEquals(Band.EVENING, Band.of(21))
        assertEquals(24, Band.entries.sumOf { it.hours })
        assertEquals(1.0, (0 until HourSlot.COUNT).sumOf { i -> HourSlot(Band.entries[i / 2], i % 2 == 1).share }, 1e-9)
        assertEquals("Tuesday evening", HourSlot.of(java.time.Instant.ofEpochMilli(at(today, "19:00")), zone).words(java.time.Instant.ofEpochMilli(at(today, "19:00")), zone))
    }

    private val now = at(today, "12:00")
    private val day = 24 * 60 * 60 * 1000L

    @Test
    fun peopleAreTheNewestConversationPerPersonOfTheLastTwoWeeks() {
        val chats = listOf(
            Chat("Alice", now - 1 * day, "whatsapp:alice"),
            Chat("alice ", now - 2 * day, "messages:alice"), // the same person in another app, older
            Chat("Bob", now - 3 * day, "messages:bob"),
            Chat("Old friend", now - 15 * day, "messages:old"), // more than 14 days ago
        )
        assertEquals(listOf("whatsapp:alice", "messages:bob"), recentPeople(chats, now).map { it.value })
    }

    @Test
    fun peopleAreAtMostFourNewestFirst() {
        val chats = (1..6).map { Chat("P$it", now - it * day, "p$it") }
        assertEquals(listOf("p1", "p2", "p3", "p4"), recentPeople(chats, now).map { it.value })
        assertEquals(emptyList<String>(), recentPeople(emptyList<Chat<String>>(), now).map { it.value })
    }

    @Test
    fun theCardIsLorsOneThingGreyedWithItsTimeWhenNotCurrent() {
        val one = DeviceHome.One("In 40 min", "Dinner", null)
        assertNull(quickCard(null, true, null, zone))
        assertEquals(QuickCard("In 40 min", "Dinner", null), quickCard(one, true, null, zone))
        val read = java.time.Instant.ofEpochMilli(at(today, "07:05"))
        assertEquals(QuickCard("In 40 min", "Dinner", "as of 7:05"), quickCard(one, false, read, zone))
    }

    @Test
    fun anAppIsNewForThreeDaysUntilOpened() {
        assertTrue(isNew(now - day, 0, now))
        assertFalse(isNew(now - day, now - 1000, now))
        assertFalse(isNew(now - 4 * day, 0, now))
    }
}
