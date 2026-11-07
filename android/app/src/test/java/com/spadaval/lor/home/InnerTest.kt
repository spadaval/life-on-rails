package com.spadaval.lor.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The inner screen's rules: pairing, moves, the focus cell, folding and a stack's wide flow (lor-j87u; decision lor-b1dx). */
class InnerTest {
    private fun c(column: Int, row: Int) = Cell(column, row)
    private fun full(column: Int, row: Int = 0) = InnerScreen(c(column, row))
    private fun pair(left: Int, right: Int, row: Int = 0) = InnerScreen(c(left, row), c(right, row))

    /** The default grid from a version 1 file with [pages] extra pages. */
    private fun default(pages: Int) = Layout.grid("d", emptyList(), emptyList(), List(pages) { emptyList() }).panels

    private fun stack(column: Int, row: Int = 0, wide: Wide = Wide.HALF) = Panel.Stack(c(column, row), listOf(StackBlock.Apps(emptyList())), wide)

    // Inner screens of a row.

    @Test
    fun theDefaultGridWithEachNumberOfPages() {
        val drawer = full(1, 1)
        // Home alone: the agent page, home drawn full, the drawer.
        assertEquals(listOf(full(0), full(1), drawer), InnerScreens.of(default(0)))
        // Home and page 2 side by side.
        assertEquals(listOf(full(0), pair(1, 2), drawer), InnerScreens.of(default(1)))
        assertEquals(listOf(full(0), pair(1, 2), full(3), drawer), InnerScreens.of(default(2)))
        assertEquals(listOf(full(0), pair(1, 2), pair(3, 4), drawer), InnerScreens.of(default(3)))
    }

    @Test
    fun threeHalvesInARowLeaveTheThirdAlone() {
        assertEquals(listOf(pair(0, 1), full(2)), InnerScreens.of(listOf(stack(0), stack(1), stack(2))))
    }

    @Test
    fun aFullPanelBetweenTwoHalvesPairsNeither() {
        assertEquals(listOf(full(0), full(1), full(2)), InnerScreens.of(listOf(stack(0), Panel.T3(c(1, 0)), stack(2))))
        // A stack set to full is its own screen too.
        assertEquals(listOf(full(0), full(1), full(2)), InnerScreens.of(listOf(stack(0), stack(1, wide = Wide.FULL), stack(2))))
    }

    @Test
    fun twoHalvesPairAcrossAGap() {
        assertEquals(listOf(pair(0, 3)), InnerScreens.of(listOf(stack(3), stack(0))))
    }

    @Test
    fun aT3SetToHalfPairsWithAStack() {
        assertEquals(listOf(pair(0, 1)), InnerScreens.of(listOf(Panel.T3(c(0, 0), Wide.HALF), stack(1))))
    }

    @Test
    fun theDrawerAloneOnItsRowIsDrawnFull() {
        assertEquals(listOf(full(1, 1)), InnerScreens.of(listOf(Panel.Drawer(c(1, 1)))))
        // Each row pairs on its own: a half doesn't pair with the row below.
        assertEquals(listOf(full(0), full(0, 1)), InnerScreens.of(listOf(stack(0), Panel.Drawer(c(0, 1)))))
    }

    // Moves between inner screens.

    @Test
    fun acrossGoesOneScreenAndTheEndsStop() {
        val screens = InnerScreens.of(default(2))
        val home = InnerPlace(pair(1, 2), c(2, 0))
        assertEquals(InnerMove(InnerPlace(full(0)), Way.LEFT, null), InnerScreens.next(screens, home, Way.LEFT))
        assertEquals(InnerMove(InnerPlace(full(3)), Way.RIGHT, null), InnerScreens.next(screens, home, Way.RIGHT))
        assertNull(InnerScreens.next(screens, InnerPlace(full(0)), Way.LEFT))
        assertNull(InnerScreens.next(screens, InnerPlace(full(3)), Way.RIGHT))
        assertNull("the drawer has nothing beside it", InnerScreens.next(screens, InnerPlace(full(1, 1)), Way.RIGHT))
    }

    @Test
    fun fromThePairToTheDrawerAndBack() {
        val screens = InnerScreens.of(default(2))
        // Untouched, the pair's left (home) stands in: a swipe up reaches the drawer under home.
        val up = InnerScreens.next(screens, InnerPlace(pair(1, 2)), Way.DOWN)!!
        assertEquals(InnerMove(InnerPlace(full(1, 1), c(1, 1)), Way.DOWN, Arrival(c(1, 0), Way.DOWN)), up)
        val down = InnerScreens.next(screens, up.to, Way.UP, up.back)!!
        assertEquals(InnerPlace(pair(1, 2), c(1, 0)), down.to)
        // Through the wrap the other way isn't the way back, but it lands on home's pair too.
        assertEquals(InnerPlace(pair(1, 2), c(1, 0)), InnerScreens.next(screens, up.to, Way.DOWN, up.back)!!.to)
        // From the lone page 3, up to the drawer and back the way it came, to page 3.
        val fromPage = InnerScreens.next(screens, InnerPlace(full(3)), Way.DOWN)!!
        assertEquals(InnerPlace(full(1, 1), c(1, 1)), fromPage.to)
        assertEquals(InnerPlace(full(3), c(3, 0)), InnerScreens.next(screens, fromPage.to, Way.UP, fromPage.back)!!.to)
    }

    @Test
    fun movesOnAGridWithGaps() {
        // Row 0: halves at 0 and 3 (a pair across the gap), a full t3 at 5; row 2: a half at 4; row 1 empty.
        val panels = listOf(stack(0), stack(3), Panel.T3(c(5, 0)), stack(4, 2))
        val screens = InnerScreens.of(panels)
        assertEquals(listOf(pair(0, 3), full(5), full(4, 2)), screens)
        assertEquals(InnerPlace(full(5)), InnerScreens.next(screens, InnerPlace(pair(0, 3)), Way.RIGHT)!!.to)
        // Down skips the empty row, from the focus cell; the nearest by column.
        val down = InnerScreens.next(screens, InnerPlace(pair(0, 3), c(3, 0)), Way.DOWN)!!
        assertEquals(InnerPlace(full(4, 2), c(4, 2)), down.to)
        // Down again wraps to row 0: not the way back, so the nearest to column 4 is page 3 (on the pair).
        assertEquals(InnerPlace(pair(0, 3), c(3, 0)), InnerScreens.next(screens, down.to, Way.DOWN, down.back)!!.to)
        // From the pair's left, down lands on the same row-2 screen, and back up returns to the left.
        val fromLeft = InnerScreens.next(screens, InnerPlace(pair(0, 3)), Way.DOWN)!!
        assertEquals(InnerPlace(full(4, 2), c(4, 2)), fromLeft.to)
        assertEquals(InnerPlace(pair(0, 3), c(0, 0)), InnerScreens.next(screens, fromLeft.to, Way.UP, fromLeft.back)!!.to)
        // One row has no vertical move.
        assertNull(InnerScreens.next(InnerScreens.of(listOf(stack(0), stack(1))), InnerPlace(pair(0, 1)), Way.DOWN))
    }

    // The focus cell.

    @Test
    fun aTouchedHalfKeepsTheFocusThroughTheDrawer() {
        val screens = InnerScreens.of(default(2))
        val touched = InnerScreens.place(screens, c(1, 0))!!.touched(c(2, 0))
        assertEquals(InnerPlace(pair(1, 2), c(2, 0)), touched)
        val up = InnerScreens.next(screens, touched, Way.DOWN)!!
        assertEquals(InnerPlace(full(1, 1), c(1, 1)), up.to)
        val down = InnerScreens.next(screens, up.to, Way.UP, up.back)!!
        assertEquals("back to the pair, page 2 still the focus", InnerPlace(pair(1, 2), c(2, 0)), down.to)
        assertEquals(c(2, 0), InnerScreens.fold(down.to))
    }

    @Test
    fun aPairReachedAcrossHasNoFocus() {
        val screens = InnerScreens.of(default(2))
        val across = InnerScreens.next(screens, InnerPlace(full(0)), Way.RIGHT)!!
        assertEquals(InnerPlace(pair(1, 2)), across.to)
        assertEquals("a fold lands on home", c(1, 0), InnerScreens.fold(across.to))
    }

    @Test
    fun movingAcrossAndBackClearsATouch() {
        val screens = InnerScreens.of(default(2))
        val touched = InnerPlace(pair(1, 2)).touched(c(2, 0))
        val away = InnerScreens.next(screens, touched, Way.RIGHT)!!
        val back = InnerScreens.next(screens, away.to, Way.LEFT)!!
        assertEquals(InnerPlace(pair(1, 2)), back.to)
        assertEquals(c(1, 0), InnerScreens.fold(back.to))
    }

    @Test
    fun aHomePressClearsTheFocusAndATouchElsewhereIsIgnored() {
        val screens = InnerScreens.of(default(2))
        assertEquals(InnerPlace(pair(1, 2)), InnerScreens.place(screens, c(2, 0)))
        assertNull(InnerScreens.place(screens, c(9, 9)))
        assertEquals(InnerPlace(pair(1, 2)), InnerPlace(pair(1, 2)).touched(c(3, 0)))
    }

    // Folding and unfolding.

    @Test
    fun unfoldingLandsOnTheScreenHoldingTheCell() {
        val screens = InnerScreens.of(default(3))
        assertEquals(InnerPlace(pair(3, 4), c(3, 0)), InnerScreens.unfold(screens, c(3, 0)))
        assertEquals(InnerPlace(pair(1, 2), c(2, 0)), InnerScreens.unfold(screens, c(2, 0)))
        assertEquals(InnerPlace(full(0), c(0, 0)), InnerScreens.unfold(screens, c(0, 0)))
        assertEquals(InnerPlace(full(1, 1), c(1, 1)), InnerScreens.unfold(screens, c(1, 1)))
        assertNull(InnerScreens.unfold(screens, c(9, 0)))
    }

    @Test
    fun foldingLandsOnTheFullCellOrThePairsFocus() {
        val screens = InnerScreens.of(default(3))
        // A full screen: its cell.
        assertEquals(c(0, 0), InnerScreens.fold(InnerPlace(full(0))))
        assertEquals(c(1, 1), InnerScreens.fold(InnerPlace(full(1, 1), c(1, 1))))
        // Unfolded from page 4 and folded again: page 4.
        assertEquals(c(4, 0), InnerScreens.fold(InnerScreens.unfold(screens, c(4, 0))!!))
        // Unfolded from page 4, then page 3 touched: page 3.
        assertEquals(c(3, 0), InnerScreens.fold(InnerScreens.unfold(screens, c(4, 0))!!.touched(c(3, 0))))
        // Reached across and never touched: the left half.
        val across = InnerScreens.next(screens, InnerPlace(pair(1, 2)), Way.RIGHT)!!.to
        assertEquals(c(3, 0), InnerScreens.fold(across))
    }

    // A stack's wide flow.

    @Test
    fun todaysHomeStackPutsTheClockLeftAndAppsWithTheDockRight() {
        val apps = StackBlock.Apps(emptyList())
        assertEquals(listOf(listOf(StackBlock.Clock), listOf(apps, StackBlock.Dock)), StackFlow.wide(listOf(StackBlock.Clock, apps, StackBlock.Dock)))
        // The dock goes under the right column wherever it is in the list.
        assertEquals(listOf(listOf(StackBlock.Clock), listOf(apps, StackBlock.Dock)), StackFlow.wide(listOf(StackBlock.Dock, StackBlock.Clock, apps)))
    }

    @Test
    fun aPageIsOneColumn() {
        val apps = StackBlock.Apps(emptyList())
        assertEquals(listOf(listOf(apps, StackBlock.Dock)), StackFlow.wide(listOf(apps, StackBlock.Dock)))
        assertEquals(listOf(listOf(apps, StackBlock.Dock)), StackFlow.wide(listOf(StackBlock.Dock, apps)))
        assertEquals(listOf(listOf(StackBlock.Dock)), StackFlow.wide(listOf(StackBlock.Dock)))
    }

    @Test
    fun aStackWithNoDockSplitsInReadingOrder() {
        val apps = StackBlock.Apps(emptyList())
        assertEquals(listOf(listOf(StackBlock.Clock), listOf(apps)), StackFlow.wide(listOf(StackBlock.Clock, apps)))
        assertEquals(listOf(listOf(apps)), StackFlow.wide(listOf(apps)))
    }

    @Test
    fun aStackOfFourBlocks() {
        // Block types later issues add (widget, today); the rule doesn't depend on them.
        val isDock = { b: String -> b == "dock" }
        assertEquals(listOf(listOf("clock", "widget"), listOf("today", "apps")), StackFlow.wide(listOf("clock", "widget", "today", "apps"), isDock))
        assertEquals(listOf(listOf("clock", "widget"), listOf("apps", "dock")), StackFlow.wide(listOf("clock", "widget", "apps", "dock"), isDock))
        // Five besides the dock: the left takes three.
        assertEquals(
            listOf(listOf("a", "b", "c"), listOf("d", "e", "dock")),
            StackFlow.wide(listOf("a", "dock", "b", "c", "d", "e"), isDock),
        )
    }
}
