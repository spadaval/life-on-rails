package com.spadaval.lor.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.Instant

/**
 * A version 1 file is read as the default grid, and the phone's next write is version 2
 * (lor-euec). The fixtures are the version 1 files the tests used before version 2.
 */
class LayoutV1Test {
    @get:Rule
    val tmp = TemporaryFolder()

    /** LayoutTest's file. */
    private val full = """
        {
          "version": 1,
          "active": "trip",
          "layouts": [
            { "name": "default",
              "home": ["com.whatsapp", "com.android.chrome/com.google.android.apps.chrome.Main",
                       { "folder": "Dev", "apps": ["com.github.android", "org.example/.Term"],
                         "reason": "You open these together.", "pinned": true }],
              "dock": ["com.google.android.dialer", "com.google.android.apps.messaging"] },
            { "name": "trip", "expires": "2026-11-18T00:00:00-08:00", "home": ["com.ubercab"] }
          ]
        }
    """.trimIndent()

    /** LayoutEditTest's file, with hidden, drawer and look at the top. */
    private val edits = """
        {
          "version": 1,
          "active": "default",
          "hidden": ["com.x"],
          "drawer": { "view": "az", "keyboard": true },
          "look": { "header": "clock" },
          "layouts": [
            { "name": "default",
              "expires": "2026-11-18T00:00:00Z",
              "home": ["com.whatsapp",
                       { "folder": "Dev", "apps": ["com.github.android", "org.example/.Term"], "reason": "Together." },
                       { "folder": "Solo", "apps": ["com.github.android"] }],
              "dock": ["com.google.android.dialer"] },
            { "name": "trip", "home": ["com.whatsapp", { "folder": "Dev", "apps": ["com.whatsapp", "a.b"] }], "dock": ["com.whatsapp"] }
          ]
        }
    """.trimIndent()

    /** HomeEditTest's pages and cover, with an empty page. */
    private val pages = """{"version": 1, "active": "a", "layouts": [{"name": "a", "home": ["a.a"],
        "pages": [{"items": ["b.b", {"folder": "F", "apps": ["c.c", "d.d/.X"], "cover": "d.d/d.d.X"}]}, {}]}]}"""

    /** LayoutStoreTest's hand-formatted file. */
    private val hand = "{\n    \"layouts\": [{\"home\": [\"com.whatsapp\", \"com.android.chrome\\/.Main\"], \"name\": \"default\"}],\n    \"version\": 1, \"active\": \"default\"\n}"

    /** The smallest: a layout with nothing in it. */
    private val empty = """{"version": 1, "active": "a", "layouts": [{"name": "a"}]}"""

    private fun ref(s: String) = AppRef.parse(s)!!

    /** The default grid, written out here rather than taken from [Layout.grid]. */
    private fun expected(
        name: String,
        home: List<LayoutItem>,
        dock: List<AppRef> = emptyList(),
        pages: List<List<LayoutItem>> = emptyList(),
        expires: Instant? = null,
    ) = Layout(
        name,
        listOf(
            Panel.T3(Cell(0, 0)),
            Panel.Stack(Cell(1, 0), listOf(StackBlock.Clock, StackBlock.Apps(home), StackBlock.Dock)),
        ) + pages.mapIndexed { i, items -> Panel.Stack(Cell(2 + i, 0), listOf(StackBlock.Apps(items), StackBlock.Dock)) } +
            Panel.Drawer(Cell(1, 1)),
        dock,
        Start(Cell(1, 0)),
        expires,
    )

    @Test
    fun eachFixtureConvertsToTheDefaultGrid() {
        val dev = Folder("Dev", listOf(ref("com.github.android"), ref("org.example/.Term")), "You open these together.", pinned = true)
        assertEquals(
            LayoutFile("trip", listOf(
                expected("default", listOf(ref("com.whatsapp"), ref("com.android.chrome/com.google.android.apps.chrome.Main"), dev),
                    listOf(ref("com.google.android.dialer"), ref("com.google.android.apps.messaging"))),
                expected("trip", listOf(ref("com.ubercab")), expires = Instant.parse("2026-11-18T08:00:00Z")),
            )),
            LayoutFile.parse(full),
        )
        assertEquals(
            LayoutFile(
                "default",
                listOf(
                    expected("default", listOf(
                        ref("com.whatsapp"),
                        Folder("Dev", listOf(ref("com.github.android"), ref("org.example/.Term")), "Together."),
                        Folder("Solo", listOf(ref("com.github.android"))),
                    ), listOf(ref("com.google.android.dialer")), expires = Instant.parse("2026-11-18T00:00:00Z")),
                    expected("trip", listOf(ref("com.whatsapp"), Folder("Dev", listOf(ref("com.whatsapp"), ref("a.b")))), listOf(ref("com.whatsapp"))),
                ),
                hidden = listOf(ref("com.x")),
                drawer = Drawer(DrawerView.AZ, keyboard = true),
                look = HomeLook(header = HomeLook.Header.CLOCK),
            ),
            LayoutFile.parse(edits),
        )
        assertEquals(
            LayoutFile("a", listOf(expected("a", listOf(ref("a.a")), pages = listOf(
                listOf(ref("b.b"), Folder("F", listOf(ref("c.c"), ref("d.d/.X")), cover = ref("d.d/.X"))),
                emptyList(),
            )))),
            LayoutFile.parse(pages),
        )
        assertEquals(LayoutFile("default", listOf(expected("default", listOf(ref("com.whatsapp"), ref("com.android.chrome/.Main"))))), LayoutFile.parse(hand))
        assertEquals(LayoutFile("a", listOf(expected("a", emptyList()))), LayoutFile.parse(empty))
        // Layout.grid, which the first run's default uses, makes the same grid.
        assertEquals(expected("a", listOf(ref("a.a")), listOf(ref("d.d")), listOf(listOf(ref("b.b")))), Layout.grid("a", listOf(ref("a.a")), listOf(ref("d.d")), listOf(listOf(ref("b.b")))))
    }

    @Test
    fun aConvertedFileWritesVersion2AndReadsBackTheSame() {
        for (text in listOf(full, edits, pages, hand, empty)) {
            val f = LayoutFile.parse(text)
            val json = f.toJson()
            assertTrue(json.contains("\"version\": 2"))
            assertFalse("no version 1 field is written", Regex("\"(home|pages)\"").containsMatchIn(json))
            assertEquals(f, LayoutFile.parse(json))
        }
    }

    @Test
    fun version1ErrorsNameVersion1Places() {
        fun error(layout: String): String = try {
            LayoutFile.parse("""{"version": 1, "active": "a", "layouts": [{"name": "a", $layout}]}""")
            fail("parsed")
            ""
        } catch (e: LayoutError) {
            e.message!!
        }
        assertEquals("layouts[0].home[1]: \"WhatsApp\" is not a package name or package/activity", error(""""home": ["com.x", "WhatsApp"]"""))
        assertEquals("layouts[0].pages: at most 4 pages after home, got 5", error(""""pages": [{}, {}, {}, {}, {}]"""))
        val many = (1..25).joinToString { "\"p.p$it\"" }
        assertEquals("layouts[0].pages[0].items: at most 24 items, got 25", error(""""pages": [{"items": [$many]}]"""))
        assertEquals("layouts[0].pages[0]: unknown field \"home\" (allowed: items)", error(""""pages": [{"home": []}]"""))
        assertTrue("version 2's fields are unknown in version 1", error(""""panels": []""").startsWith("layouts[0]: unknown field \"panels\""))
    }

    @Test
    fun theLastGoodCopyAndTheRestoreTakeAVersion1File() {
        // Launcher polish 3.1: layout.good.json holds version 1 text from before the update.
        val store = LayoutStore(tmp.root.resolve("layout.json"))
        store.good.writeText(full)
        store.file.writeText("""{ "version": 2, "active": "a", "layouts": [] }""")
        val good = store.readGood()
        assertEquals(LayoutFile.parse(full), good)
        store.restore(good!!)
        val written = store.file.readText()
        assertTrue("the restore writes version 2", written.contains("\"version\": 2"))
        assertEquals(good, store.read(null).layout)
        assertEquals("""{ "version": 2, "active": "a", "layouts": [] }""", store.broken.readText())
    }

    @Test
    fun aVersion1FileIsReadAndThePhonesNextWriteIsVersion2() {
        val store = LayoutStore(tmp.root.resolve("layout.json"))
        store.file.writeText(edits)
        val read = store.read(null)
        assertEquals(LayoutFile.parse(edits), read.layout)
        assertEquals("a read leaves the file as it is", edits, store.file.readText())
        assertEquals("the last good copy takes version 1 text", edits, store.good.readText())
        val edited = store.edit { it.addToDock(AppId("org.example", "org.example.Main"), AppRef("org.example")) }.getOrThrow()
        val text = store.file.readText()
        assertTrue(text.contains("\"version\": 2"))
        assertEquals(edited.layout, LayoutFile.parse(text))
        // Undo puts the version 1 text back, byte for byte, and it still reads.
        store.undo(edited.undo!!)
        assertEquals(edits, store.file.readText())
        assertEquals(read.layout, store.read(null).layout)
    }
}
