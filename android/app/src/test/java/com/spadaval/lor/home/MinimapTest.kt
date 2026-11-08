package com.spadaval.lor.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** What the minimap draws and offers (lor-6get), from the file's grid. */
class MinimapTest {
    private fun ref(s: String) = AppRef.parse(s)!!

    /** The default grid with two pages: t3 [0, 0], home [1, 0], pages [2, 0] and [3, 0], drawer [1, 1]. */
    private val home = listOf(ref("a.a"), ref("b.b"), Folder("Dev", listOf(ref("c.c"), ref("d.d"))))
    private val grid = LayoutFile("a", listOf(Layout.grid("a", home, listOf(ref("x.x")), listOf(listOf(ref("e.e")), emptyList()))))

    private fun tile(f: LayoutFile, column: Int, row: Int) = MinimapRules.rows(f.current)[row][column]

    @Test
    fun theMapIsTheReachWithEachPanelNamedByItsType() {
        val rows = MinimapRules.rows(grid.current)
        // Columns 0 to one past the last (3), rows 0 to one past the last (1).
        assertEquals(3, rows.size)
        assertTrue(rows.all { it.size == 5 })
        assertEquals(listOf(Cell(0, 0), Cell(1, 0), Cell(2, 0), Cell(3, 0), Cell(4, 0)), rows[0].map { it.cell })
        assertEquals(listOf("Agent page", "Stack", "Stack", "Stack", null), rows[0].map { it.name })
        assertEquals(listOf(null, "Drawer", null, null, null), rows[1].map { it.name })
        assertTrue(rows[2].all { it.panel == null })
    }

    @Test
    fun aStackListsItsBlocksWithEveryAppCounted() {
        // Two apps and a folder of two: four apps.
        assertEquals("Clock\u00A0· 4\u00A0apps\u00A0· Dock", tile(grid, 1, 0).blocks)
        assertEquals("1\u00A0app\u00A0· Dock", tile(grid, 2, 0).blocks)
        assertEquals("0\u00A0apps\u00A0· Dock", tile(grid, 3, 0).blocks)
        assertNull(tile(grid, 0, 0).blocks)
        assertEquals("Stack at [1, 0]. Clock · 4 apps · Dock. Half. Phone start. Inner start", tile(grid, 1, 0).description)
        assertEquals("Empty cell [4, 0]", tile(grid, 4, 0).description)
    }

    @Test
    fun eachPanelSaysFullOrHalf() {
        assertEquals("Full", tile(grid, 0, 0).wide)
        assertEquals("Half", tile(grid, 1, 0).wide)
        assertEquals("Half", tile(grid, 1, 1).wide)
        val full = grid.setWide(Cell(2, 0), grid.current.panelAt(Cell(2, 0))!!, Wide.FULL)
        assertEquals("Full", tile(full, 2, 0).wide)
    }

    @Test
    fun theStartsMoveTogetherUntilTheInnerOneIsSet() {
        // No start.wide: both marks are on the narrow start, and setting it moves both.
        assertTrue(tile(grid, 1, 0).phoneStart && tile(grid, 1, 0).innerStart)
        val moved = grid.setStart(Cell(2, 0), StartScreen.NARROW)
        assertTrue(tile(moved, 2, 0).phoneStart && tile(moved, 2, 0).innerStart)
        assertFalse(tile(moved, 1, 0).phoneStart || tile(moved, 1, 0).innerStart)
        // The inner start set apart, each mark stays where it is put.
        val inner = grid.setStart(Cell(3, 0), StartScreen.WIDE)
        assertTrue(tile(inner, 1, 0).phoneStart)
        assertFalse(tile(inner, 1, 0).innerStart)
        assertTrue(tile(inner, 3, 0).innerStart)
        // A moved start panel takes its mark with it.
        val swapped = inner.movePanel(Cell(3, 0), inner.current.panelAt(Cell(3, 0))!!, Cell(4, 0))
        assertTrue(tile(swapped, 4, 0).innerStart)
    }

    @Test
    fun halvesThatShareAnInnerScreenAreJoined() {
        // Row 0: t3 full; home and page 2 halves pair; page 3 alone. Row 1: the drawer alone.
        assertEquals(listOf(Cell(1, 0) to Cell(2, 0)), MinimapRules.pairs(grid.current))
        val full = grid.setWide(Cell(2, 0), grid.current.panelAt(Cell(2, 0))!!, Wide.FULL)
        assertEquals(emptyList<Pair<Cell, Cell>>(), MinimapRules.pairs(full.current))
    }

    @Test
    fun thePaletteDimsATypeTheLayoutHoldsItsOneOfWithAddPanelsReason() {
        val kinds = MinimapRules.palette(grid.current)
        assertEquals(Panel.TYPES, kinds.map { it.type })
        assertEquals(listOf("Stack", "Agent page", "Drawer"), kinds.map { it.name })
        assertEquals(listOf(null, "there's already an agent page", "there's already a drawer"), kinds.map { it.taken })
        // The palette's reason is the edit's own refusal.
        val refusal = try {
            grid.addPanel(Cell(4, 0), "drawer")
            null
        } catch (e: LayoutError) {
            e.message
        }
        assertEquals(kinds.last().taken, refusal)
        // Without a drawer, it can be added.
        val noDrawer = grid.removePanel(Cell(1, 1), grid.current.panelAt(Cell(1, 1))!!)
        assertNull(MinimapRules.palette(noDrawer.current).last().taken)
    }

    @Test
    fun lockedShowIsTheOnlyAction() {
        assertEquals(listOf(MinimapRules.Action.SHOW), MinimapRules.actions(locked = true))
        assertEquals(MinimapRules.Action.entries, MinimapRules.actions(locked = false))
    }

    @Test
    fun aMoveSaysWhereOrWhatSwapped() {
        assertEquals("Drawer moved to [3, 1].", MinimapRules.moved("Drawer", Cell(1, 1), Cell(3, 1), swapped = false))
        assertEquals("Swapped [1, 0] and [2, 0].", MinimapRules.moved("Stack", Cell(1, 0), Cell(2, 0), swapped = true))
    }
}
