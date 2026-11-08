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
    fun lockedShowAndAStacksBlocksAreTheOnlyActions() {
        assertEquals(listOf(MinimapRules.Action.SHOW, MinimapRules.Action.BLOCKS), MinimapRules.actions(locked = true, stack = true))
        assertEquals(listOf(MinimapRules.Action.SHOW), MinimapRules.actions(locked = true, stack = false))
        assertEquals(MinimapRules.Action.entries, MinimapRules.actions(locked = false, stack = true))
        assertEquals(MinimapRules.Action.entries - MinimapRules.Action.BLOCKS, MinimapRules.actions(locked = false, stack = false))
    }

    private fun stack(f: LayoutFile, column: Int, row: Int = 0) = f.current.panelAt(Cell(column, row)) as Panel.Stack

    @Test
    fun theBlockListNamesEachBlockWithTheAppsCounted() {
        val home = stack(grid, 1)
        assertEquals(listOf("Clock", "Apps", "Dock"), home.blocks.map { MinimapRules.blockTitle(it) })
        assertEquals(listOf(null, "4 apps", null), home.blocks.map { MinimapRules.blockDetail(it) })
    }

    @Test
    fun moveToListsTheOtherStacksByTheirBlocksAndGoesBeforeTheDock() {
        val others = MinimapRules.otherStacks(grid.current, Cell(1, 0))
        assertEquals(listOf("Column 2: 1 app · Dock", "Column 3: 0 apps · Dock"), others.map { MinimapRules.stackEntry(it) })
        assertEquals("Column 2, row 1", MinimapRules.stackName(Cell(2, 1)))
        // Before the dock, else last.
        assertEquals(1, MinimapRules.tapIndex(others.first()))
        val noDock = grid.removeBlock(BlockAt(Cell(2, 0), 1), StackBlock.Dock)
        assertEquals(1, MinimapRules.tapIndex(stack(noDock, 2)))
        // Moving home's clock there with that index puts it over the dock.
        val moved = grid.moveBlock(BlockAt(Cell(1, 0), 0), StackBlock.Clock, Cell(2, 0), MinimapRules.tapIndex(others.first()))
        assertEquals(listOf("apps", "clock", "dock"), stack(moved, 2).blocks.map { it.type })
    }

    @Test
    fun thePaletteOffersTheTypesTheStackDoesntHold() {
        assertEquals(emptyList<String>(), MinimapRules.blockPalette(stack(grid, 1)))
        assertEquals(listOf("clock"), MinimapRules.blockPalette(stack(grid, 2)))
        val bare = grid.removeBlock(BlockAt(Cell(2, 0), 1), StackBlock.Dock)
        assertEquals(listOf("clock", "dock"), MinimapRules.blockPalette(stack(bare, 2)))
        // Each one the palette offers, addBlock takes.
        MinimapRules.blockPalette(stack(bare, 2)).forEach { bare.addBlock(Cell(2, 0), MinimapRules.tapIndex(stack(bare, 2)), it) }
    }

    @Test
    fun aWidgetIsNamedByItsProvidersLabelWithItsHeightAndNeverOffered() {
        val w = StackBlock.Widget(7, "com.example/.Clock", 180)
        assertEquals("Analog clock", MinimapRules.blockTitle(w) { "Analog clock" })
        assertEquals("com.example/.Clock", MinimapRules.blockTitle(w))
        assertEquals("Widget, 180 dp", MinimapRules.blockDetail(w))
        assertEquals("Widget", MinimapRules.blockName(w))
        // Home's stack with two widgets: the palette still offers nothing, and no stack is offered a widget.
        val two = grid.withCurrent(grid.current.copy(panels = grid.current.panels.map {
            if (it.at == Cell(1, 0)) (it as Panel.Stack).copy(blocks = listOf(StackBlock.Clock, w, StackBlock.Widget(8, "com.example/.Notes", 120)) + it.blocks.drop(1)) else it
        }))
        assertEquals(emptyList<String>(), MinimapRules.blockPalette(stack(two, 1)))
        assertEquals(listOf("clock"), MinimapRules.blockPalette(stack(two, 2)))
        assertFalse(MinimapRules.blockPalette(stack(grid.removeBlock(BlockAt(Cell(2, 0), 1), StackBlock.Dock), 2)).contains(StackBlock.Widget.TYPE))
        // The Undo bar names it by its label.
        assertEquals("Analog clock moved under Notes.", MinimapRules.movedWithin(stack(two, 1).blocks, 1, 2) { if (it.id == 7) "Analog clock" else "Notes" })
        assertEquals("Notes moved to the top.", MinimapRules.movedWithin(stack(two, 1).blocks, 2, 0) { if (it.id == 7) "Analog clock" else "Notes" })
        assertEquals("Notes removed from Column 1.", MinimapRules.removed(stack(two, 1).blocks[2], Cell(1, 0)) { "Notes" })
        // Move to… page 2 keeps its id and height, before page 2's dock.
        val page2 = stack(two, 2)
        val moved = two.moveBlock(BlockAt(Cell(1, 0), 1), w, Cell(2, 0), MinimapRules.tapIndex(page2))
        assertEquals(listOf(StackBlock.Apps(page2.blocks[0].let { (it as StackBlock.Apps).items }), w, StackBlock.Dock), stack(moved, 2).blocks)
    }

    @Test
    fun aDropGoesBetweenTheRowsMiddles() {
        val middles = listOf(50f, 150f, 250f)
        assertEquals(0, MinimapRules.dropIndex(middles, 10f))
        assertEquals(1, MinimapRules.dropIndex(middles, 99f))
        assertEquals(2, MinimapRules.dropIndex(middles, 200f))
        assertEquals(3, MinimapRules.dropIndex(middles, 400f))
    }

    @Test
    fun aDraggedBlockSwapsOnlyOncePastANeighboursMiddle() {
        // Rows of 100, 60 and 100 px, gaps included.
        val h = listOf(100f, 60f, 100f)
        val start = MinimapRules.Reorder.start(0, 3)
        // 29 px down: short of the 60 px row's middle.
        val short = start.dragged(29f) { h[it] }
        assertEquals(listOf(0, 1, 2), short.order)
        // 31 px: past it; the clock now sits second, 29 px under its new slot.
        val past = short.dragged(2f) { h[it] }
        assertEquals(listOf(1, 0, 2), past.order)
        assertEquals(1, past.at)
        assertEquals(-29f, past.offset, 0.01f)
        // One fast fling past both neighbours swaps twice.
        val far = start.dragged(200f) { h[it] }
        assertEquals(listOf(1, 2, 0), far.order)
        assertEquals(2, far.at)
        assertEquals(0, far.block)
        // And back up to the top.
        val back = far.dragged(-200f) { h[it] }
        assertEquals(listOf(0, 1, 2), back.order)
        // The ends stop it.
        assertEquals(listOf(0, 1, 2), start.dragged(-500f) { h[it] }.order)
    }

    @Test
    fun aBlockMoveSaysWhatItNowSitsUnder() {
        val blocks = stack(grid, 1).blocks
        assertEquals("Clock moved under Apps.", MinimapRules.movedWithin(blocks, 0, 1))
        assertEquals("Dock moved to the top.", MinimapRules.movedWithin(blocks, 2, 0))
        assertEquals("Apps removed from Column 1; 4 apps left home.", MinimapRules.removed(blocks[1], Cell(1, 0)))
        assertEquals("Clock removed from Column 1.", MinimapRules.removed(blocks[0], Cell(1, 0)))
    }

    @Test
    fun aMoveSaysWhereOrWhatSwapped() {
        assertEquals("Drawer moved to [3, 1].", MinimapRules.moved("Drawer", Cell(1, 1), Cell(3, 1), swapped = false))
        assertEquals("Swapped [1, 0] and [2, 0].", MinimapRules.moved("Stack", Cell(1, 0), Cell(2, 0), swapped = true))
    }
}
