package com.spadaval.lor.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The finger on a [Follow] (lor-eo1g): what the drawer and the grid's axes share. */
class FollowTest {
    private class Run(start: Float, target: Float) {
        val follow = Follow(start).also { it.target = target }
        var ticks = 0
        var committed: Boolean? = null
        var heldAtCommit: Boolean? = null
        val finger = FollowFinger(follow, 1000f, 2f, onTick = { ticks++ }) { toOne, _ ->
            committed = toOne
            heldAtCommit = follow.held
        }
    }

    @Test
    fun aFingerHoldsTheFollowAndMovesIt1To1() {
        val r = Run(0f, 0f)
        assertTrue(r.follow.held)
        // 400 px toward 1 (negative travel) over 1000 px is 0.4.
        r.finger.move(-400f)
        assertEquals(0.4f, r.follow.position, 1e-5f)
        // Never past either end.
        r.finger.move(-1500f)
        assertEquals(1f, r.follow.position, 1e-5f)
        r.finger.move(300f)
        assertEquals(0f, r.follow.position, 1e-5f)
    }

    @Test
    fun theCommitPointTicksOncePerCrossingEitherWay() {
        val r = Run(0f, 0f)
        r.finger.move(-200f)
        assertEquals(0, r.ticks)
        r.finger.move(-350f) // past 0.3
        assertEquals(1, r.ticks)
        r.finger.move(-400f) // still past
        assertEquals(1, r.ticks)
        r.finger.move(-100f) // back short of it
        assertEquals(2, r.ticks)
        // From 1 (target 1), the point is 30% of the way down: 0.7.
        val o = Run(1f, 1f)
        o.finger.move(250f)
        assertEquals(0, o.ticks)
        o.finger.move(350f)
        assertEquals(1, o.ticks)
    }

    @Test
    fun aSlowReleaseSettlesBySideOfTheCommitPoint() {
        val short = Run(0f, 0f)
        short.finger.move(-250f)
        short.finger.release(0f)
        assertEquals(false, short.committed)
        assertEquals(0f, short.follow.target, 0f)
        assertFalse(short.follow.held)
        assertEquals(true, short.heldAtCommit)

        val past = Run(0f, 0f)
        past.finger.move(-350f)
        past.finger.release(0f)
        assertEquals(true, past.committed)
        assertEquals(1f, past.follow.target, 0f)
        assertFalse(past.follow.held)

        // From 1, a pull of 25% goes back; 35% closes.
        val back = Run(1f, 1f)
        back.finger.move(250f)
        back.finger.release(0f)
        assertEquals(1f, back.follow.target, 0f)
        val closes = Run(1f, 1f)
        closes.finger.move(350f)
        closes.finger.release(0f)
        assertEquals(0f, closes.follow.target, 0f)
    }

    @Test
    fun aFlingAgainstThePositionWins() {
        // Moved 90% toward 1, then flung back toward 0 at 1000 dp/s (2000 px/s at density 2): it goes to 0.
        val r = Run(0f, 0f)
        r.finger.move(-900f)
        r.finger.release(2000f)
        assertEquals(false, r.committed)
        assertEquals(0f, r.follow.target, 0f)
        // The settle starts from the finger's velocity, in sizes per second toward 1.
        assertEquals(-2f, r.follow.fling, 1e-5f)
        // The mirror: from 1, pulled to 0.15 and flung up, it stays at 1.
        val o = Run(1f, 1f)
        o.finger.move(850f)
        o.finger.release(-2000f)
        assertEquals(true, o.committed)
        assertEquals(1f, o.follow.target, 0f)
        assertEquals(2f, o.follow.fling, 1e-5f)
    }

    @Test
    fun aFingerCatchingASettleMeasuresFromWhereItCaughtIt() {
        // Caught at 0.5 while closing (target 0): the point is 0.65, as FollowMotion.commitPoint says.
        val r = Run(0.5f, 0f)
        r.finger.move(-100f) // 0.6
        r.finger.release(0f)
        assertEquals(0f, r.follow.target, 0f)
        val p = Run(0.5f, 0f)
        p.finger.move(-200f) // 0.7
        p.finger.release(0f)
        assertEquals(1f, p.follow.target, 0f)
    }
}
