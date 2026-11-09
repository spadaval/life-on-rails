package com.spadaval.lor.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionTest {
    @Test
    fun aSlowReleaseCommitsPastThirtyPercentOfItsTravel() {
        assertFalse(FollowMotion.settles(fromOpen = false, start = 0f, shown = 0.25f, upDpS = 100f))
        assertTrue(FollowMotion.settles(fromOpen = false, start = 0f, shown = 0.35f, upDpS = -100f))
        // From open: closing needs 30% of travel down.
        assertTrue(FollowMotion.settles(fromOpen = true, start = 1f, shown = 0.75f, upDpS = 0f))
        assertFalse(FollowMotion.settles(fromOpen = true, start = 1f, shown = 0.65f, upDpS = 0f))
        // A gesture that caught the drawer mid-way measures 30% of the travel left from where it caught it.
        assertFalse(FollowMotion.settles(fromOpen = false, start = 0.5f, shown = 0.6f, upDpS = 0f))
        assertTrue(FollowMotion.settles(fromOpen = false, start = 0.5f, shown = 0.7f, upDpS = 0f))
    }

    @Test
    fun aDrawerCaughtNearAnEndCanStillBeTakenToTheOther() {
        // Caught closing at 0.8 and dragged fully open, let go slowly: it opens.
        assertTrue(FollowMotion.settles(fromOpen = false, start = 0.8f, shown = 1f, upDpS = 0f))
        assertFalse(FollowMotion.settles(fromOpen = false, start = 0.8f, shown = 0.82f, upDpS = 0f))
        // The mirror: caught at 0.2 from open and pulled fully shut, it closes.
        assertFalse(FollowMotion.settles(fromOpen = true, start = 0.2f, shown = 0f, upDpS = 0f))
        assertTrue(FollowMotion.settles(fromOpen = true, start = 0.2f, shown = 0.18f, upDpS = 0f))
        // The point is never past either end, wherever the gesture started.
        for (s in listOf(0f, 0.5f, 0.9f, 0.99f)) assertTrue(FollowMotion.commitPoint(false, s) <= 1f)
        for (s in listOf(1f, 0.5f, 0.1f, 0.01f)) assertTrue(FollowMotion.commitPoint(true, s) >= 0f)
    }

    @Test
    fun aFlingDecidesByItsDirection() {
        assertTrue(FollowMotion.settles(fromOpen = false, start = 0f, shown = 0.05f, upDpS = 1200f))
        assertFalse(FollowMotion.settles(fromOpen = true, start = 1f, shown = 0.8f, upDpS = -1200f))
    }

    @Test
    fun aVerticalMoveCommitsAt110DpOrAFlingPast24Dp() {
        // lor-83uq: releases from the Fold's log, on a 960 dp tall cover screen.
        val h = 960f
        assertTrue(FollowMotion.settles(fromOpen = false, start = 0f, shown = 0.155f, upDpS = 700f, sizeDp = h))
        assertTrue(FollowMotion.settles(fromOpen = false, start = 0f, shown = 0.194f, upDpS = 462f, sizeDp = h))
        assertFalse(FollowMotion.settles(fromOpen = false, start = 0f, shown = 0.15f, upDpS = -489f, sizeDp = h))
        assertFalse(FollowMotion.settles(fromOpen = false, start = 0f, shown = 0.01f, upDpS = 440f, sizeDp = h))
        // A slow release goes by distance: 110 dp is 0.1146 of 960.
        assertFalse(FollowMotion.settles(fromOpen = false, start = 0f, shown = 0.11f, upDpS = 0f, sizeDp = h))
        assertTrue(FollowMotion.settles(fromOpen = false, start = 0f, shown = 0.12f, upDpS = 0f, sizeDp = h))
        // A fling just past 24 dp (0.025 of 960) goes; at 400 dp/s or under it goes by distance.
        assertTrue(FollowMotion.settles(fromOpen = false, start = 0f, shown = 0.026f, upDpS = 401f, sizeDp = h))
        assertFalse(FollowMotion.settles(fromOpen = false, start = 0f, shown = 0.05f, upDpS = 400f, sizeDp = h))
        // The mirror, from open (the drawer closing): down 120 dp slowly closes, 100 dp stays open.
        assertFalse(FollowMotion.settles(fromOpen = true, start = 1f, shown = 0.875f, upDpS = 0f, sizeDp = h))
        assertTrue(FollowMotion.settles(fromOpen = true, start = 1f, shown = 0.896f, upDpS = 0f, sizeDp = h))
        // From open, a fling down past 24 dp closes, and one back up keeps it open however far it went.
        assertFalse(FollowMotion.settles(fromOpen = true, start = 1f, shown = 0.97f, upDpS = -500f, sizeDp = h))
        assertTrue(FollowMotion.settles(fromOpen = true, start = 1f, shown = 0.5f, upDpS = 500f, sizeDp = h))
        // Caught near the far end, the point is still short of it: 30% of what is left.
        assertTrue(FollowMotion.commitPoint(false, 0.95f, h) < 1f)
        assertTrue(FollowMotion.settles(fromOpen = false, start = 0.95f, shown = 1f, upDpS = 0f, sizeDp = h))
    }

    @Test
    fun aHorizontalMoveKeepsThirtyPercentAnd800DpS() {
        // The same releases as a page swipe: 0.155 at 700 dp/s and 0.194 at 462 dp/s stay.
        assertFalse(FollowMotion.settles(fromOpen = false, start = 0f, shown = 0.155f, upDpS = 700f))
        assertFalse(FollowMotion.settles(fromOpen = false, start = 0f, shown = 0.194f, upDpS = 462f))
        assertTrue(FollowMotion.settles(fromOpen = false, start = 0f, shown = 0.05f, upDpS = 801f))
    }

    @Test
    fun theTickIsAtTheCommitPointEitherWay() {
        val p = FollowMotion.commitPoint(false, 0f)
        assertTrue(FollowMotion.crossed(p, 0.2f, 0.4f))
        assertTrue(FollowMotion.crossed(p, 0.4f, 0.2f))
        assertFalse(FollowMotion.crossed(p, 0.4f, 0.5f))
        assertTrue(FollowMotion.crossed(FollowMotion.commitPoint(true, 1f), 0.8f, 0.6f))
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

    @Test
    fun itemKeysAreUniqueForFoldersWithOneNameAndFollowTheirItems() {
        val a = AppRef("com.a")
        val f1 = Folder("Tools", listOf(AppRef("com.b")))
        val f2 = Folder("Tools", listOf(AppRef("com.c")))
        val keys = itemKeys(listOf(a, f1, f2))
        org.junit.Assert.assertEquals(3, keys.toSet().size)
        // The same items in another order keep their keys (apps and the folder names that differ).
        val b = AppRef("com.b", "com.b.Main")
        val f = Folder("Media", emptyList())
        val one = itemKeys(listOf(a, b, f)).zip(listOf(a, b, f)).toMap()
        val other = itemKeys(listOf(f, a, b)).zip(listOf(f, a, b)).toMap()
        org.junit.Assert.assertEquals(one, other)
    }
}
