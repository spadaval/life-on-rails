package com.spadaval.lor.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** [PopoverPlace] (lor-fhk5): centred on the icon, kept inside the screen, the arrow on the icon. */
class PopoverTest {
    // A 1080 × 2400 screen at 2.625 px a dp: 8 dp margin, an 11 dp half arrow 10 dp tall, 4 dp gap, 6 dp chamfer.
    private val area = Bounds(0f, 0f, 1080f, 2400f)
    private val width = 651f

    private fun place(anchor: Bounds, height: Float = 500f) =
        PopoverPlace.place(anchor, width, height, area, margin = 21f, arrowHalf = 29f, arrowHeight = 26f, gap = 10f, chamfer = 16f)

    /** A 147 px icon centred at [cx], with its top at [top]. */
    private fun icon(cx: Float, top: Float) = Bounds(cx - 73.5f, top, cx + 73.5f, top + 147f)

    private fun tipOnScreen(p: PopoverPlacement) = p.left + p.tipX

    @Test
    fun anIconInTheMiddleHasTheBodyCentredOnIt() {
        val p = place(icon(540f, 600f))
        assertEquals(540f - width / 2, p.left, 0.01f)
        assertEquals(width / 2, p.tipX, 0.01f)
        assertEquals(540f, tipOnScreen(p), 0.01f)
    }

    @Test
    fun anIconAtTheLeftEdgePushesTheBodyInAndKeepsTheArrowOnIt() {
        val p = place(icon(100f, 600f))
        assertEquals(21f, p.left, 0.01f)
        assertEquals(100f, tipOnScreen(p), 0.01f)
    }

    @Test
    fun anIconAtTheRightEdgePushesTheBodyInAndKeepsTheArrowOnIt() {
        val p = place(icon(980f, 600f))
        assertEquals(1080f - 21f - width, p.left, 0.01f)
        assertEquals(p.left + p.width, 1059f, 0.01f)
        assertEquals(980f, tipOnScreen(p), 0.01f)
    }

    @Test
    fun atTheEdgeTheTipStillPointsAtTheFingerAndTheBaseStaysOnTheBody() {
        // A finger at the very edge: the body is pushed in, the base stops short of the cut corner,
        // and the arrow leans so its tip is exactly on the finger.
        for (x in listOf(0f, 24f)) {
            val left = place(Bounds(x, 600f, x, 600f))
            assertEquals(x, tipOnScreen(left), 0.001f)
            assertEquals(29f + 16f, left.baseX, 0.001f)
        }
        for (x in listOf(1080f, 1056f)) {
            val right = place(Bounds(x, 1800f, x, 1800f))
            assertFalse(right.below)
            assertEquals(x, tipOnScreen(right), 0.001f)
            assertEquals(width - 29f - 16f, right.baseX, 0.001f)
        }
        // Away from the edges the base is right under the tip.
        val middle = place(icon(540f, 600f))
        assertEquals(middle.tipX, middle.baseX, 0.001f)
    }

    @Test
    fun aSourceFillingTheScreenStillGetsAUsableMenu() {
        // The review's witness (lor-1xon): a widget filling the panel leaves no room on either side.
        val area = Bounds(0f, 24f, 412f, 880f)
        val p = PopoverPlace.place(Bounds(12f, 24f, 400f, 880f), 248f, 96f, area, 8f, 11f, 10f, 4f, 6f, minHeight = 104f)
        assertEquals(96f, p.height, 0.001f)
        assertTrue(p.bodyTop >= area.top + 8f && p.bodyTop + p.height <= area.bottom - 8f)
        // A long menu there shows the minimum and scrolls; it stays inside the area.
        val q = PopoverPlace.place(Bounds(12f, 24f, 400f, 880f), 248f, 400f, area, 8f, 11f, 10f, 4f, 6f, minHeight = 104f)
        assertEquals(104f, q.height, 0.001f)
        assertTrue(q.bodyTop >= area.top + 8f && q.bodyTop + q.height <= area.bottom - 8f)
        // The arrow stays joined to the body.
        assertEquals(if (q.below) q.bodyTop - 10f else q.bodyTop + q.height + 10f, q.tipY, 0.001f)
    }

    @Test
    fun aWidgetsMenuAtThePressHasRoom() {
        // Pointing at the finger on that widget, the roomier side holds the whole menu.
        val area = Bounds(0f, 24f, 412f, 880f)
        val p = PopoverPlace.place(Bounds(200f, 300f, 200f, 300f), 248f, 150f, area, 8f, 11f, 10f, 4f, 6f, minHeight = 104f)
        assertTrue(p.below)
        assertEquals(150f, p.height, 0.001f)
        assertEquals(304f, p.tipY, 0.001f)
        assertEquals(200f, p.left + p.tipX, 0.001f)
    }

    @Test
    fun theTopHalfOpensBelowAndTheBottomHalfAbove() {
        val top = place(icon(540f, 300f))
        assertTrue(top.below)
        assertEquals(447f + 10f, top.tipY, 0.01f)
        assertEquals(top.tipY + 26f, top.bodyTop, 0.01f)
        assertEquals(500f, top.height, 0.01f)

        val bottom = place(icon(540f, 1900f))
        assertFalse(bottom.below)
        assertEquals(1900f - 10f, bottom.tipY, 0.01f)
        assertEquals(bottom.tipY - 26f, bottom.bodyTop + bottom.height, 0.01f)
    }

    @Test
    fun theSideItOpensOnIsAlwaysTheRoomier() {
        // So a body too tall for its side is too tall for the other: there is no flip, only a clamp.
        for (top in 0..2400 step 25) for (tall in listOf(0f, 147f, 600f)) {
            val a = Bounds(500f, top.toFloat(), 580f, top + tall)
            val roomBelow = place(a, height = 5000f).let { if (it.below) it.height else null }
            val roomAbove = place(a, height = 5000f).let { if (!it.below) it.height else null }
            val other = if (roomBelow != null) maxOf(0f, (a.top - 36f) - 21f) else maxOf(0f, 2379f - (a.bottom + 36f))
            assertTrue("anchor at $top, $tall tall", (roomBelow ?: roomAbove!!) >= other)
        }
    }

    @Test
    fun aMenuTooTallForEitherSideIsClampedToTheRoomierAndScrolls() {
        val p = place(icon(540f, 700f), height = 3000f)
        // 700 above, about 1550 below: below, cut to the room there.
        assertTrue(p.below)
        assertEquals(2400f - 21f - (847f + 10f + 26f), p.height, 0.01f)
        assertEquals(2400f - 21f, p.bodyTop + p.height, 0.01f)
        assertEquals(540f, tipOnScreen(p), 0.01f)
    }

    @Test
    fun aScreenNarrowerThanTheMenuFitsItBetweenTheMargins() {
        val narrow = PopoverPlace.place(Bounds(200f, 600f, 300f, 700f), width, 300f, Bounds(0f, 0f, 500f, 2400f), 21f, 29f, 26f, 10f, 16f)
        assertEquals(21f, narrow.left, 0.01f)
        assertEquals(500f - 42f, narrow.width, 0.01f)
        assertEquals(250f, narrow.left + narrow.tipX, 0.01f)
    }
}
