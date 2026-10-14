package com.spadaval.lor.collect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionsTest {
    private fun ev(pkg: String, type: Int, t: Long) = UsageEvent(pkg, type, t * 1000)
    private val R = Sessions.ACTIVITY_RESUMED
    private val P = Sessions.ACTIVITY_PAUSED

    @Test
    fun switchingAppsClosesTheFirstWhicheverEventComesFirst() {
        // Newer Android reports B resumed before A paused; older, the other way round.
        val r = Sessions.build(listOf(ev("a", R, 0), ev("b", R, 60), ev("a", P, 61), ev("b", P, 120), ev("c", R, 200), ev("c", P, 260)))
        assertEquals(listOf(Session("a", 0, 60_000), Session("b", 60_000, 120_000), Session("c", 200_000, 260_000)), r.closed)
        assertNull(r.openSince)
    }

    @Test
    fun screenOffEndsTheSessionAndShortGapsInOneAppAreMerged() {
        val r = Sessions.build(listOf(
            ev("a", R, 0), ev("a", P, 30), ev("a", R, 40), ev("a", P, 100), // a 10 s gap inside one app
            ev("b", R, 200), ev("", Sessions.SCREEN_NON_INTERACTIVE, 250),
        ))
        assertEquals(listOf(Session("a", 0, 100_000), Session("b", 200_000, 250_000)), r.closed)
    }

    @Test
    fun theSessionStillOpenIsHeldBackAndReported() {
        val r = Sessions.build(listOf(ev("a", R, 0), ev("a", P, 50), ev("a", R, 55), ev("b", R, 100), ev("a", P, 100), ev("b", P, 130), ev("b", R, 140)))
        // a closed; b is open again 10 s after it paused, so its session continues and is held back from 100 s.
        assertEquals(listOf(Session("a", 0, 100_000)), r.closed)
        assertEquals(100_000L, r.openSince)
        assertEquals("b", r.openPkg)
    }

    @Test
    fun theLauncherAndFlickersAreDropped() {
        val r = Sessions.build(
            listOf(ev("home", R, 0), ev("a", R, 30), ev("a", P, 31), ev("b", R, 31), ev("b", P, 90)),
            ignore = setOf("home"),
        )
        assertEquals(listOf(Session("b", 31_000, 90_000)), r.closed)
    }

    @Test
    fun observationIdsAreStableAndValid() {
        val a = Obs.id("app", "pixel-9a", "com.example", 1234L)
        assertEquals(a, Obs.id("app", "pixel-9a", "com.example", 1234L))
        assert(a.matches(Regex("^[0-9a-f]{8}-[0-9a-f]{4}-3[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")))
        assertEquals("2026-10-14T16:00:00Z", Obs.iso(1_791_993_600_000L))
    }
}
