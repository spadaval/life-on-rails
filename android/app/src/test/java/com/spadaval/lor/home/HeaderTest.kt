package com.spadaval.lor.home

import com.spadaval.lor.net.DeviceHome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/** Home's header (launcher polish 6.7): the event line and `look.header`. */
class HeaderTest {
    private val zone = ZoneId.of("America/Los_Angeles")
    private fun at(hm: String) = Instant.parse("2026-11-06T${hm}:00-08:00")
    private fun ev(title: String, start: String, end: String) = DeviceHome.Event(title, at(start), at(end))

    @Test
    fun underWayThenNextThenNothing() {
        val standup = ev("Standup", "10:00", "10:30")
        val lunch = ev("Lunch", "12:00", "13:00")
        assertEquals("Standup until 10:30", headerEvent(listOf(lunch, standup), at("10:10"), zone))
        assertEquals("Lunch at 12:00", headerEvent(listOf(standup, lunch), at("09:00").plusSeconds(3600 + 1900), zone))
        // Standup just ended: the next one shows.
        assertEquals("Lunch at 12:00", headerEvent(listOf(standup, lunch), at("10:30"), zone))
        assertNull(headerEvent(listOf(standup, lunch), at("13:00"), zone))
        assertNull(headerEvent(emptyList(), at("13:00"), zone))
    }

    @Test
    fun headerParses() {
        val f = LayoutFile.parse(v2Empty(""""look": {"header": "clock"},"""))
        assertEquals(HomeLook.Header.CLOCK, f.look.header)
        assertEquals(f, LayoutFile.parse(f.toJson()))
        try {
            LayoutFile.parse(v2Empty(""""look": {"header": "big"},"""))
            fail()
        } catch (e: LayoutError) {
            assertEquals("look.header: must be one of date, clock, none", e.message)
        }
    }
}
