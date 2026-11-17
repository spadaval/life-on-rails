package com.spadaval.lor.home

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/** The lab's fixtures are in New York: its zone must read the same on both targets. */
class ZoneTest {
    @Test
    fun newYorkIsReadOnBothTargets() {
        val t = Instant.parse("2026-11-17T20:00:00Z").toLocalDateTime(TimeZone.of("America/New_York"))
        assertEquals(15, t.hour)
        assertEquals("15:00", clockText(Instant.parse("2026-11-17T20:00:00Z"), TimeZone.of("America/New_York")))
    }
}
