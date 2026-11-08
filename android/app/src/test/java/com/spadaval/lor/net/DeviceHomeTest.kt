package com.spadaval.lor.net

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/**
 * `lor/home.ndjson`: three `GET /device/home` replies from the LoR test server, seeded with two
 * flagged tasks and two events: with no timer, then a timer running, then paused.
 */
class DeviceHomeTest {
    private val replies = javaClass.classLoader!!.getResource("lor/home.ndjson")!!.readText().lines().filter { it.isNotBlank() }.map { DeviceHome.parse(JSONObject(it)) }

    @Test
    fun readsTheDayNeedsAndTheOneThing() {
        val home = replies[0]
        assertEquals(ZoneId.of("America/New_York"), home.zone)
        assertEquals(Instant.parse("2026-11-05T13:00:00Z"), home.day.wakeStart)
        assertEquals(Instant.parse("2026-11-06T03:00:00Z"), home.day.wakeEnd)
        assertEquals(listOf("Reply to the landlord about the lease", "Shortlist ceremony venues"), home.needsYou.map { it.title })
        assertEquals("He needs an answer by Friday.", home.needsYou[0].note)
        assertEquals(listOf("Dentist", "Dinner with Priya"), home.day.events.map { it.title })
        assertEquals("Dentist", home.one!!.text)
        assertFalse(home.one!!.isTimer)
        assertNull(home.day.running)
        assertTrue(home.day.remainingFraction in 0.0..1.0)
    }

    @Test
    fun theTimerComesFromTheDayNotTheOneThing() {
        val (_, running, paused) = replies
        assertTrue(running.one!!.isTimer)
        assertEquals(false, running.day.running!!.paused)
        // While paused, `one` still says "Running since"; the day says paused.
        assertTrue(paused.one!!.isTimer)
        assertEquals(true, paused.day.running!!.paused)
    }

    @Test
    fun nullsAndOffsetsAreRead() {
        val o = JSONObject(javaClass.classLoader!!.getResource("lor/home.ndjson")!!.readText().lines().first())
        o.put("one", JSONObject.NULL).put("now", "2026-11-05T16:03:03-05:00")
        val home = DeviceHome.parse(o)
        assertNull(home.one)
        assertEquals(Instant.parse("2026-11-05T21:03:03Z"), home.now)
    }

    private fun first() = JSONObject(javaClass.classLoader!!.getResource("lor/home.ndjson")!!.readText().lines().first())

    @Test
    fun readsTheWeather() {
        val o = first().put("weather", JSONObject("""{"temp_c":17.6,"condition":"Partly cloudy","is_day":true,"observed_at":"2026-11-07T22:15:00.000Z"}"""))
        assertEquals(DeviceHome.Weather(17.6, "Partly cloudy", true, Instant.parse("2026-11-07T22:15:00Z")), DeviceHome.parse(o).weather)
        // A whole number comes as an integer in the JSON.
        assertEquals(18.0, DeviceHome.parse(first().put("weather", JSONObject("""{"temp_c":18,"condition":"Clear","is_day":false,"observed_at":"2026-11-07T22:15:00Z"}"""))).weather!!.tempC, 0.0)
    }

    @Test
    fun noWeatherIsNull() {
        assertNull(DeviceHome.parse(first().put("weather", JSONObject.NULL)).weather)
        // An older server sends no field at all.
        val older = first().also { it.remove("weather") }
        assertFalse(older.has("weather"))
        assertNull(DeviceHome.parse(older).weather)
    }
}
