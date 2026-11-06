package com.spadaval.lor.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionTest {
    @Test
    fun aSlowReleaseCommitsPastThirtyPercentOfItsTravel() {
        assertFalse(DrawerMotion.settles(fromOpen = false, start = 0f, shown = 0.25f, upDpS = 100f))
        assertTrue(DrawerMotion.settles(fromOpen = false, start = 0f, shown = 0.35f, upDpS = -100f))
        // From open: closing needs 30% of travel down.
        assertTrue(DrawerMotion.settles(fromOpen = true, start = 1f, shown = 0.75f, upDpS = 0f))
        assertFalse(DrawerMotion.settles(fromOpen = true, start = 1f, shown = 0.65f, upDpS = 0f))
        // A gesture that caught the drawer mid-way measures from where it caught it.
        assertFalse(DrawerMotion.settles(fromOpen = false, start = 0.5f, shown = 0.7f, upDpS = 0f))
    }

    @Test
    fun aFlingDecidesByItsDirection() {
        assertTrue(DrawerMotion.settles(fromOpen = false, start = 0f, shown = 0.05f, upDpS = 1200f))
        assertFalse(DrawerMotion.settles(fromOpen = true, start = 1f, shown = 0.8f, upDpS = -1200f))
    }

    @Test
    fun theTickIsAtTheCommitPointEitherWay() {
        val p = DrawerMotion.commitPoint(false, 0f)
        assertTrue(DrawerMotion.crossed(p, 0.2f, 0.4f))
        assertTrue(DrawerMotion.crossed(p, 0.4f, 0.2f))
        assertFalse(DrawerMotion.crossed(p, 0.4f, 0.5f))
        assertTrue(DrawerMotion.crossed(DrawerMotion.commitPoint(true, 1f), 0.8f, 0.6f))
    }
}
