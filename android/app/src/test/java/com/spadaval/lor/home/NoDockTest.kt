package com.spadaval.lor.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Home has no dock (lor-bdus). A file written before that keeps its dock apps: each `dock` block
 * goes, and each dock app not on home already joins the end of the first page with room.
 */
class NoDockTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun ref(s: String) = AppRef.parse(s)!!

    private fun error(text: String): String = try {
        LayoutFile.parse(text)
        fail("parsed: $text")
        ""
    } catch (e: LayoutError) {
        e.message!!
    }

    /** A version 2 layout "a" with [dock], [panels] and the start at [start]. */
    private fun v2(dock: String, panels: String, start: String = "[1, 0]") =
        """{"version": 2, "active": "a", "layouts": [{"name": "a", "start": {"narrow": $start}, "dock": [$dock], "panels": [$panels]}]}"""

    private fun stackAt(column: Int, blocks: String) = """{"at": [$column, 0], "type": "stack", "blocks": [$blocks]}"""

    private fun appsOf(vararg items: String) = """{"type": "apps", "items": [${items.joinToString { "\"$it\"" }}]}"""

    private val dockBlock = """{"type": "dock"}"""

    private fun blocks(f: LayoutFile, column: Int) = (f.current.panelAt(Cell(column, 0)) as Panel.Stack).blocks

    /** Five full pages: the start row holds as many stacks with an `apps` block as it may. */
    private fun fullHome(dock: String) = v2(dock, (1..5).joinToString { c -> stackAt(c, appsOf(*(1..24).map { "p$c.p$it" }.toTypedArray())) })

    @Test
    fun aFullHomeRefusesNamingTheAppAndOverwritesNothing() {
        assertEquals(
            "layouts[0].dock[1]: home is full (5 pages of 24), so \"e.e\" can't move there from the dock, which is gone; make room on home or take it out of \"dock\"",
            error(fullHome("\"p1.p1\", \"e.e\"")),
        )
        // Home shows the last good file, and the file stays as it was written.
        val store = LayoutStore(tmp.root.resolve("layout.json"))
        val good = LayoutFile("a", listOf(Layout.grid("a", listOf(ref("a.a")))))
        store.write(good)
        val text = fullHome("\"e.e\"")
        store.file.writeText(text)
        try {
            store.read(null); fail("read")
        } catch (e: LayoutError) {
            assertTrue(e.message!!.contains("\"e.e\""))
        }
        assertEquals(good, store.readGood())
        assertEquals(text, store.file.readText())
        assertTrue(store.edit { it }.isFailure)
        assertEquals(text, store.file.readText())
        // With nothing new in the dock, the full home reads.
        assertEquals(5, LayoutFile.parse(fullHome("\"p3.p3\"")).current.pages.size)
    }

    /**
     * lor-bdus's review, R3: on the upgrade, layout.json and layout.good.json hold the same old file,
     * and neither converts. Home still shows that home, without the app that didn't fit, and
     * neither file is written.
     */
    @Test
    fun anOldFullHomeIsStillShownFromItsLastGoodCopyWithoutWritingEither() {
        val store = LayoutStore(tmp.root.resolve("layout.json"))
        val text = fullHome("\"p2.p2\", \"e.e\"")
        store.file.writeText(text)
        store.good.writeText(text)
        try {
            store.read(null); fail("read")
        } catch (e: LayoutError) {
            assertTrue(e.message!!.contains("\"e.e\""))
        }
        // Nothing a restore could write from, but a home to show, naming what was left out.
        assertNull(store.readGood())
        val shown = store.readLastGood()!!
        assertEquals(listOf(ref("e.e")), shown.left)
        assertEquals(5, shown.layout.current.pages.size)
        assertEquals(24 * 5, shown.layout.current.allItems.size)
        val state = LayoutState.Broken("x", shown.layout, shown.left)
        assertEquals("layout.json has an error. Showing the last good layout without 1 old dock app.", brokenLine(state))
        assertTrue(leftOutLine(shown.left).contains("\"e.e\""))
        // An edit is refused, and neither file changes.
        assertTrue(store.edit { it }.isFailure)
        assertEquals(text, store.file.readText())
        assertEquals(text, store.good.readText())
        // A good copy that converts in full is shown as before, with nothing left out.
        store.good.writeText(fullHome("\"p2.p2\""))
        assertEquals(emptyList<AppRef>(), store.readLastGood()!!.left)
        assertEquals(store.readGood(), store.readLastGood()!!.layout)
        assertEquals("layout.json has an error. Showing the last good layout.", brokenLine(LayoutState.Broken("x", store.readGood())))
    }

    /**
     * lor-bdus's review, R4: R2 and R3 together. A dock-only start beside five full pages, the same
     * old file in layout.json and layout.good.json: home shows a clock-only start without the dock
     * app, and that start, which has no apps block, still carries the banner (its block 0).
     */
    @Test
    fun aDockOnlyStartBesideAFullHomeStillCarriesTheBanner() {
        val full = (1..5).joinToString { c -> stackAt(c, appsOf(*(1..24).map { "p$c.p$it" }.toTypedArray())) }
        val text = v2("\"d.d\"", "${stackAt(0, dockBlock)}, $full", start = "[0, 0]")
        val store = LayoutStore(tmp.root.resolve("layout.json"))
        store.file.writeText(text)
        store.good.writeText(text)
        assertTrue(store.edit { it }.isFailure)
        assertNull(store.readGood())
        val shown = store.readLastGood()!!
        assertEquals(listOf(ref("d.d")), shown.left)
        assertEquals(120, shown.layout.current.allItems.size)
        val start = shown.layout.current.panelAt(Cell(0, 0)) as Panel.Stack
        assertEquals(listOf(StackBlock.Clock), start.blocks)
        assertEquals(0, notesBlock(start))
        assertEquals("layout.json has an error. Showing the last good layout without 1 old dock app.", brokenLine(LayoutState.Broken("x", shown.layout, shown.left)))
        assertEquals(text, store.file.readText())
        assertEquals(text, store.good.readText())
        // With an apps block, the notes go with it, wherever it is in the stack.
        assertEquals(1, notesBlock(Panel.Stack(Cell(0, 0), listOf(StackBlock.Clock, StackBlock.Apps(emptyList())))))
    }
}
