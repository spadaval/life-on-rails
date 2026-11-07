package com.spadaval.lor.home

import androidx.compose.ui.unit.IntOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Where the grid host draws its cells (lor-qdmy's review: lor-glg9, lor-ymm9). */
class GridPlaceTest {
    private val w = 1000
    private val h = 2000

    /** The default grid with two extra pages, standing on [at]. */
    private fun place(at: Cell) = GridPlace().apply {
        panels = Layout.grid("d", emptyList(), emptyList(), listOf(emptyList(), emptyList())).panels
        goTo(at, animate = false)
    }

    @Test
    fun atRestNeighboursWaitOffScreen() {
        val g = place(Cell(3, 0))
        assertEquals(IntOffset.Zero, g.offsetOf(Cell(3, 0), w, h))
        assertEquals(IntOffset(-w, 0), g.offsetOf(Cell(2, 0), w, h))
        // The drawer is reached both ways (the wrap); it waits on the first, above.
        assertEquals(IntOffset(0, -h), g.offsetOf(Cell(1, 1), w, h))
    }

    @Test
    fun aStraightMoveDrawsOnlyItsTwoCells() {
        // A home press from page 2: home slides in from the left, and page 1, page 2's left
        // neighbour, isn't drawn in the same slot (lor-glg9).
        val g = place(Cell(3, 0))
        g.goTo(g.start, animate = true)
        g.follow.position = 0.25f
        assertEquals(Way.LEFT, g.move!!.way)
        assertEquals(IntOffset(250, 0), g.offsetOf(Cell(3, 0), w, h))
        assertEquals(IntOffset(-750, 0), g.offsetOf(Cell(1, 0), w, h))
        assertNull(g.offsetOf(Cell(2, 0), w, h))
        assertNull(g.offsetOf(Cell(1, 1), w, h))
    }

    @Test
    fun aHomePressLeavesTheDrawerTheWayItCame() {
        // Opened by a swipe down from home: the drawer came in from the top, so it leaves upward
        // and home comes back from below (lor-ymm9).
        val g = place(Cell(1, 1)).apply { back = Arrival(Cell(1, 0), Way.UP) }
        g.goTo(g.start, animate = true)
        assertEquals(Way.DOWN, g.move!!.way)
        // Opened by a swipe up from page 2, a home press brings home from above.
        val p = place(Cell(1, 1)).apply { back = Arrival(Cell(3, 0), Way.DOWN) }
        p.goTo(p.start, animate = true)
        assertEquals(Way.UP, p.move!!.way)
        assertEquals(Cell(1, 0), p.move!!.to)
    }
}
