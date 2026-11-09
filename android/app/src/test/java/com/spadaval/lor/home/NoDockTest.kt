package com.spadaval.lor.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun aVersion1FilesDockJoinsTheEndOfHome() {
        val f = LayoutFile.parse("""{"version": 1, "active": "a", "layouts": [{"name": "a", "home": ["a.a"], "dock": ["d.d", "e.e"]}]}""")
        assertEquals(Layout.grid("a", listOf(ref("a.a"), ref("d.d"), ref("e.e"))), f.current)
    }

    @Test
    fun aVersion2FilesDockBlocksGoAndItsAppsJoinTheFirstPage() {
        val text = v2(
            "\"d.d\", \"e.e\"",
            """{"at": [0, 0], "type": "t3"},
               ${stackAt(1, """{"type": "clock"}, ${appsOf("a.a")}, $dockBlock""")},
               ${stackAt(2, """${appsOf("b.b")}, $dockBlock""")}""",
        )
        val f = LayoutFile.parse(text)
        assertEquals(listOf(StackBlock.Clock, StackBlock.Apps(listOf(ref("a.a"), ref("d.d"), ref("e.e")))), blocks(f, 1))
        assertEquals(listOf(StackBlock.Apps(listOf(ref("b.b")))), blocks(f, 2))
        // The phone writes no dock, and what it writes reads back the same.
        val json = f.toJson()
        assertFalse(json.contains("dock"))
        assertEquals(f, LayoutFile.parse(json))
    }

    @Test
    fun aStartStackWithNoAppsBlockSendsTheDockToTheFirstPageElseANewOne() {
        // The start stack holds a clock and the dock; page 2 has room, so the dock goes there.
        val withPage = LayoutFile.parse(v2("\"d.d\"", "${stackAt(1, """{"type": "clock"}, $dockBlock""")}, ${stackAt(2, appsOf("b.b"))}"))
        assertEquals(listOf(StackBlock.Clock), blocks(withPage, 1))
        assertEquals(listOf(StackBlock.Apps(listOf(ref("b.b"), ref("d.d")))), blocks(withPage, 2))
        // With no page, a new page after the start row's last panel, as Add to home makes one.
        val alone = LayoutFile.parse(v2("\"d.d\"", stackAt(1, """{"type": "clock"}, $dockBlock""")))
        assertEquals(listOf(StackBlock.Clock), blocks(alone, 1))
        assertEquals(listOf(StackBlock.Apps(listOf(ref("d.d")))), blocks(alone, 2))
    }

    @Test
    fun aStackOfOnlyDockGoesUnlessItIsTheStart() {
        // Away from the start, it goes.
        val away = LayoutFile.parse(v2("", "${stackAt(1, appsOf("a.a"))}, ${stackAt(2, dockBlock)}"))
        assertEquals(listOf(Cell(1, 0)), away.current.panels.map { it.at })
        // At the start, it stays as an empty page, which takes the dock's apps.
        val start = LayoutFile.parse(v2("\"d.d\"", "${stackAt(1, dockBlock)}, ${stackAt(2, dockBlock)}"))
        assertEquals(listOf(Cell(1, 0)), start.current.panels.map { it.at })
        assertEquals(listOf(StackBlock.Apps(listOf(ref("d.d")))), blocks(start, 1))
        val empty = LayoutFile.parse(v2("", stackAt(1, dockBlock)))
        assertEquals(listOf(StackBlock.Apps(emptyList())), blocks(empty, 1))
        // Two dock blocks in one stack are an old file's, not an error.
        assertEquals(listOf(StackBlock.Apps(emptyList())), blocks(LayoutFile.parse(v2("", stackAt(1, "$dockBlock, ${appsOf()}, $dockBlock"))), 1))
    }

    @Test
    fun aFullFirstPageSendsTheAppToTheNextPage() {
        val full = (1..24).map { "p.p$it" }.toTypedArray()
        val f = LayoutFile.parse(v2("\"d.d\"", "${stackAt(1, appsOf(*full))}, ${stackAt(2, appsOf("b.b"))}, ${stackAt(3, appsOf())}"))
        assertEquals(24, f.current.items(BlockAt(Cell(1, 0), 0))!!.size)
        assertEquals(listOf(ref("b.b"), ref("d.d")), f.current.items(BlockAt(Cell(2, 0), 0)))
        assertEquals(emptyList<LayoutItem>(), f.current.items(BlockAt(Cell(3, 0), 0)))
    }

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

    @Test
    fun aDockAppAlreadyOnHomeIsNotAddedAgain() {
        val f = LayoutFile.parse(
            v2(
                "\"a.a\", \"f.f/f.f.Main\", \"work:w.w\", \"w.w\"",
                stackAt(1, """{"type": "apps", "items": ["a.a", {"folder": "F", "apps": ["f.f"]}, "work:w.w"]}, $dockBlock"""),
            ),
        )
        // On home itself, in a folder (a package names its activity too), in the work profile: kept as is.
        // The main profile's w.w is another app, so it joins.
        assertEquals(
            listOf(ref("a.a"), Folder("F", listOf(ref("f.f"))), ref("work:w.w"), ref("w.w")),
            f.current.items(BlockAt(Cell(1, 0), 0)),
        )
    }

    @Test
    fun aFreshInstallHasNoDockAndNoGoogleFolder() {
        val google = (1..21).map { "com.google.android.app$it" }
        val mine = (google + listOf("com.android.dialer", "com.whatsapp", "com.google.android.gm", "org.other")).toSet()
        val f = LayoutFile.firstRun(mine, listOf("com.android.dialer", "com.not.installed"))
        // The dialer first, then the common apps installed; every other app is in the drawer only.
        assertEquals(listOf(ref("com.android.dialer"), ref("com.whatsapp"), ref("com.google.android.gm")), f.current.allItems)
        assertTrue(f.current.allItems.none { it is Folder })
        assertFalse(f.toJson().contains("dock"))
        assertEquals(f, LayoutFile.parse(f.toJson()))
    }
}
