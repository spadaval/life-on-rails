package com.spadaval.lor.net

import org.junit.Assert.assertEquals
import org.junit.Test

/** The process's LoR reader (lor-leh4, lor-lz97): when the next read is due, and which. */
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
}
