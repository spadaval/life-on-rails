package com.spadaval.lor.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** The minimap's edits and the lock's switch (lor-v692). */
class GridEditTest {
    private fun ref(s: String) = AppRef.parse(s)!!

    private fun refused(edit: () -> Any): String = try {
        edit()
        fail("not refused")
        ""
    } catch (e: LayoutError) {
        e.message!!
    }

    private fun roundTrips(f: LayoutFile) = assertEquals(f, LayoutFile.parse(f.toJson()))

    /** The default grid: t3 [0, 0], home [1, 0] (clock, apps), each page to its right, drawer [1, 1]. */
    private fun grid(vararg pages: List<LayoutItem>, home: List<LayoutItem> = listOf(ref("a.a"))) =
        LayoutFile("a", listOf(Layout.grid("a", home, pages.toList())))

    private val one = grid(listOf(ref("e.e")))

    /** Home and four pages: row 0 holds five stacks with an `apps` block. */
    private val five = grid(listOf(ref("p.p1")), listOf(ref("p.p2")), listOf(ref("p.p3")), listOf(ref("p.p4")))

    private fun LayoutFile.with(vararg panels: Panel) = withCurrent(current.copy(panels = current.panels + panels))
    private fun LayoutFile.panel(column: Int, row: Int) = current.panelAt(Cell(column, row))!!
    private fun LayoutFile.blocks(column: Int, row: Int) = (panel(column, row) as Panel.Stack).blocks

    @Test
    fun theReachIsOneCellPastTheGridEachWay() {
        assertEquals(Reach(0..3, 0..2), one.current.reach)
        assertEquals(12, one.current.reach.cells.size)
        assertTrue(Cell(3, 2) in one.current.reach)
        assertFalse(Cell(4, 0) in one.current.reach)
    }

    @Test
    fun setLockedLocksAndUnlocksOnce() {
        val locked = one.setLocked(true)
        assertTrue(locked.locked)
        assertEquals("home is already locked", refused { locked.setLocked(true) })
        assertFalse(locked.setLocked(false).locked)
        assertEquals("home isn't locked", refused { one.setLocked(false) })
        roundTrips(locked)
    }

    @Test
    fun addPanelFillsAnEmptyCellInReach() {
        val added = one.addPanel(Cell(3, 0), "stack")
        assertEquals(Panel.Stack(Cell(3, 0), listOf(StackBlock.Apps(emptyList()))), added.panel(3, 0))
        roundTrips(added)
        assertEquals("a stack in another row", Panel.Stack(Cell(0, 2), listOf(StackBlock.Apps(emptyList()))), one.addPanel(Cell(0, 2), "stack").panel(0, 2))
        val noT3 = one.removePanel(Cell(0, 0), Panel.T3(Cell(0, 0)))
        assertEquals(Panel.T3(Cell(3, 1)), noT3.addPanel(Cell(3, 1), "t3").panel(3, 1))
        assertEquals("a new panel goes next to the grid", refused { one.addPanel(Cell(4, 0), "stack") })
        assertEquals("a new panel goes next to the grid", refused { one.addPanel(Cell(0, 3), "stack") })
        assertEquals(HOME_CHANGED, refused { one.addPanel(Cell(1, 0), "stack") })
        assertEquals("there's already an agent page", refused { one.addPanel(Cell(3, 0), "t3") })
        assertEquals("there's already a drawer", refused { one.addPanel(Cell(3, 0), "drawer") })
        // One lor panel a layout (lor-lz97), as t3 and drawer.
        val lor = one.addPanel(Cell(3, 0), "lor")
        assertEquals(Panel.Lor(Cell(3, 0)), lor.panel(3, 0))
        roundTrips(lor)
        assertEquals("there's already a LoR panel", refused { lor.addPanel(Cell(3, 1), "lor") })
        assertEquals("there's no panel type \"notes\"", refused { one.addPanel(Cell(3, 0), "notes") })
        assertEquals("row 0 holds 5 stacks with apps already", refused { five.addPanel(Cell(6, 0), "stack") })
        assertEquals(Cell(6, 1), five.addPanel(Cell(6, 1), "stack").panel(6, 1).at)
    }

    @Test
    fun movePanelMovesOrSwapsAndStartFollows() {
        // Into an empty cell.
        val drawer = one.panel(1, 1)
        val moved = one.movePanel(Cell(1, 1), drawer, Cell(3, 1))
        assertEquals(Panel.Drawer(Cell(3, 1)), moved.panel(3, 1))
        assertNull(moved.current.panelAt(Cell(1, 1)))
        roundTrips(moved)
        // Onto a panel: the two swap, and a start on either follows its panel.
        val wide = one.setStart(Cell(2, 0), StartScreen.WIDE)
        val home = wide.panel(1, 0)
        val swapped = wide.movePanel(Cell(1, 0), home, Cell(2, 0))
        assertEquals((home as Panel.Stack).blocks, swapped.blocks(2, 0))
        assertEquals(listOf(StackBlock.Apps(listOf(ref("e.e")))), swapped.blocks(1, 0))
        assertEquals(Start(Cell(2, 0), Cell(1, 0)), swapped.current.start)
        roundTrips(swapped)
        val toT3 = one.movePanel(Cell(1, 0), one.panel(1, 0), Cell(3, 2))
        assertEquals(Start(Cell(3, 2)), toT3.current.start)
        // Where it is, nothing changes; refusals.
        assertSame(one, one.movePanel(Cell(1, 1), drawer, Cell(1, 1)))
        assertEquals(HOME_CHANGED, refused { one.movePanel(Cell(1, 1), Panel.T3(Cell(1, 1)), Cell(3, 1)) })
        assertEquals("a panel goes next to the grid", refused { one.movePanel(Cell(1, 1), drawer, Cell(4, 1)) })
        // The row limit holds where a stack lands, by a move and by a swap.
        val sixth = five.addPanel(Cell(0, 1), "stack")
        val stack = sixth.panel(0, 1)
        assertEquals("row 0 holds 5 stacks with apps already", refused { sixth.movePanel(Cell(0, 1), stack, Cell(6, 0)) })
        assertEquals("row 0 holds 5 stacks with apps already", refused { sixth.movePanel(Cell(0, 1), stack, Cell(0, 0)) })
        assertEquals(Panel.T3(Cell(0, 1)), five.movePanel(Cell(0, 0), five.panel(0, 0), Cell(0, 1)).panel(0, 1))
    }

    @Test
    fun removePanelTakesItsAppsButNotAStart() {
        val gone = one.removePanel(Cell(2, 0), one.panel(2, 0))
        assertNull(gone.current.panelAt(Cell(2, 0)))
        assertFalse(ref("e.e") in gone.current.allItems)
        roundTrips(gone)
        assertEquals("home starts there; choose another start first", refused { one.removePanel(Cell(1, 0), one.panel(1, 0)) })
        val wide = one.setStart(Cell(2, 0), StartScreen.WIDE)
        assertEquals("home starts there; choose another start first", refused { wide.removePanel(Cell(2, 0), wide.panel(2, 0)) })
        assertEquals(HOME_CHANGED, refused { one.removePanel(Cell(2, 0), Panel.T3(Cell(2, 0))) })
        assertEquals(HOME_CHANGED, refused { one.removePanel(Cell(3, 0), Panel.T3(Cell(3, 0))) })
    }

    @Test
    fun setWideWritesOnlyWhatDiffersFromTheTypesDefault() {
        val full = one.setWide(Cell(1, 0), one.panel(1, 0), Wide.FULL)
        assertEquals(Wide.FULL, full.panel(1, 0).wide)
        assertTrue(full.toJson().contains("\"wide\": \"full\""))
        roundTrips(full)
        val back = full.setWide(Cell(1, 0), full.panel(1, 0), Wide.HALF)
        assertEquals(one, back)
        assertFalse(back.toJson().contains("\"wide\""))
        val halfT3 = one.setWide(Cell(0, 0), one.panel(0, 0), Wide.HALF)
        assertTrue(halfT3.toJson().contains("\"wide\": \"half\""))
        assertEquals("it's already full", refused { one.setWide(Cell(0, 0), one.panel(0, 0), Wide.FULL) })
        assertEquals("it's already half", refused { one.setWide(Cell(1, 1), one.panel(1, 1), Wide.HALF) })
        assertEquals(HOME_CHANGED, refused { full.setWide(Cell(1, 0), one.panel(1, 0), Wide.HALF) })
    }

    @Test
    fun setStartSetsEachScreensStartAndWritesWideOnlyWhenItDiffers() {
        assertEquals(Start(Cell(0, 0)), one.setStart(Cell(0, 0), StartScreen.NARROW).current.start)
        val wide = one.setStart(Cell(2, 0), StartScreen.WIDE)
        assertEquals(Start(Cell(1, 0), Cell(2, 0)), wide.current.start)
        roundTrips(wide)
        assertEquals("the wide start on the narrow one clears it", Start(Cell(1, 0)), wide.setStart(Cell(1, 0), StartScreen.WIDE).current.start)
        assertEquals("the narrow start on the wide one clears it", Start(Cell(2, 0)), wide.setStart(Cell(2, 0), StartScreen.NARROW).current.start)
        assertEquals(Start(Cell(0, 0), Cell(2, 0)), wide.setStart(Cell(0, 0), StartScreen.NARROW).current.start)
        assertEquals("home already starts there", refused { one.setStart(Cell(1, 0), StartScreen.NARROW) })
        assertEquals("the inner screen already starts there", refused { one.setStart(Cell(1, 0), StartScreen.WIDE) })
        assertEquals("the inner screen already starts there", refused { wide.setStart(Cell(2, 0), StartScreen.WIDE) })
        assertEquals(HOME_CHANGED, refused { one.setStart(Cell(3, 0), StartScreen.NARROW) })
    }

    @Test
    fun addBlockAddsATypeThatTakesNoFieldOnce() {
        val clocked = one.addBlock(Cell(2, 0), 0, "clock")
        assertEquals(listOf(StackBlock.Clock, StackBlock.Apps(listOf(ref("e.e")))), clocked.blocks(2, 0))
        roundTrips(clocked)
        val bare = one.removeBlock(BlockAt(Cell(1, 0), 1), one.blocks(1, 0)[1])
        assertEquals(listOf(StackBlock.Clock, StackBlock.Apps(emptyList())), bare.addBlock(Cell(1, 0), 1, "apps").blocks(1, 0))
        assertEquals("added first", listOf(StackBlock.Apps(emptyList()), StackBlock.Clock), bare.addBlock(Cell(1, 0), 0, "apps").blocks(1, 0))
        assertEquals("a stack holds one \"clock\" block", refused { one.addBlock(Cell(1, 0), 0, "clock") })
        assertEquals("a widget is added from the home menu", refused { one.addBlock(Cell(2, 0), 0, "widget") })
        // LoR's blocks take no field (lor-lz97).
        val today = one.addBlock(Cell(2, 0), 0, "today")
        assertEquals(listOf(StackBlock.Today, StackBlock.Apps(listOf(ref("e.e")))), today.blocks(2, 0))
        roundTrips(today)
        for (type in listOf("needs-you", "running", "t3-queue")) assertEquals(StackBlock.bare(type), today.addBlock(Cell(2, 0), 1, type).blocks(2, 0)[1])
        assertEquals("a stack holds one \"today\" block", refused { today.addBlock(Cell(2, 0), 0, "today") })
        assertEquals("there's no block type \"weather\"", refused { one.addBlock(Cell(2, 0), 0, "weather") })
        assertEquals(HOME_CHANGED, refused { one.addBlock(Cell(0, 0), 0, "clock") })
        assertEquals(HOME_CHANGED, refused { one.addBlock(Cell(3, 0), 0, "clock") })
        assertEquals(HOME_CHANGED, refused { one.addBlock(Cell(2, 0), 2, "clock") })
        assertEquals(HOME_CHANGED, refused { one.addBlock(Cell(2, 0), -1, "clock") })
        // An apps block counts toward the row limit where it is added.
        val clockOnly = five.with(Panel.Stack(Cell(6, 0), listOf(StackBlock.Clock)))
        assertEquals("row 0 holds 5 stacks with apps already", refused { clockOnly.addBlock(Cell(6, 0), 1, "apps") })
        assertEquals(listOf(StackBlock.Clock, StackBlock.Today), clockOnly.addBlock(Cell(6, 0), 1, "today").blocks(6, 0))
        assertEquals("there's no block type \"dock\"", refused { one.addBlock(Cell(2, 0), 0, "dock") })
    }

    @Test
    fun moveBlockWithinAStackAndToAnother() {
        val home = BlockAt(Cell(1, 0), 0)
        // Within a stack: the index is where it ends up.
        assertEquals(listOf(StackBlock.Apps(listOf(ref("a.a"))), StackBlock.Clock), one.moveBlock(home, StackBlock.Clock, Cell(1, 0), 1).blocks(1, 0))
        assertSame(one, one.moveBlock(home, StackBlock.Clock, Cell(1, 0), 0))
        // To another stack, before its block at the index.
        val moved = one.moveBlock(home, StackBlock.Clock, Cell(2, 0), 0)
        assertEquals(listOf(StackBlock.Apps(listOf(ref("a.a")))), moved.blocks(1, 0))
        assertEquals(listOf(StackBlock.Clock, StackBlock.Apps(listOf(ref("e.e")))), moved.blocks(2, 0))
        assertEquals("last", listOf(StackBlock.Apps(listOf(ref("e.e"))), StackBlock.Clock), one.moveBlock(home, StackBlock.Clock, Cell(2, 0), 1).blocks(2, 0))
        roundTrips(moved)
        // A moved apps block keeps its items.
        val homeApps = one.blocks(1, 0)[1]
        val noApps = one.with(Panel.Stack(Cell(3, 0), listOf(StackBlock.Today)))
        val carried = noApps.moveBlock(BlockAt(Cell(1, 0), 1), homeApps, Cell(3, 0), 0)
        assertEquals(listOf(homeApps, StackBlock.Today), carried.blocks(3, 0))
        assertEquals(listOf(StackBlock.Clock), carried.blocks(1, 0))
        // Refusals: the last block, a type the stack holds, what the file no longer holds, the row limit.
        val clockOnly = one.with(Panel.Stack(Cell(3, 0), listOf(StackBlock.Clock)))
        assertEquals("a stack needs a block; remove the panel instead", refused { clockOnly.moveBlock(BlockAt(Cell(3, 0), 0), StackBlock.Clock, Cell(2, 0), 0) })
        assertEquals("a stack holds one \"apps\" block", refused { one.moveBlock(BlockAt(Cell(1, 0), 1), one.blocks(1, 0)[1], Cell(2, 0), 0) })
        assertEquals(HOME_CHANGED, refused { one.moveBlock(home, StackBlock.Today, Cell(2, 0), 0) })
        assertEquals(HOME_CHANGED, refused { one.moveBlock(home, StackBlock.Clock, Cell(0, 0), 0) })
        assertEquals(HOME_CHANGED, refused { one.moveBlock(home, StackBlock.Clock, Cell(2, 0), 2) })
        assertEquals(HOME_CHANGED, refused { one.moveBlock(home, StackBlock.Clock, Cell(1, 0), 2) })
        val sixth = five.with(Panel.Stack(Cell(6, 0), listOf(StackBlock.Clock)), Panel.Stack(Cell(0, 1), listOf(StackBlock.Apps(listOf(ref("x.x"))), StackBlock.Today)))
        assertEquals("row 0 holds 5 stacks with apps already",
            refused { sixth.moveBlock(BlockAt(Cell(0, 1), 0), StackBlock.Apps(listOf(ref("x.x"))), Cell(6, 0), 1) })
    }

    @Test
    fun removeBlockLeavesAStacksLastBlock() {
        val noClock = one.removeBlock(BlockAt(Cell(1, 0), 0), StackBlock.Clock)
        assertEquals(listOf(StackBlock.Apps(listOf(ref("a.a")))), noClock.blocks(1, 0))
        roundTrips(noClock)
        val noApps = one.removeBlock(BlockAt(Cell(1, 0), 1), one.blocks(1, 0)[1])
        assertFalse("its apps leave home", ref("a.a") in noApps.current.allItems)
        val todayOnly = one.with(Panel.Stack(Cell(3, 0), listOf(StackBlock.Today)))
        assertEquals("a stack needs a block; remove the panel instead", refused { todayOnly.removeBlock(BlockAt(Cell(3, 0), 0), StackBlock.Today) })
        assertEquals(HOME_CHANGED, refused { one.removeBlock(BlockAt(Cell(1, 0), 0), StackBlock.Today) })
        assertEquals(HOME_CHANGED, refused { one.removeBlock(BlockAt(Cell(0, 0), 0), StackBlock.Clock) })
    }

    @Test
    fun aStackTakesEachTypeOnceForTheParserAndTheEdits() {
        val stack = Panel.Stack(Cell(0, 0), listOf(StackBlock.Clock, StackBlock.Widget(1, "a.b/a.b.W", 100)))
        assertFalse(stack.takes("clock"))
        assertTrue(stack.takes("apps"))
        assertTrue("widgets repeat", stack.takes("widget"))
        assertEquals("layouts[0].panels[0].blocks[1]: a stack holds one \"clock\" block",
            refused { LayoutFile.parse(v2Stack("""[{"type": "clock"}, {"type": "clock"}]""")) })
    }

    /** The five layouts lor-v692 names; sparse and wide hold widgets (lor-vtmn), so blocks edits move and remove them. */
    private val layouts: Map<String, LayoutFile> by lazy {
        val firstRun = LayoutFile("default", listOf(Layout.grid("default", listOf(ref("a.a"), Folder("F", listOf(ref("b.b"), ref("c.c")))))))
        val converted = LayoutFile.parse(
            """{"version": 1, "active": "default", "layouts": [{"name": "default", "home": ["a.a"], "dock": ["d.d"],
                "pages": [{"items": ["p.p1"]}, {"items": []}, {"items": ["p.p3", {"folder": "G", "apps": ["g.g"]}]}, {"items": ["p.p4"]}]}]}""",
        )
        val sparse = LayoutFile.parse(
            """{"version": 2, "active": "s", "layouts": [{"name": "s", "start": {"narrow": [0, 0]}, "panels": [
                {"at": [0, 0], "type": "stack", "blocks": [{"type": "apps", "items": ["a.a"]}]},
                {"at": [3, 0], "type": "t3"},
                {"at": [1, 2], "type": "stack", "blocks": [{"type": "clock"}, {"type": "widget", "id": 7, "provider": "w.w/.W", "height": 120}]},
                {"at": [4, 3], "type": "drawer"}]}]}""",
        )
        val wide = LayoutFile.parse(
            """{"version": 2, "active": "w", "layouts": [{"name": "w", "start": {"narrow": [1, 0], "wide": [2, 0]}, "panels": [
                {"at": [0, 0], "type": "t3", "wide": "half"},
                {"at": [1, 0], "type": "stack", "blocks": [{"type": "clock"}, {"type": "widget", "id": 1, "provider": "w.w/.W", "height": 40},
                  {"type": "widget", "id": 2, "provider": "w.w/.V", "height": 2000}, {"type": "apps", "items": ["a.a"]}]},
                {"at": [2, 0], "type": "stack", "wide": "full", "blocks": [{"type": "apps", "items": ["e.e"]}, {"type": "widget", "id": 3, "provider": "w.w/.W", "height": 90}]},
                {"at": [1, 1], "type": "drawer"}]}]}""",
        )
        val locked = firstRun.copy(locked = true, hidden = listOf(ref("h.h")))
        mapOf("first run" to firstRun, "converted" to converted, "sparse" to sparse, "wide" to wide, "locked" to locked)
    }

    @Test
    fun everyEditParsesBackOrRefuses() {
        for ((name, f) in layouts) {
            var took = 0
            var refusedCount = 0
            fun attempt(edit: () -> LayoutFile) {
                val result = try {
                    edit()
                } catch (e: LayoutError) {
                    refusedCount++
                    return
                }
                assertEquals("$name: an edit's result parses back", result, LayoutFile.parse(result.toJson()))
                took++
            }
            val layout = f.current
            val reach = layout.reach
            // Every cell in reach, and one past it each way.
            val cells = reach.cells + Cell(reach.columns.last + 1, 0) + Cell(0, reach.rows.last + 1)
            fun size(c: Cell) = (layout.panelAt(c) as? Panel.Stack)?.blocks?.size ?: 0
            for (on in listOf(true, false)) attempt { f.setLocked(on) }
            for (c in cells) {
                for (type in Panel.TYPES + "notes") attempt { f.addPanel(c, type) }
                for (screen in StartScreen.entries) attempt { f.setStart(c, screen) }
                for (i in -1..size(c) + 1) for (type in StackBlock.TYPES + "weather") attempt { f.addBlock(c, i, type) }
                for (id in listOf(0, 3, 99)) attempt { f.addWidget(c, id, "w.w/.W", 100) }
            }
            // The widget edits (WidgetEdit.kt), by id: each widget the layouts hold, and one they don't.
            for (id in listOf(1, 2, 3, 7, 99)) {
                attempt { f.removeWidget(id) }
                for (h in listOf(39, 40, 500, 2000, 2001)) attempt { f.setWidgetHeight(id, h) }
            }
            for (p in layout.panels) {
                for (c in cells) attempt { f.movePanel(p.at, p, c) }
                attempt { f.removePanel(p.at, p) }
                for (w in Wide.entries) attempt { f.setWide(p.at, p, w) }
                if (p !is Panel.Stack) continue
                p.blocks.forEachIndexed { b, block ->
                    attempt { f.removeBlock(BlockAt(p.at, b), block) }
                    for (c in cells) for (i in -1..size(c) + 1) attempt { f.moveBlock(BlockAt(p.at, b), block, c, i) }
                }
            }
            println("$name: $took edits took, $refusedCount refused")
            assertTrue("$name: no edit took", took > 0)
            assertTrue("$name: no edit refused", refusedCount > 0)
        }
    }
}
