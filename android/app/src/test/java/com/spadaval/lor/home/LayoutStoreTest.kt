package com.spadaval.lor.home

import org.junit.Assert.assertEquals
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

    private val one = LayoutFile("default", listOf(Layout.grid("default", listOf(AppRef("com.whatsapp")), listOf(AppRef("com.android.chrome")))))
    private val two = one.copy(layouts = listOf(Layout.grid("default", listOf(AppRef("org.example")), emptyList())))
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
        store.edit { it.addToDock(AppId("com.whatsapp", "com.whatsapp.Main"), AppRef("com.whatsapp")) }.getOrThrow()
        assertEquals(listOf(AppRef("com.whatsapp")), store.readGood()!!.current.dock)
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
        val undo = store.edit { it.addToDock(AppId("org.example", "org.example.Main"), AppRef("org.example")) }.getOrThrow().undo!!
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

    private fun undoFails(store: LayoutStore, undo: LayoutStore.Undo): String = try {
        store.undo(undo)
        fail("undo")
        ""
    } catch (e: LayoutError) {
        e.message!!
    }
}
