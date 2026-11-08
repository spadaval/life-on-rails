package com.spadaval.lor.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.Instant

class LayoutTest {
    private val full = """
        {
          "version": 2,
          "active": "trip",
          "layouts": [
            { "name": "default",
              "start": { "narrow": [1, 0], "wide": [2, 0] },
              "dock": ["com.google.android.dialer", "com.google.android.apps.messaging"],
              "panels": [
                { "at": [0, 0], "type": "t3" },
                { "at": [1, 0], "type": "stack", "blocks": [
                  { "type": "clock" },
                  { "type": "apps", "items": ["com.whatsapp", "com.android.chrome/com.google.android.apps.chrome.Main",
                    { "folder": "Dev", "apps": ["com.github.android", "org.example/.Term"],
                      "reason": "You open these together.", "pinned": true }] },
                  { "type": "dock" }] },
                { "at": [2, 0], "type": "stack", "blocks": [{ "type": "apps" }, { "type": "dock" }] },
                { "at": [1, 1], "type": "drawer" }] },
            { "name": "trip", "expires": "2026-11-18T00:00:00-08:00",
              "start": { "narrow": [0, 0] },
              "panels": [{ "at": [0, 0], "type": "stack", "blocks": [{ "type": "apps", "items": ["com.ubercab"] }] }] }
          ]
        }
    """.trimIndent()

    private fun error(text: String): String = try {
        LayoutFile.parse(text)
        fail("parsed: $text")
        ""
    } catch (e: LayoutError) {
        e.message!!
    }

    private fun one(layout: String) = """{"version": 2, "active": "a", "layouts": [$layout]}"""

    /** A layout "a" of [panels], starting at [start]. */
    private fun panels(panels: String, start: String = """{"narrow": [0, 0]}""") = one("""{"name": "a", "start": $start, "panels": [$panels]}""")

    /** A layout "a" with one stack at [0, 0] whose blocks are [blocks]. */
    private fun stack(blocks: String) = panels("""{"at": [0, 0], "type": "stack", "blocks": $blocks}""")

    /** A layout "a" whose one stack holds an apps block of [items]. */
    private fun apps(items: String) = stack("""[{"type": "apps", "items": $items}]""")

    private val t3 = """{"at": [0, 0], "type": "t3"}"""

    @Test
    fun parsesEveryPart() {
        val file = LayoutFile.parse(full)
        assertEquals("trip", file.current.name)
        val default = file.layouts[0]
        assertEquals(listOf(Panel.T3(Cell(0, 0)), Panel.Drawer(Cell(1, 1))), default.panels.filter { it !is Panel.Stack })
        assertEquals(Start(Cell(1, 0), Cell(2, 0)), default.start)
        assertEquals(listOf(StackBlock.Clock, StackBlock.Apps(default.home), StackBlock.Dock), (default.panelAt(Cell(1, 0)) as Panel.Stack).blocks)
        assertEquals(listOf(StackBlock.Apps(emptyList()), StackBlock.Dock), (default.panelAt(Cell(2, 0)) as Panel.Stack).blocks)
        assertEquals(listOf(HOME_APPS, BlockAt(Cell(2, 0), 0)), default.pages)
        assertEquals(Start(Cell(0, 0)), file.layouts[1].start)
        val home = default.home
        assertEquals(AppRef("com.whatsapp"), home[0])
        assertEquals(AppRef("com.android.chrome", "com.google.android.apps.chrome.Main"), home[1])
        val dev = home[2] as Folder
        assertEquals("Dev", dev.name)
        assertEquals(listOf(AppRef("com.github.android"), AppRef("org.example", "org.example.Term")), dev.apps)
        assertEquals("You open these together.", dev.reason)
        assertTrue(dev.pinned)
        assertEquals(2, default.dock.size)
        assertNull(default.expires)
        assertEquals(Instant.parse("2026-11-18T08:00:00Z"), file.layouts[1].expires)
        assertEquals(emptyList<AppRef>(), file.layouts[1].dock)
        assertEquals(listOf(AppRef("com.ubercab")), file.layouts[1].items(BlockAt(Cell(0, 0), 0)))
    }

    @Test
    fun roundTripsThroughJson() {
        val file = LayoutFile.parse(full)
        assertEquals(file, LayoutFile.parse(file.toJson()))
        val plain = LayoutFile("a", listOf(Layout.grid("a", listOf(Folder("F", listOf(AppRef("a.b")))), emptyList())))
        assertEquals(plain, LayoutFile.parse(plain.toJson()))
        assertFalse("defaults are left out", plain.toJson().contains("pinned"))
        assertFalse("wide is left out when unset", plain.toJson().contains("wide"))
        assertTrue("the phone writes version 2", plain.toJson().contains("\"version\": 2"))
    }

    @Test
    fun refsMatchPackageOrActivity() {
        assertTrue(AppRef("a.b").matches("a.b", "a.b.Main"))
        assertTrue(AppRef("a.b", "a.b.Main").matches("a.b", "a.b.Main"))
        assertFalse(AppRef("a.b", "a.b.Other").matches("a.b", "a.b.Main"))
        assertFalse(AppRef("a.b").matches("a.bc", "a.bc.Main"))
        assertNull(AppRef.parse("whatsapp"))
        assertNull(AppRef.parse("com.x/"))
        assertNull(AppRef.parse("com.x/a b"))
        assertNull(AppRef.parse(""))
    }

    @Test
    fun rejectsWhatIsWrongAndSaysWhere() {
        assertTrue(error("{\"version\": 2,").startsWith("not JSON"))
        assertTrue(error("not json").contains("JSON object"))
        assertTrue(error("[]").contains("JSON object"))
        assertEquals("version: must be 2 (or 1, which is converted), got 3", error("""{"version": 3, "active": "a", "layouts": []}"""))
        assertEquals("version: must be 2 (or 1, which is converted), got nothing", error("""{"active": "a", "layouts": []}"""))
        assertEquals("layouts: must hold at least one layout", error("""{"version": 2, "active": "a", "layouts": []}"""))
        val b = """{"name": "b", "start": {"narrow": [0, 0]}, "panels": [$t3]}"""
        val a = b.replace("\"b\"", "\"a\"")
        assertEquals("active: no layout is named \"a\"", error("""{"version": 2, "active": "a", "layouts": [$b]}"""))
        assertEquals("layouts: the name \"a\" is used twice", error("""{"version": 2, "active": "a", "layouts": [$a, $a]}"""))
        assertTrue(error("""{"version": 2, "active": "a", "layouts": [$a], "extra": 1}""").startsWith("unknown field \"extra\""))
        assertTrue(error(one("""{"name": "a", "folders": []}""")).startsWith("layouts[0]: unknown field \"folders\""))
        assertTrue("version 1's fields are unknown in version 2", error(one("""{"name": "a", "home": []}""")).startsWith("layouts[0]: unknown field \"home\""))
        assertEquals("layouts[0].name: must not be empty", error(one("""{"name": " "}""")))
        assertEquals("layouts[0].panels[0].blocks[0].items[1]: \"WhatsApp\" is not a package name or package/activity",
            error(apps("""["com.x", "WhatsApp"]""")))
        assertEquals("layouts[0].panels[0].blocks[0].items[0]: must be an app (a package name) or a folder object", error(apps("[3]")))
        assertEquals("layouts[0].panels[0].blocks[0].items[0]: an object here must be a folder (\"folder\": name)", error(apps("""[{"app": "com.x"}]""")))
        assertEquals("layouts[0].panels[0].blocks[0].items[0].apps: a folder needs at least one app", error(apps("""[{"folder": "F", "apps": []}]""")))
        // Folders don't nest.
        assertTrue(error(apps("""[{"folder": "F", "apps": [{"folder": "G", "apps": ["a.b"]}]}]"""))
            .startsWith("layouts[0].panels[0].blocks[0].items[0].apps[0]: "))
        assertEquals("layouts[0].panels[0].blocks[0].items[0].pinned: must be true or false", error(apps("""[{"folder": "F", "apps": ["a.b"], "pinned": "yes"}]""")))
        assertEquals("layouts[0].panels[0].blocks[0].items[0].reason: must be a string", error(apps("""[{"folder": "F", "apps": ["a.b"], "reason": 1}]""")))
        assertEquals("layouts[0].dock[0]: \"{\"folder\":\"F\"}\" is not a package name or package/activity", error(one("""{"name": "a", "dock": [{"folder": "F"}]}""")))
        assertEquals("layouts[0].dock: at most 5 apps, got 6", error(one("""{"name": "a", "dock": ["a.a", "a.b", "a.c", "a.d", "a.e", "a.f"]}""")))
        val many = (1..25).joinToString { "\"a.a$it\"" }
        assertEquals("layouts[0].panels[0].blocks[0].items: at most 24 items, got 25", error(apps("[$many]")))
        assertEquals("layouts[0].expires: \"2026-11-18\" is not a date-time like 2026-11-18T00:00:00Z", error(one("""{"name": "a", "expires": "2026-11-18"}""")))
        assertEquals("layouts[0].panels[0].blocks[0].items: must be a list", error(apps("\"com.x\"")))
    }

    @Test
    fun wideOnEveryPanelType() {
        val types = mapOf(
            "stack" to """, "blocks": [{"type": "apps"}]""",
            "t3" to "",
            "drawer" to "",
        )
        val defaults = mapOf("stack" to Wide.HALF, "t3" to Wide.FULL, "drawer" to Wide.HALF)
        for ((type, rest) in types) {
            fun panel(wide: String?) = panels("""$t3, {"at": [1, 0], "type": "$type"${if (wide == null) "" else ", \"wide\": $wide"}$rest}""")
            fun wideOf(wide: String?) = LayoutFile.parse(panel(wide)).current.panelAt(Cell(1, 0))!!.wide
            assertEquals(type, Wide.FULL, wideOf("\"full\""))
            assertEquals(type, Wide.HALF, wideOf("\"half\""))
            assertEquals("$type's default", defaults[type], wideOf(null))
            assertEquals(type, "layouts[0].panels[1].wide: \"Full\" is not \"full\" or \"half\"", error(panel("\"Full\"")))
            assertEquals(type, "layouts[0].panels[1].wide: \"\" is not \"full\" or \"half\"", error(panel("\"\"")))
            for (bad in listOf("1", "true", "null", "[\"full\"]")) {
                assertEquals("$type $bad", "layouts[0].panels[1].wide: must be \"full\" or \"half\"", error(panel(bad)))
            }
        }
    }

    @Test
    fun wideIsWrittenOnlyWhenItDiffersFromTheTypesDefault() {
        val file = LayoutFile.parse(
            panels(
                """{"at": [0, 0], "type": "t3", "wide": "half"}, {"at": [1, 0], "type": "stack", "wide": "full", "blocks": [{"type": "apps"}]},
                {"at": [2, 0], "type": "stack", "wide": "half", "blocks": [{"type": "apps"}]}, {"at": [3, 0], "type": "t3", "wide": "full"},
                {"at": [0, 1], "type": "drawer", "wide": "half"}""",
            ),
        )
        val json = file.toJson()
        assertEquals("only the two that differ", 2, Regex("\"wide\"").findAll(json).count())
        assertTrue(json.contains("\"wide\": \"half\"") && json.contains("\"wide\": \"full\""))
        assertEquals(file, LayoutFile.parse(json))
        assertEquals(Wide.HALF, file.current.panelAt(Cell(0, 0))!!.wide)
        assertEquals(Wide.FULL, file.current.panelAt(Cell(1, 0))!!.wide)
    }

    @Test
    fun rejectsWhatIsWrongInTheGridAndSaysWhere() {
        // panels
        assertEquals("layouts[0].panels: must be a list", error(one("""{"name": "a", "start": {"narrow": [0, 0]}}""")))
        assertEquals("layouts[0].panels: must be a list", error(one("""{"name": "a", "start": {"narrow": [0, 0]}, "panels": {}}""")))
        assertEquals("layouts[0].panels[0]: must be an object", error(panels("3")))
        assertEquals("layouts[0].panels[0].type: must be a string", error(panels("""{"at": [0, 0]}""")))
        assertEquals("layouts[0].panels[0].type: must be one of stack, t3, drawer, got \"widget\"", error(panels("""{"at": [0, 0], "type": "widget"}""")))
        assertEquals("layouts[0].panels[0]: unknown field \"size\" (allowed: at, type, wide)", error(panels("""{"at": [0, 0], "type": "t3", "size": "full"}""")))
        assertEquals("layouts[0].panels[0]: unknown field \"blocks\" (allowed: at, type, wide)", error(panels("""{"at": [0, 0], "type": "drawer", "blocks": []}""")))
        // at
        for (bad in listOf("[0]", "[0, 0, 0]", "[-1, 0]", "[0.5, 0]", "[\"0\", 0]", "\"0,0\"", """{"column": 0}""")) {
            assertEquals(bad, "layouts[0].panels[0].at: must be [column, row], two whole numbers from 0", error(panels("""{"at": $bad, "type": "t3"}""")))
        }
        assertEquals("layouts[0].panels[0].at: must be [column, row], two whole numbers from 0", error(panels("""{"type": "t3"}""")))
        assertEquals("layouts[0].panels[1].at: [0, 0] is taken by panels[0]", error(panels("""$t3, {"at": [0, 0], "type": "drawer"}""")))
        // blocks
        assertEquals("layouts[0].panels[0].blocks: must be a list", error(panels("""{"at": [0, 0], "type": "stack"}""")))
        assertEquals("layouts[0].panels[0].blocks: a stack needs at least one block", error(stack("[]")))
        assertEquals("layouts[0].panels[0]: unknown field \"items\" (allowed: at, type, wide, blocks)",
            error(panels("""{"at": [0, 0], "type": "stack", "blocks": [{"type": "dock"}], "items": []}""")))
        assertEquals("layouts[0].panels[0].blocks[1]: must be an object", error(stack("""[{"type": "clock"}, "apps"]""")))
        assertEquals("layouts[0].panels[0].blocks[1].type: must be a string", error(stack("""[{"type": "clock"}, {"items": []}]""")))
        assertEquals("layouts[0].panels[0].blocks[1].type: must be one of clock, apps, dock, widget, got \"today\"", error(stack("""[{"type": "clock"}, {"type": "today"}]""")))
        assertEquals("layouts[0].panels[0].blocks[1]: unknown field \"items\" (allowed: type)", error(stack("""[{"type": "clock"}, {"type": "dock", "items": []}]""")))
        assertEquals("layouts[0].panels[0].blocks[0]: unknown field \"style\" (allowed: type)", error(stack("""[{"type": "clock", "style": "date"}]""")))
        assertEquals("layouts[0].panels[0].blocks[2]: a stack holds one \"apps\" block", error(stack("""[{"type": "apps"}, {"type": "dock"}, {"type": "apps"}]""")))
        // start
        assertEquals("layouts[0].start: must be an object", error(one("""{"name": "a", "panels": [$t3]}""")))
        assertEquals("layouts[0].start: must be an object", error(panels(t3, start = "[0, 0]")))
        assertEquals("layouts[0].start.narrow: must be [column, row], two whole numbers from 0", error(panels(t3, start = """{"wide": [0, 0]}""")))
        assertEquals("layouts[0].start.narrow: no panel is at [1, 0]", error(panels(t3, start = """{"narrow": [1, 0]}""")))
        assertEquals("layouts[0].start.wide: no panel is at [0, 1]", error(panels(t3, start = """{"narrow": [0, 0], "wide": [0, 1]}""")))
        assertEquals("layouts[0].start: unknown field \"cover\" (allowed: narrow, wide)", error(panels(t3, start = """{"narrow": [0, 0], "cover": [0, 0]}""")))
        // A row holds at most 5 stacks with an apps block; other panels and other rows don't count.
        fun appsAt(c: Int, r: Int) = """{"at": [$c, $r], "type": "stack", "blocks": [{"type": "apps"}]}"""
        val five = (0..4).joinToString { appsAt(it, 0) }
        LayoutFile.parse(panels("""$five, {"at": [5, 0], "type": "t3"}, {"at": [6, 0], "type": "stack", "blocks": [{"type": "clock"}]}, ${appsAt(0, 1)}"""))
        assertEquals("layouts[0].panels: row 0 holds 6 stacks with an apps block, at most 5", error(panels("$five, ${appsAt(9, 0)}")))
    }

    @Test
    fun rejectsTrailingContent() {
        val ok = panels(t3)
        LayoutFile.parse("$ok \n\t ")
        assertEquals("not JSON: more after the closing }", error("$ok x"))
        assertEquals("not JSON: more after the closing }", error("$ok {}"))
    }

    @Test
    fun rejectsAnOversizedFile() {
        val padded = panels(t3) + " ".repeat(LayoutFile.MAX_BYTES.toInt())
        assertEquals("the file is over 256 KB", error(padded))
    }

    @Test
    fun rejectsDeepNestingWithoutCrashing() {
        // Android's org.json recurses without a limit, and parse catches its StackOverflowError.
        // The JVM can't test that catch: its org.json stops at depth 512, and below that it catches
        // a StackOverflowError itself (JSONTokener.nextValue) and throws a JSONException, even on a
        // thread with a 16 KB stack. So this checks only that deep nesting is "not JSON", not a crash.
        val deep = "[".repeat(100_000) + "]".repeat(100_000)
        assertTrue(error("""{"version": 2, "active": "a", "layouts": $deep}""").startsWith("not JSON"))
    }

    /** The layout lock's field (lor-v692, decision lor-73li). */
    @Test
    fun lockedIsTrueOrFalseAndWrittenOnlyWhenTrue() {
        val locked = LayoutFile.parse(v2Empty(""""locked": true,"""))
        assertTrue(locked.locked)
        assertTrue(locked.toJson().contains("\"locked\": true"))
        val unlocked = LayoutFile.parse(v2Empty(""""locked": false,"""))
        assertFalse(unlocked.locked)
        assertFalse("false is read, then dropped at the next write", unlocked.toJson().contains("locked"))
        assertEquals(unlocked, LayoutFile.parse(unlocked.toJson()))
        assertEquals("locked: must be true or false", error(v2Empty(""""locked": "yes",""")))
        assertEquals("locked: must be true or false", error(v2Empty(""""locked": 1,""")))
        assertFalse("absent is false", LayoutFile.parse(v2Empty()).locked)
        // A locked file writes back byte for byte the same after parse and toJson.
        val text = locked.toJson()
        assertEquals(text, LayoutFile.parse(text).toJson())
    }
}
