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

    @Test
    fun aFolderStartsOnItsTileAndEndsInPlace() {
        val card = floatArrayOf(100f, 500f, 900f, 1300f)
        val tile = floatArrayOf(40f, 300f, 200f, 460f)
        val start = FolderMotion.at(0f, card, tile)
        org.junit.Assert.assertEquals(0.2f, start.scale, 1e-4f)
        // The card's centre (500, 900) is moved onto the tile's (120, 380).
        org.junit.Assert.assertEquals(-380f, start.dx, 1e-3f)
        org.junit.Assert.assertEquals(-520f, start.dy, 1e-3f)
        org.junit.Assert.assertEquals(0f, start.alpha, 1e-4f)
        val end = FolderMotion.at(1f, card, tile)
        org.junit.Assert.assertEquals(1f, end.scale, 1e-4f)
        org.junit.Assert.assertEquals(0f, end.dx, 1e-4f)
        org.junit.Assert.assertEquals(1f, end.alpha, 1e-4f)
        // Gone from home: a fade in place.
        val fade = FolderMotion.at(0.5f, card, null)
        org.junit.Assert.assertEquals(1f, fade.scale, 1e-4f)
        org.junit.Assert.assertEquals(0.5f, fade.alpha, 1e-4f)
    }

    @Test
    fun backScalesTheOpenCardAboutItsTile() {
        val card = floatArrayOf(100f, 500f, 900f, 1300f)
        val tile = floatArrayOf(40f, 300f, 200f, 460f)
        val f = FolderMotion.at(1f, card, tile, back = 0.9f)
        org.junit.Assert.assertEquals(0.9f, f.scale, 1e-4f)
        // The centre (500, 900) moves a tenth of the way to the tile's (120, 380).
        org.junit.Assert.assertEquals(-38f, f.dx, 1e-3f)
        org.junit.Assert.assertEquals(-52f, f.dy, 1e-3f)
        // Without back, nothing changes; at the tile, back keeps it there.
        org.junit.Assert.assertEquals(0f, FolderMotion.at(1f, card, tile).dx, 1e-4f)
        org.junit.Assert.assertEquals(-380f, FolderMotion.at(0f, card, tile, back = 0.9f).dx, 1e-3f)
    }
}
