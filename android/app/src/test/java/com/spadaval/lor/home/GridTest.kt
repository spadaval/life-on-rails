package com.spadaval.lor.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The grid's neighbour, wrap and drawer-reach rules (lor-qdmy; decision lor-y7qi). */
class GridTest {
    private fun c(column: Int, row: Int) = Cell(column, row)

    /** The default grid from a version 1 file with two extra pages: t3, home, page 1, page 2; the drawer under home. */
    private val default = Layout.grid("d", emptyList(), emptyList(), listOf(emptyList(), emptyList())).panels.map { it.at }

    @Test
    fun theDefaultGridIsTodaysHome() {
        assertEquals(listOf(c(0, 0), c(1, 0), c(2, 0), c(3, 0), c(1, 1)), default)
    }

    @Test
    fun acrossGoesToTheNextCellInTheRowAndStopsAtTheEnds() {
        // From home, a swipe right reaches the agent page and a swipe left the next page.
        assertEquals(c(0, 0), GridMoves.next(default, c(1, 0), Way.LEFT))
        assertEquals(c(2, 0), GridMoves.next(default, c(1, 0), Way.RIGHT))
        // No horizontal wrap.
        assertNull(GridMoves.next(default, c(0, 0), Way.LEFT))
        assertNull(GridMoves.next(default, c(3, 0), Way.RIGHT))
        // The drawer's row has nothing beside it.
        assertNull(GridMoves.next(default, c(1, 1), Way.LEFT))
        assertNull(GridMoves.next(default, c(1, 1), Way.RIGHT))
    }

    @Test
    fun upAndDownWrapSoBothReachTheDrawerFromHome() {
        // A swipe up (the row below) and a swipe down (through the wrap) both reach the drawer.
        assertEquals(c(1, 1), GridMoves.next(default, c(1, 0), Way.DOWN))
        assertEquals(c(1, 1), GridMoves.next(default, c(1, 0), Way.UP))
        // And from the drawer either way back to home.
        assertEquals(c(1, 0), GridMoves.next(default, c(1, 1), Way.UP))
        assertEquals(c(1, 0), GridMoves.next(default, c(1, 1), Way.DOWN))
    }

    @Test
    fun everyPageReachesTheDrawerAndComesBack() {
        for (page in listOf(c(0, 0), c(2, 0), c(3, 0))) {
            assertEquals("from $page", c(1, 1), GridMoves.next(default, page, Way.DOWN))
            // Back the way it came returns to the page, not home.
            assertEquals("back to $page", page, GridMoves.next(default, c(1, 1), Way.UP, Arrival(page, Way.DOWN)))
            // Through the wrap the other way, it isn't the way back: home's column.
            assertEquals(c(1, 0), GridMoves.next(default, c(1, 1), Way.DOWN, Arrival(page, Way.DOWN)))
        }
        // Coming in from the top (a swipe down from page 2), a swipe up goes back.
        assertEquals(c(3, 0), GridMoves.next(default, c(1, 1), Way.DOWN, Arrival(c(3, 0), Way.UP)))
    }

    @Test
    fun theWayBackForgetsACellThatWent() {
        val without = default - c(3, 0)
        assertEquals(c(1, 0), GridMoves.next(without, c(1, 1), Way.UP, Arrival(c(3, 0), Way.DOWN)))
    }

    @Test
    fun aGridWithGaps() {
        // Row 0: [0,0] and [3,0]; row 2: [1,2] and [2,2]; row 1 empty; row 4: [5,4].
        val cells = listOf(c(0, 0), c(3, 0), c(1, 2), c(2, 2), c(5, 4))
        // Across skips the empty columns.
        assertEquals(c(3, 0), GridMoves.next(cells, c(0, 0), Way.RIGHT))
        assertEquals(c(0, 0), GridMoves.next(cells, c(3, 0), Way.LEFT))
        // Down skips the empty row; nearest by column, the left one on a tie.
        assertEquals(c(1, 2), GridMoves.next(cells, c(0, 0), Way.DOWN))
        assertEquals(c(2, 2), GridMoves.next(cells, c(3, 0), Way.DOWN))
        // A tie by column goes left.
        assertEquals(c(0, 2), GridMoves.nearest(listOf(c(0, 2), c(2, 2)), 2, 1))
        // Down from the last row wraps to the first; up from the first wraps to the last.
        assertEquals(c(3, 0), GridMoves.next(cells, c(5, 4), Way.DOWN))
        assertEquals(c(5, 4), GridMoves.next(cells, c(0, 0), Way.UP))
        assertEquals(c(2, 2), GridMoves.next(cells, c(5, 4), Way.UP))
    }

    @Test
    fun oneRowHasNoVerticalMove() {
        val cells = listOf(c(0, 0), c(1, 0))
        assertNull(GridMoves.next(cells, c(0, 0), Way.UP))
        assertNull(GridMoves.next(cells, c(0, 0), Way.DOWN))
    }

    @Test
    fun noCellIsStranded() {
        // From every cell of a sparse grid, every other cell is reached by some sequence of moves.
        val cells = listOf(c(0, 0), c(4, 0), c(2, 1), c(0, 3), c(1, 3), c(6, 3), c(3, 5))
        for (from in cells) {
            val seen = mutableSetOf(from)
            var edge = listOf(from)
            while (edge.isNotEmpty()) {
                edge = edge.flatMap { a -> Way.entries.mapNotNull { GridMoves.next(cells, a, it) } }.filter { seen.add(it) }
            }
            assertEquals("from $from", cells.toSet(), seen)
        }
    }

    @Test
    fun aSwipeTakesTheGridTheOtherWayAlongItsLargerComponent() {
        assertEquals(Way.RIGHT, Way.ofSwipe(-30f, 10f))
        assertEquals(Way.LEFT, Way.ofSwipe(30f, -10f))
        assertEquals(Way.DOWN, Way.ofSwipe(5f, -30f))
        assertEquals(Way.UP, Way.ofSwipe(-5f, 30f))
    }

    @Test
    fun whereHomeStandsWhenItsCellWent() {
        assertEquals(c(2, 0), GridMoves.stay(default, c(2, 0), c(1, 0)))
        // Page 3 went: the nearest in its row, the left one on a tie.
        assertEquals(c(3, 0), GridMoves.stay(default, c(4, 0), c(1, 0)))
        assertEquals(c(1, 0), GridMoves.stay(default, c(5, 7), c(1, 0)))
    }

    @Test
    fun theWayToACellAndTheWindow() {
        assertEquals(Way.LEFT, GridMoves.wayTo(c(3, 0), c(1, 0)))
        assertEquals(Way.RIGHT, GridMoves.wayTo(c(0, 0), c(1, 0)))
        assertEquals(Way.UP, GridMoves.wayTo(c(1, 1), c(1, 0)))
        assertEquals(Way.DOWN, GridMoves.wayTo(c(1, 0), c(1, 1)))
        // Back to the row the drawer was opened from, the way it came (lor-ymm9): opened by a swipe
        // down (it came in from the top, going UP), home comes back from below.
        assertEquals(Way.DOWN, GridMoves.wayTo(c(1, 1), c(1, 0), Arrival(c(1, 0), Way.UP)))
        assertEquals(Way.DOWN, GridMoves.wayTo(c(1, 1), c(1, 0), Arrival(c(3, 0), Way.UP)))
        assertEquals(Way.UP, GridMoves.wayTo(c(1, 1), c(1, 0), Arrival(c(3, 0), Way.DOWN)))
        // Home's window: itself, the agent page, page 1 and the drawer; not page 2.
        assertEquals(setOf(c(1, 0), c(0, 0), c(2, 0), c(1, 1)), GridMoves.window(default, c(1, 0), null))
        assertEquals(setOf(c(3, 0), c(2, 0), c(1, 1)), GridMoves.window(default, c(3, 0), null))
    }
}
