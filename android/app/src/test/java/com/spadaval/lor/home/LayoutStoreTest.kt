package com.spadaval.lor.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** The last good layout and the restore (launcher polish 3.1). */
class LayoutStoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val one = LayoutFile("default", listOf(Layout.grid("default", listOf(AppRef("com.whatsapp")))))
    private val two = one.copy(layouts = listOf(Layout.grid("default", listOf(AppRef("org.example")))))
    private val brokenText = """{ "version": 2, "active": "default", "layouts": [{ "name": "default", "panles": [] }] }"""

    private fun store() = LayoutStore(tmp.root.resolve("layout.json"))

    private fun readFails(store: LayoutStore): String = try {
        store.read(null)
        fail("read")
        ""
    } catch (e: LayoutError) {
        e.message!!
    }

    @Test
    fun aReadThatParsesKeepsTheCopyOnce() {
        val store = store()
        store.file.writeText(one.toJson())
        store.read(null)
        assertEquals(one.toJson(), store.good.readText())
        // Reading the same text again leaves the copy alone.
        val old = 1_000_000_000_000L
        assertTrue(store.good.setLastModified(old))
        store.read(null)
        assertEquals(old, store.good.lastModified())
    }

    @Test
    fun aWriteUpdatesTheCopy() {
        val store = store()
        store.write(one)
        store.write(two)
        assertEquals(two, store.readGood())
        store.edit { it.addToHome(AppId("com.whatsapp", "com.whatsapp.Main"), AppRef("com.whatsapp")) }.getOrThrow()
        assertEquals(listOf(AppRef("org.example"), AppRef("com.whatsapp")), store.readGood()!!.current.allItems)
    }

    @Test
    fun aBrokenFileLeavesTheLastGoodLayout() {
        val store = store()
        store.write(one)
        store.file.writeText(brokenText)
        assertTrue(readFails(store).contains("panles"))
        assertEquals(one, store.readGood())
        // An edit refuses with the same error and changes nothing.
        assertTrue(store.edit { it }.exceptionOrNull()!!.message!!.contains("panles"))
        assertEquals(brokenText, store.file.readText())
    }

    @Test
    fun aCorruptOrMissingCopyIsNoLayout() {
        val store = store()
        assertNull(store.readGood())
        store.good.writeText("{ not json")
        assertNull(store.readGood())
    }

    @Test
    fun restoreSetsTheBrokenFileAsideAndWritesTheFallback() {
        val store = store()
        store.write(one)
        store.file.writeText(brokenText)
        store.broken.writeText("an older broken file")
        val stamp = store.restore(store.readGood()!!)
        assertEquals(brokenText, store.broken.readText())
        assertEquals(stamp, store.file.lastModified())
        assertEquals(one, store.read(null).layout)
    }

    @Test
    fun restoreWithNoFileJustWrites() {
        val store = store()
        store.restore(two)
        assertEquals(two, store.read(null).layout)
        assertTrue(!store.broken.exists())
    }

    /** Undo (launcher polish 4.2). A hand-formatted file: four spaces, keys out of order, an escaped slash, no final newline. */
    private val handText = "{\n    \"layouts\": [{\"panels\": [{\"type\": \"stack\", \"blocks\": [{\"type\": \"clock\"}, {\"items\": [\"com.whatsapp\", \"com.android.chrome\\/.Main\"], \"type\": \"apps\"}], \"at\": [1,0]}],\n" +
        "      \"start\": {\"narrow\": [1,0]}, \"name\": \"default\"}],\n    \"version\": 2, \"active\": \"default\"\n}"

    @Test
    fun undoPutsTheFileBackByteForByte() {
        val store = store()
        store.file.writeText(handText)
        val edited = store.edit { it.removeFromHome(AppId("com.whatsapp", "com.whatsapp.Main")) }.getOrThrow()
        val undo = edited.undo!!
        assertEquals(handText, undo.before)
        assertEquals(store.file.readText(), undo.wrote)
        val back = store.undo(undo)
        assertEquals(handText, store.file.readText())
        assertEquals(handText, store.good.readText())
        assertEquals(LayoutFile.parse(handText), back.layout)
        assertEquals("a second undo refuses", "layout.json changed since; nothing was undone", undoFails(store, undo))
    }

    @Test
    fun undoRefusesAfterAnotherWrite() {
        val store = store()
        store.write(one)
        val undo = store.edit { it.addToHome(AppId("org.example", "org.example.Main"), AppRef("org.example")) }.getOrThrow().undo!!
        store.file.writeText(two.toJson())
        assertEquals("layout.json changed since; nothing was undone", undoFails(store, undo))
        assertEquals(two.toJson(), store.file.readText())
    }

    @Test
    fun anEditThatChangesNothingWritesNothing() {
        val store = store()
        store.file.writeText(handText)
        store.file.setLastModified(1_000_000L)
        val read = store.edit { it }.getOrThrow()
        assertNull(read.undo)
        assertEquals(handText, store.file.readText())
        assertEquals(1_000_000L, store.file.lastModified())
    }

    /** A t3 set to half beside a stack set to full (lor-j87u): `wide` survives an edit and its Undo. */
    @Test
    fun aNonDefaultWideKeepsThroughAnEditAndUndo() {
        val text = """{"version": 2, "active": "a", "layouts": [{"name": "a", "start": {"narrow": [1, 0]}, "panels": [
            {"at": [0, 0], "type": "t3", "wide": "half"},
            {"at": [1, 0], "type": "stack", "wide": "full", "blocks": [{"type": "apps", "items": ["com.whatsapp"]}]}]}]}"""
        val store = store()
        store.file.writeText(text)
        fun wides(file: LayoutFile) = file.current.panels.map { it.wide }
        assertEquals(listOf(Wide.HALF, Wide.FULL), wides(store.read(null).layout))
        val edited = store.edit { it.removeFromHome(AppId("com.whatsapp", "com.whatsapp.Main")) }.getOrThrow()
        val written = store.file.readText()
        assertTrue(written.contains("\"wide\": \"half\"") && written.contains("\"wide\": \"full\""))
        assertEquals(listOf(Wide.HALF, Wide.FULL), wides(LayoutFile.parse(written)))
        assertEquals(emptyList<LayoutItem>(), LayoutFile.parse(written).current.items(BlockAt(Cell(1, 0), 0)))
        store.undo(edited.undo!!)
        assertEquals(text, store.file.readText())
        assertEquals(listOf(Wide.HALF, Wide.FULL), wides(store.read(null).layout))
    }

    /**
     * The layout lock (lor-v692, decision lor-73li): while the file is locked, every phone edit that
     * changes the grid or a folder is refused and writes nothing; the rest are taken.
     */
    @Test
    fun aLockedFileRefusesEditsToHomeAndTakesTheRest() {
        fun ref(s: String) = AppRef(s)
        fun app(s: String) = AppId(s, "$s.Main")
        val folder = Folder("F", listOf(ref("b.b"), ref("c.c")))
        val open = LayoutFile(
            "default",
            listOf(Layout.grid("default", listOf(ref("a.a"), folder), listOf(listOf(ref("e.e"))))),
            hidden = listOf(ref("h.h")),
        )
        val locked = open.copy(locked = true)
        val store = store()
        store.write(locked)
        val text = store.file.readText()
        assertTrue(store.file.setLastModified(1_000_000L))
        val home = Cell(1, 0)
        val page = Cell(2, 0)
        val homeApps = BlockAt(home, 1)
        val f = From.At(Spot.Apps(homeApps, 1), folder)
        val refusals: List<Pair<String, (LayoutFile) -> LayoutFile>> = listOf(
            "addToHome" to { it.addToHome(app("z.z"), ref("z.z")) },
            "removeFromHome" to { it.removeFromHome(app("a.a")) },
            "removeFromFolder" to { it.removeFromFolder(app("b.b")) },
            "removeAt" to { it.removeAt(From.At(Spot.Apps(homeApps, 0), ref("a.a"))) },
            "hide of an app on home" to { it.hide("a.a") },
            "a drop from the drawer" to { it.drop(From.Drawer(app("z.z"), ref("z.z")), Target.AtEnd(Place.InApps(homeApps))).file },
            "a move between pages" to { it.drop(From.At(Spot.Apps(homeApps, 0), ref("a.a")), Target.AtEnd(Place.InApps(BlockAt(page, 0)))).file },
            "a folder made by a drop" to { it.drop(From.At(Spot.Apps(BlockAt(page, 0), 0), ref("e.e")), Target.Onto(Spot.Apps(homeApps, 0))).file },
            "renameFolder" to { it.renameFolder(f, "G") },
            "setCover" to { it.setCover(f, ref("c.c")) },
            "addToFolder" to { it.addToFolder(f, listOf(app("z.z") to ref("z.z"))).file },
            "addPanel" to { it.addPanel(Cell(3, 0), "stack") },
            "movePanel" to { it.movePanel(page, it.current.panelAt(page)!!, Cell(3, 0)) },
            "removePanel" to { it.removePanel(page, it.current.panelAt(page)!!) },
            "setWide" to { it.setWide(page, it.current.panelAt(page)!!, Wide.FULL) },
            "setStart" to { it.setStart(page, StartScreen.NARROW) },
            "addBlock" to { it.addBlock(page, 0, "clock") },
            "moveBlock" to { it.moveBlock(BlockAt(home, 0), StackBlock.Clock, page, 0) },
            "removeBlock" to { it.removeBlock(BlockAt(home, 0), StackBlock.Clock) },
        )
        for ((name, edit) in refusals) {
            // Each would change the unlocked file, so the refusal is the lock's.
            assertTrue(name, edit(open) != open)
            assertEquals(name, LOCKED, store.edit(edit).exceptionOrNull()?.message)
            assertEquals(name, text, store.file.readText())
            assertEquals(name, 1_000_000L, store.file.lastModified())
        }
        assertTrue("the words name the home menu", LOCKED.contains("home menu"))
        // Taken while locked: what leaves the grid alone.
        val taken: List<Pair<String, (LayoutFile) -> LayoutFile>> = listOf(
            "a drawer keyboard change" to { it.copy(drawer = it.drawer.copy(keyboard = !it.drawer.keyboard)) },
            "newTab" to { it.newTab("Work") },
            "a look change" to { it.copy(look = it.look.copy(labels = false)) },
            "unhide" to { it.unhide("h.h") },
            "moveToTab" to { it.newTab("Daily").moveToTab(app("a.a"), ref("a.a"), "Daily") },
            "hide of an app not on home" to { it.hide("q.q") },
        )
        for ((name, edit) in taken) {
            val read = store.edit(edit).getOrThrow()
            assertTrue(name, read.layout.locked)
            assertTrue(name, read.undo != null)
        }
        val unlocked = store.edit { it.setLocked(false) }.getOrThrow()
        assertFalse(unlocked.layout.locked)
        assertFalse(store.file.readText().contains("locked"))
        // Lock, then Undo: the unlocked file comes back.
        val before = store.file.readText()
        val lock = store.edit { it.setLocked(true) }.getOrThrow()
        assertTrue(store.read(null).layout.locked)
        val back = store.undo(lock.undo!!)
        assertFalse(back.layout.locked)
        assertEquals(before, store.file.readText())
    }

    private fun undoFails(store: LayoutStore, undo: LayoutStore.Undo): String = try {
        store.undo(undo)
        fail("undo")
        ""
    } catch (e: LayoutError) {
        e.message!!
    }
}
