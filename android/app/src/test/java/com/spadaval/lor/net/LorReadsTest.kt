package com.spadaval.lor.net

import org.junit.Assert.assertEquals
import org.junit.Test

/** The process's `/device/home` reader (lor-leh4): when the next read is due. */
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
}
