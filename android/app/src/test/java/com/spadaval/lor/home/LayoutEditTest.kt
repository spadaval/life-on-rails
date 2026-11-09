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
          "version": 2,
          "active": "default",
          "layouts": [
            { "name": "default",
              "expires": "2026-11-18T00:00:00Z",
              "start": { "narrow": [1, 0] },
              "panels": [
                { "at": [0, 0], "type": "t3" },
                { "at": [1, 0], "type": "stack", "blocks": [
                  { "type": "clock" },
                  { "type": "apps", "items": ["com.whatsapp",
                    { "folder": "Dev", "apps": ["com.github.android", "org.example/.Term"], "reason": "Together." },
                    { "folder": "Solo", "apps": ["com.github.android"] }] }] },
                { "at": [1, 1], "type": "drawer" }] },
            { "name": "trip",
              "start": { "narrow": [1, 0] },
              "panels": [
                { "at": [1, 0], "type": "stack", "blocks": [
                  { "type": "clock" },
                  { "type": "apps", "items": ["com.whatsapp", { "folder": "Dev", "apps": ["com.whatsapp", "a.b"] }] }] }] }
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
        assertEquals(Places(home = true, folder = null), file.placesOf(wa))
        assertEquals(Places(home = false, folder = "Dev"), file.placesOf(gh))
        assertEquals(Places(home = false, folder = null), file.placesOf(dialer))
        assertEquals(Places(home = false, folder = null), file.placesOf(AppId("x.y", "x.y.Z")))
        // A package/activity ref names only that activity.
        assertEquals(Places(home = false, folder = null), file.placesOf(AppId("org.example", "org.example.Other")))
    }

    @Test
    fun addsToHome() {
        val chrome = AppId("com.android.chrome", "com.google.android.apps.chrome.Main")
        val home = file.addToHome(chrome, AppRef("com.android.chrome"))
        assertEquals(AppRef("com.android.chrome"), home.current.home.last())
        assertEquals("other layouts stay", file.layouts[1], home.layouts[1])
        roundTrips(home)
    }

    @Test
    fun refusesDuplicatesAndLimits() {
        assertEquals("it's already on home", refused { file.addToHome(wa, AppRef("com.whatsapp")) })
        var full = file
        repeat(LayoutFile.MAX_HOME - file.current.home.size) { full = full.addToHome(AppId("a.a$it", "a.a$it.M"), AppRef("a.a$it")) }
        assertEquals("a full home page sends it to a new page (4.1)", listOf(AppRef("z.z")), full.addToHome(AppId("z.z", "z.z.M"), AppRef("z.z")).current.items(page(1)))
        assertEquals("it isn't on home", refused { file.removeFromHome(gh) })
        assertEquals("it isn't in a folder", refused { file.removeFromFolder(wa) })
    }

    @Test
    fun removesFromHome() {
        val home = file.removeFromHome(wa)
        assertFalse(home.placesOf(wa).home)
        assertEquals(2, home.current.home.size)
        assertEquals("the trip layout keeps its WhatsApp", file.layouts[1], home.layouts[1])
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
        assertEquals("in every layout, from home and folders", listOf(Folder("Dev", listOf(AppRef("a.b")), pinned = true)), trip.home)
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
        val text = v2Empty(""""hidden": ["com.x", "com.y/.Main"],""")
        val f = LayoutFile.parse(text)
        assertEquals(listOf(AppRef("com.x"), AppRef("com.y", "com.y.Main")), f.hidden)
        roundTrips(f)
        assertFalse("an empty list is left out", LayoutFile("a", listOf(Layout.grid("a", emptyList()))).toJson().contains("hidden"))
        fun error(hidden: String) = try {
            LayoutFile.parse(v2Empty(""""hidden": $hidden,"""))
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
        val broken = store.edit { it.copy(layouts = it.layouts.map { l -> l.withItems(HOME_APPS, l.home + Folder("Empty", emptyList())) }) }
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
        store.file.writeText(file.removeFromHome(wa).toJson())
        val ok = store.edit { it.addToHome(wa, AppRef("com.whatsapp")) }.getOrThrow()
        assertEquals(AppRef("com.whatsapp"), ok.layout.current.home.last())
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
        val f = LayoutFile("a", listOf(Layout.grid("a", listOf(AppRef("com.x", "com.x.Main"), Folder("a\\/b", listOf(AppRef("a.b")))))))
        assertTrue(f.toJson().contains("\"com.x/com.x.Main\""))
        // The JVM's org.json escapes "</" as "<\/", so this fails if toJson stops unescaping.
        assertTrue(f.copy(layouts = listOf(Layout.grid("a", listOf(Folder("a</b", listOf(AppRef("a.b"))))))).toJson().contains("\"a</b\""))
        assertEquals(f, LayoutFile.parse(f.toJson()))
    }
}
