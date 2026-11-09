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
                "\"a.a\", \"f.f/.Main\", \"work:w.w\", \"w.w\"",
                stackAt(1, """{"type": "apps", "items": ["a.a", {"folder": "F", "apps": ["f.f/f.f.Main"]}, "work:w.w"]}, $dockBlock"""),
            ),
        )
        // The same ref on home itself, in a folder (`.Main` written short), in the work profile: kept as is.
        // The main profile's w.w is another app, so it joins.
        assertEquals(
            listOf(ref("a.a"), Folder("F", listOf(ref("f.f/f.f.Main"))), ref("work:w.w"), ref("w.w")),
            f.current.items(BlockAt(Cell(1, 0), 0)),
        )
    }

    /** lor-bdus's review, R1: a package ref draws as one activity, which the parser can't know, so a near match is kept. */
    @Test
    fun aDockActivityAPackageRefOnHomeMayNotDrawAsIsKept() {
        val a = AppId("p.p", "p.p.A")
        val b = AppId("p.p", "p.p.B")
        val installed = listOf(a, b)
        for ((home, dock) in listOf("p.p" to "p.p/p.p.B", "p.p/p.p.B" to "p.p")) {
            val f = LayoutFile.parse(v2("\"$dock\"", stackAt(1, "${appsOf(home)}, $dockBlock")))
            assertEquals(listOf(ref(home), ref(dock)), f.current.allItems)
            // Home draws both activities, and the file the phone writes keeps both.
            assertEquals(setOf(a, b), f.current.allItems.map { (it as AppRef).resolve(installed) }.toSet())
            val back = LayoutFile.parse(f.toJson())
            assertEquals(f, back)
            assertEquals(listOf(ref(home), ref(dock)), back.current.allItems)
        }
    }

    /**
     * lor-bdus's review, R2: a start stack of only `dock` beside five pages. Its row has no room
     * for a sixth page, so it stays as a clock and the dock app joins the first page with room.
     */
    @Test
    fun aDockOnlyStartBesideFivePagesStaysWithinTheRowsLimit() {
        val pages = (1..5).joinToString { c -> stackAt(c, appsOf("p.p$c")) }
        for (start in listOf("""{"narrow": [0, 0]}""", """{"narrow": [1, 0], "wide": [0, 0]}""")) {
            val text = """{"version": 2, "active": "a", "layouts": [{"name": "a", "start": $start, "dock": ["d.d"], "panels": [
                ${stackAt(0, dockBlock)}, $pages]}]}"""
            val f = LayoutFile.parse(text)
            assertEquals(start, listOf(StackBlock.Clock), blocks(f, 0))
            assertEquals(start, listOf(ref("p.p1"), ref("d.d")), f.current.items(BlockAt(Cell(1, 0), 0)))
            assertEquals(start, 5, f.current.panels.count { it.hasApps })
            assertEquals(start, f, LayoutFile.parse(f.toJson()))
        }
        // With room in the row, it is an empty page, as before, and takes the dock app first.
        val four = (1..4).joinToString { c -> stackAt(c, appsOf("p.p$c")) }
        val roomy = LayoutFile.parse("""{"version": 2, "active": "a", "layouts": [{"name": "a", "start": {"narrow": [0, 0]}, "dock": ["d.d"], "panels": [
            ${stackAt(0, dockBlock)}, $four]}]}""")
        assertEquals(listOf(StackBlock.Apps(listOf(ref("d.d")))), blocks(roomy, 0))
        assertEquals(roomy, LayoutFile.parse(roomy.toJson()))
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
