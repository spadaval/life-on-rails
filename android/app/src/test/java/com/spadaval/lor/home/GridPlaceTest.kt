package com.spadaval.lor.home

import androidx.compose.ui.unit.IntOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    // The inner screen (lor-el51).

    /** The default grid with [pages] extra pages, halves paired, standing on [at]. */
    private fun inner(pages: Int, at: Cell = Cell(1, 0)) = GridPlace().apply {
        pair(true)
        edited(Layout.grid("d", emptyList(), emptyList(), List(pages) { emptyList() }).panels, Layout.HOME)
        goTo(at, animate = false)
    }

    @Test
    fun narrowKeepsOneCellAndItsNeighboursComposed() {
        // Home's window: itself, the agent page and page 1; the drawer only while a move involves it; not page 2.
        assertEquals(setOf(Cell(1, 0), Cell(0, 0), Cell(2, 0)), place(Cell(1, 0)).composed().toSet())
        assertEquals(setOf(Cell(3, 0), Cell(2, 0)), place(Cell(3, 0)).composed().toSet())
        assertEquals(w, place(Cell(1, 0)).widthOf(Cell(2, 0), w))
    }

    @Test
    fun aPairIsDrawnSideBySideWithItsNeighbourScreensOffScreen() {
        val g = inner(3)
        assertEquals(InnerScreen(Cell(1, 0), Cell(2, 0)), g.inner.screen)
        assertEquals(IntOffset.Zero, g.offsetOf(Cell(1, 0), w, h))
        assertEquals(IntOffset(w / 2, 0), g.offsetOf(Cell(2, 0), w, h))
        assertEquals(w / 2, g.widthOf(Cell(1, 0), w))
        assertEquals(w / 2, g.widthOf(Cell(2, 0), w))
        assertEquals(Sides.LEFT, g.sidesOf(Cell(1, 0)))
        assertEquals(Sides.RIGHT, g.sidesOf(Cell(2, 0)))
        // The agent page, full, waits a whole screen left; pages 3 and 4 a screen right, side by side.
        assertEquals(IntOffset(-w, 0), g.offsetOf(Cell(0, 0), w, h))
        assertEquals(w, g.widthOf(Cell(0, 0), w))
        assertEquals(Sides.BOTH, g.sidesOf(Cell(0, 0)))
        assertEquals(IntOffset(w, 0), g.offsetOf(Cell(3, 0), w, h))
        assertEquals(IntOffset(w + w / 2, 0), g.offsetOf(Cell(4, 0), w, h))
        // The drawer, alone on its row, is full width.
        assertEquals(w, g.widthOf(Cell(1, 1), w))
        // Composed: the pair and the screens a move reaches, the drawer only while a move involves it.
        assertEquals(setOf(Cell(1, 0), Cell(2, 0), Cell(0, 0), Cell(3, 0), Cell(4, 0)), g.composed().toSet())
        g.goTo(Cell(1, 1), animate = true, Way.DOWN, Cell(1, 1))
        assertEquals(setOf(Cell(1, 0), Cell(2, 0), Cell(0, 0), Cell(3, 0), Cell(4, 0), Cell(1, 1)), g.composed().toSet())
    }

    @Test
    fun aMoveAcrossSlidesAWholeScreen() {
        val g = inner(3)
        val right = g.nextMove(Way.RIGHT)!!
        assertEquals(GridMove(Cell(3, 0), Way.RIGHT, null), right)
        g.move = right
        g.follow.target = 1f
        g.follow.position = 0.25f
        assertEquals(IntOffset(-250, 0), g.offsetOf(Cell(1, 0), w, h))
        assertEquals(IntOffset(250, 0), g.offsetOf(Cell(2, 0), w, h))
        assertEquals(IntOffset(750, 0), g.offsetOf(Cell(3, 0), w, h))
        assertEquals(IntOffset(1250, 0), g.offsetOf(Cell(4, 0), w, h))
        assertNull(g.offsetOf(Cell(0, 0), w, h))
        g.follow.position = 1f
        g.arrive()
        assertEquals(InnerPlace(InnerScreen(Cell(3, 0), Cell(4, 0))), g.inner)
        // The end stops; left goes back to home's pair.
        assertNull(g.nextMove(Way.RIGHT))
        assertEquals(Cell(1, 0), g.next(Way.LEFT))
    }

    @Test
    fun upAndBackFromAPairsRightHalfKeepsItsFocus() {
        val g = inner(3)
        g.goTo(Cell(2, 0), animate = false, focus = Cell(2, 0))
        val up = g.nextMove(Way.DOWN)!!
        assertEquals(GridMove(Cell(1, 1), Way.DOWN, Cell(1, 1)), up)
        g.move = up
        g.follow.position = 1f
        g.arrive()
        val back = g.nextMove(Way.UP)!!
        assertEquals(GridMove(Cell(2, 0), Way.UP, Cell(2, 0)), back)
    }

    @Test
    fun aHomePressToAPairLandsWithNoFocus() {
        val g = inner(3, Cell(3, 0))
        // Start is page 2, the pair's right half: home lands on the pair, standing on its left.
        g.goTo(Cell(2, 0), animate = true)
        assertEquals(GridMove(Cell(1, 0), Way.LEFT, null), g.move)
        assertTrue(g.heading(Cell(2, 0)))
        // On the pair already, with page 2 the focus: the focus clears, nothing moves.
        val p = inner(3)
        p.goTo(Cell(2, 0), animate = false, focus = Cell(2, 0))
        p.goTo(Cell(1, 0), animate = true)
        assertNull(p.move)
        assertEquals(InnerPlace(InnerScreen(Cell(1, 0), Cell(2, 0))), p.inner)
    }

    @Test
    fun anEditThatPairsHomeBuildsThePlaceAgain() {
        // lor-70hj: home alone at full width, then page 2 added: home's screen is the pair now, and
        // a move right finds nothing past it rather than the row's first screen.
        val g = inner(0)
        assertEquals(InnerScreen(Cell(1, 0)), g.inner.screen)
        g.edited(Layout.grid("d", emptyList(), emptyList(), listOf(emptyList())).panels, Layout.HOME)
        assertEquals(InnerPlace(InnerScreen(Cell(1, 0), Cell(2, 0))), g.inner)
        assertNull(g.nextMove(Way.RIGHT))
        assertEquals(Cell(0, 0), g.next(Way.LEFT))
        // Standing on page 4 with it the focus, page 4 removed: home stands on page 3's screen, no focus.
        val p = inner(3)
        p.goTo(Cell(4, 0), animate = false, focus = Cell(4, 0))
        p.edited(Layout.grid("d", emptyList(), emptyList(), List(2) { emptyList() }).panels, Layout.HOME)
        assertEquals(InnerPlace(InnerScreen(Cell(3, 0))), p.inner)
    }

    @Test
    fun foldingAndUnfoldingKeepTheCell() {
        val g = inner(3)
        g.goTo(Cell(4, 0), animate = false, focus = Cell(4, 0))
        g.pair(false)
        assertEquals(Cell(4, 0), g.cell)
        assertEquals(w, g.widthOf(Cell(4, 0), w))
        assertEquals(Cell(3, 0), g.next(Way.LEFT))
        g.pair(true)
        assertEquals(InnerPlace(InnerScreen(Cell(3, 0), Cell(4, 0)), Cell(4, 0)), g.inner)
    }
}
