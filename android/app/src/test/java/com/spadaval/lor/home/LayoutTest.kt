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

    private fun error(text: String): String = try {
        LayoutFile.parse(text)
        fail("parsed: $text")
        ""
    } catch (e: LayoutError) {
        e.message!!
    }

    private fun one(layout: String) = """{"version": 1, "active": "a", "layouts": [$layout]}"""

    @Test
    fun parsesEveryPart() {
        val file = LayoutFile.parse(full)
        assertEquals("trip", file.current.name)
        val home = file.layouts[0].home
        assertEquals(AppRef("com.whatsapp"), home[0])
        assertEquals(AppRef("com.android.chrome", "com.google.android.apps.chrome.Main"), home[1])
        val dev = home[2] as Folder
        assertEquals("Dev", dev.name)
        assertEquals(listOf(AppRef("com.github.android"), AppRef("org.example", "org.example.Term")), dev.apps)
        assertEquals("You open these together.", dev.reason)
        assertTrue(dev.pinned)
        assertEquals(2, file.layouts[0].dock.size)
        assertNull(file.layouts[0].expires)
        assertEquals(Instant.parse("2026-11-18T08:00:00Z"), file.layouts[1].expires)
        assertEquals(emptyList<AppRef>(), file.layouts[1].dock)
    }

    @Test
    fun roundTripsThroughJson() {
        val file = LayoutFile.parse(full)
        assertEquals(file, LayoutFile.parse(file.toJson()))
        val plain = LayoutFile("a", listOf(Layout("a", listOf(Folder("F", listOf(AppRef("a.b")))), emptyList())))
        assertEquals(plain, LayoutFile.parse(plain.toJson()))
        assertFalse("defaults are left out", plain.toJson().contains("pinned"))
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
        assertTrue(error("{\"version\": 1,").startsWith("not JSON"))
        assertTrue(error("not json").contains("JSON object"))
        assertTrue(error("[]").contains("JSON object"))
        assertEquals("version: must be 1, got 2", error("""{"version": 2, "active": "a", "layouts": []}"""))
        assertEquals("version: must be 1, got nothing", error("""{"active": "a", "layouts": []}"""))
        assertEquals("layouts: must hold at least one layout", error("""{"version": 1, "active": "a", "layouts": []}"""))
        assertEquals("active: no layout is named \"a\"", error("""{"version": 1, "active": "a", "layouts": [{"name": "b"}]}"""))
        assertEquals("layouts: the name \"a\" is used twice", error("""{"version": 1, "active": "a", "layouts": [{"name": "a"}, {"name": "a"}]}"""))
        assertTrue(error("""{"version": 1, "active": "a", "layouts": [{"name": "a"}], "extra": 1}""").startsWith("unknown field \"extra\""))
        assertTrue(error(one("""{"name": "a", "folders": []}""")).startsWith("layouts[0]: unknown field \"folders\""))
        assertEquals("layouts[0].name: must not be empty", error(one("""{"name": " "}""")))
        assertEquals("layouts[0].home[1]: \"WhatsApp\" is not a package name or package/activity",
            error(one("""{"name": "a", "home": ["com.x", "WhatsApp"]}""")))
        assertEquals("layouts[0].home[0]: must be an app (a package name) or a folder object", error(one("""{"name": "a", "home": [3]}""")))
        assertEquals("layouts[0].home[0]: an object here must be a folder (\"folder\": name)", error(one("""{"name": "a", "home": [{"app": "com.x"}]}""")))
        assertEquals("layouts[0].home[0].apps: a folder needs at least one app", error(one("""{"name": "a", "home": [{"folder": "F", "apps": []}]}""")))
        // Folders don't nest.
        assertTrue(error(one("""{"name": "a", "home": [{"folder": "F", "apps": [{"folder": "G", "apps": ["a.b"]}]}]}"""))
            .startsWith("layouts[0].home[0].apps[0]: "))
        assertEquals("layouts[0].home[0].pinned: must be true or false", error(one("""{"name": "a", "home": [{"folder": "F", "apps": ["a.b"], "pinned": "yes"}]}""")))
        assertEquals("layouts[0].home[0].reason: must be a string", error(one("""{"name": "a", "home": [{"folder": "F", "apps": ["a.b"], "reason": 1}]}""")))
        assertEquals("layouts[0].dock[0]: \"{\"folder\":\"F\"}\" is not a package name or package/activity", error(one("""{"name": "a", "dock": [{"folder": "F"}]}""")))
        assertEquals("layouts[0].dock: at most 5 apps, got 6", error(one("""{"name": "a", "dock": ["a.a", "a.b", "a.c", "a.d", "a.e", "a.f"]}""")))
        val many = (1..25).joinToString { "\"a.a$it\"" }
        assertEquals("layouts[0].home: at most 24 items, got 25", error(one("""{"name": "a", "home": [$many]}""")))
        assertEquals("layouts[0].expires: \"2026-11-18\" is not a date-time like 2026-11-18T00:00:00Z", error(one("""{"name": "a", "expires": "2026-11-18"}""")))
        assertEquals("layouts[0].home: must be a list", error(one("""{"name": "a", "home": "com.x"}""")))
    }
}
