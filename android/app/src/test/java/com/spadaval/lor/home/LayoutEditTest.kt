package com.spadaval.lor.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LayoutEditTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val wa = AppId("com.whatsapp", "com.whatsapp.Main")
    private val gh = AppId("com.github.android", "com.github.android.Main")
    private val term = AppId("org.example", "org.example.Term")
    private val dialer = AppId("com.google.android.dialer", "com.google.android.dialer.Main")

    private val file = LayoutFile.parse(
        """
        {
          "version": 1,
          "active": "default",
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
        """.trimIndent(),
    )

    private fun refused(edit: () -> LayoutFile): String = try {
        edit()
        fail("not refused")
        ""
    } catch (e: LayoutError) {
        e.message!!
    }

    private fun roundTrips(f: LayoutFile) = assertEquals(f, LayoutFile.parse(f.toJson()))

    @Test
    fun knowsWhereAnAppIs() {
        assertEquals(Places(home = true, dock = false, folder = null), file.placesOf(wa))
        assertEquals(Places(home = false, dock = false, folder = "Dev"), file.placesOf(gh))
        assertEquals(Places(home = false, dock = true, folder = null), file.placesOf(dialer))
        assertEquals(Places(home = false, dock = false, folder = null), file.placesOf(AppId("x.y", "x.y.Z")))
        // A package/activity ref names only that activity.
        assertEquals(Places(home = false, dock = false, folder = null), file.placesOf(AppId("org.example", "org.example.Other")))
    }

    @Test
    fun addsToHomeAndDock() {
        val chrome = AppId("com.android.chrome", "com.google.android.apps.chrome.Main")
        val home = file.addToHome(chrome, AppRef("com.android.chrome"))
        assertEquals(AppRef("com.android.chrome"), home.current.home.last())
        assertEquals("other layouts stay", file.layouts[1], home.layouts[1])
        roundTrips(home)
        val dock = file.addToDock(chrome, AppRef("com.android.chrome", chrome.className))
        assertEquals(listOf(AppRef("com.google.android.dialer"), AppRef("com.android.chrome", chrome.className)), dock.current.dock)
        roundTrips(dock)
    }

    @Test
    fun refusesDuplicatesAndLimits() {
        assertEquals("it's already on home", refused { file.addToHome(wa, AppRef("com.whatsapp")) })
        assertEquals("it's already on home, in Dev", refused { file.addToHome(gh, AppRef("com.github.android")) })
        assertEquals("it's already in the dock", refused { file.addToDock(dialer, AppRef("com.google.android.dialer")) })
        var full = file
        repeat(LayoutFile.MAX_HOME - file.current.home.size) { full = full.addToHome(AppId("a.a$it", "a.a$it.M"), AppRef("a.a$it")) }
        assertEquals("home is full (24 items)", refused { full.addToHome(AppId("z.z", "z.z.M"), AppRef("z.z")) })
        var dock = file
        repeat(LayoutFile.MAX_DOCK - 1) { dock = dock.addToDock(AppId("d.d$it", "d.d$it.M"), AppRef("d.d$it")) }
        assertEquals("the dock is full (5 apps)", refused { dock.addToDock(AppId("z.z", "z.z.M"), AppRef("z.z")) })
        assertEquals("it isn't on home", refused { file.removeFromHome(gh) })
        assertEquals("it isn't in the dock", refused { file.removeFromDock(wa) })
        assertEquals("it isn't in a folder", refused { file.removeFromFolder(wa) })
    }

    @Test
    fun removesFromHomeAndDock() {
        val home = file.removeFromHome(wa)
        assertFalse(home.placesOf(wa).home)
        assertEquals(2, home.current.home.size)
        assertEquals("the trip layout keeps its WhatsApp", file.layouts[1], home.layouts[1])
        assertEquals(emptyList<AppRef>(), file.removeFromDock(dialer).current.dock)
    }

    @Test
    fun removingFromAFolderPinsItAndAnEmptiedFolderGoes() {
        val once = file.removeFromFolder(gh)
        val dev = once.current.home[1] as Folder
        assertEquals(listOf(AppRef("org.example", "org.example.Term")), dev.apps)
        assertTrue("a touched folder is pinned", dev.pinned)
        assertEquals("its reason stays", "Together.", dev.reason)
        assertEquals("only the first folder holding it changes", "Solo", (once.current.home[2] as Folder).name)
        val twice = once.removeFromFolder(term)
        assertEquals(listOf("com.whatsapp", "Solo"), twice.current.home.map { (it as? Folder)?.name ?: it.toString() })
        roundTrips(twice)
    }

    @Test
    fun hidesAPackageEverywhereAndUnhides() {
        val hidden = file.hide("com.whatsapp")
        assertEquals(listOf(AppRef("com.whatsapp")), hidden.hidden)
        assertTrue(hidden.isHidden(wa))
        assertFalse(hidden.isHidden(gh))
        assertFalse(hidden.placesOf(wa).home)
        val trip = hidden.layouts[1]
        assertEquals("in every layout, from home, folders and the dock", listOf(Folder("Dev", listOf(AppRef("a.b")), pinned = true)), trip.home)
        assertEquals(emptyList<AppRef>(), trip.dock)
        assertEquals("folders without it are untouched", file.current.home[1], hidden.current.home[0])
        roundTrips(hidden)
        assertEquals("it's already hidden", refused { hidden.hide("com.whatsapp") })
        val back = hidden.unhide("com.whatsapp")
        assertEquals(emptyList<AppRef>(), back.hidden)
        assertFalse("unhiding puts nothing back", back.placesOf(wa).home)
        assertEquals("it isn't hidden", refused { file.unhide("com.whatsapp") })
    }

    @Test
    fun hiddenParsesAndRoundTrips() {
        val text = """{"version": 1, "active": "a", "hidden": ["com.x", "com.y/.Main"], "layouts": [{"name": "a"}]}"""
        val f = LayoutFile.parse(text)
        assertEquals(listOf(AppRef("com.x"), AppRef("com.y", "com.y.Main")), f.hidden)
        roundTrips(f)
        assertFalse("an empty list is left out", LayoutFile("a", listOf(Layout("a", emptyList(), emptyList()))).toJson().contains("hidden"))
        fun error(hidden: String) = try {
            LayoutFile.parse("""{"version": 1, "active": "a", "hidden": $hidden, "layouts": [{"name": "a"}]}""")
            fail("parsed")
            ""
        } catch (e: LayoutError) {
            e.message!!
        }
        assertEquals("hidden: must be a list", error("\"com.x\""))
        assertEquals("hidden[1]: \"X\" is not a package name or package/activity", error("""["com.x", "X"]"""))
        assertEquals("hidden[0]: \"3\" is not a package name or package/activity", error("[3]"))
        assertEquals("hidden: \"com.x\" is listed twice", error("""["com.x", "com.x"]"""))
        assertEquals("hidden: \"com.y/com.y.Main\" is listed twice", error("""["com.y/.Main", "com.y/com.y.Main"]"""))
    }

    @Test
    fun roundTripKeepsEveryField() {
        val f = file.hide("a.b")
        val back = LayoutFile.parse(f.toJson())
        assertEquals(f, back)
        assertEquals(file.current.expires, back.current.expires)
        assertEquals("Together.", (back.current.home[1] as Folder).reason)
        assertTrue((back.layouts[1].home[1] as Folder).pinned)
    }

    @Test
    fun storeReplacesTheFileOnlyWhenTheEditIsValid() {
        val store = LayoutStore(tmp.root.resolve("layout.json"))
        store.write(file)
        val before = store.file.readText()
        // A folder with no apps doesn't parse, so this edit must leave the file alone.
        val broken = store.edit { it.copy(layouts = it.layouts.map { l -> l.copy(home = l.home + Folder("Empty", emptyList())) }) }
        assertTrue(broken.isFailure)
        assertEquals(before, store.file.readText())
        // A refusal leaves it alone too.
        assertEquals("it's already on home", store.edit { it.addToHome(wa, AppRef("com.whatsapp")) }.exceptionOrNull()?.message)
        assertEquals(before, store.file.readText())
        val ok = store.edit { it.removeFromHome(wa) }.getOrThrow()
        assertEquals(ok.stamp, store.file.lastModified())
        assertEquals(ok.layout, LayoutFile.parse(store.file.readText()))
        assertFalse(ok.layout.placesOf(wa).home)
    }

    @Test
    fun storeEditRereadsTheFile() {
        val store = LayoutStore(tmp.root.resolve("layout.json"))
        store.write(file)
        // A hand edit between reads is what the edit applies to.
        store.file.writeText(file.removeFromDock(dialer).toJson())
        val ok = store.edit { it.addToDock(wa, AppRef("com.whatsapp")) }.getOrThrow()
        assertEquals(listOf(AppRef("com.whatsapp")), ok.layout.current.dock)
    }

    @Test
    fun noDefaultIsWrittenWithoutAnAppList() {
        val store = LayoutStore(tmp.root.resolve("layout.json"))
        try {
            store.read(null)
            fail("read")
        } catch (e: LayoutError) {
            assertEquals("the app list didn't load, so there is no default layout yet", e.message)
        }
        assertFalse(store.file.exists())
        assertTrue(store.edit { it }.isFailure)
        assertFalse(store.file.exists())
        val made = store.read { file }
        assertEquals(file, made.layout)
        assertEquals(file, LayoutFile.parse(store.file.readText()))
    }

    @Test
    fun slashesAreWrittenPlain() {
        // Android's org.json escapes every slash; the JVM's doesn't, so the unescape is tested directly.
        assertEquals("\"com.x/com.x.Main\"", LayoutFile.unescapeSlashes("\"com.x\\/com.x.Main\""))
        // An escaped backslash before a slash stays a backslash.
        assertEquals("\"a\\\\/b\"", LayoutFile.unescapeSlashes("\"a\\\\\\/b\""))
        assertEquals("\"a\\\\b\"", LayoutFile.unescapeSlashes("\"a\\\\b\""))
        val f = LayoutFile("a", listOf(Layout("a", listOf(AppRef("com.x", "com.x.Main"), Folder("a\\/b", listOf(AppRef("a.b")))), emptyList())))
        assertTrue(f.toJson().contains("\"com.x/com.x.Main\""))
        assertEquals(f, LayoutFile.parse(f.toJson()))
    }
}
