package com.spadaval.lor.home

import com.spadaval.lor.net.DeviceHome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import java.time.Duration
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

    @Test
    fun alarmWithinADay() {
        val now = at("22:00")
        assertEquals("Alarm 7:00", alarmLine(Instant.parse("2026-11-07T07:00:00-08:00"), now, zone))
        // Exactly 24 hours away still shows; a minute more doesn't.
        assertEquals("Alarm 22:00", alarmLine(now.plus(Duration.ofHours(24)), now, zone))
        assertNull(alarmLine(now.plus(Duration.ofHours(24)).plusSeconds(60), now, zone))
        // Two days away, none, or one already gone.
        assertNull(alarmLine(now.plus(Duration.ofDays(2)), now, zone))
        assertNull(alarmLine(null, now, zone))
        assertNull(alarmLine(now.minusSeconds(60), now, zone))
        // Home's 24-hour clock.
        assertEquals("Alarm 13:05", alarmLine(at("13:05").plus(Duration.ofDays(1)), now, zone))
    }

    private fun weather(c: Double, observed: Instant = at("12:00")) = DeviceHome.Weather(c, "Partly cloudy", true, observed)

    @Test
    fun weatherInEachUnit() {
        val now = at("12:30")
        assertEquals("18° Partly cloudy", weatherLine(weather(18.0), now, fahrenheit = false))
        assertEquals("64° Partly cloudy", weatherLine(weather(18.0), now, fahrenheit = true))
        assertEquals("-5° Partly cloudy", weatherLine(weather(-5.0), now, fahrenheit = false))
        assertEquals("23° Partly cloudy", weatherLine(weather(-5.0), now, fahrenheit = true))
    }

    @Test
    fun weatherRoundsToWholeDegrees() {
        val now = at("12:30")
        assertEquals("18° Partly cloudy", weatherLine(weather(17.5), now, false))
        assertEquals("17° Partly cloudy", weatherLine(weather(17.4), now, false))
        // 17.4 °C is 63.32 °F; 17.5 °C is 63.5 °F.
        assertEquals("63° Partly cloudy", weatherLine(weather(17.4), now, true))
        assertEquals("64° Partly cloudy", weatherLine(weather(17.5), now, true))
    }

    @Test
    fun noOrStaleWeatherIsNotShown() {
        assertNull(weatherLine(null, at("12:30"), false))
        // Observed at 12:00: shown until 14:00, not after.
        assertEquals("18° Partly cloudy", weatherLine(weather(18.0), at("14:00"), false))
        assertNull(weatherLine(weather(18.0), at("14:01"), false))
    }
}
