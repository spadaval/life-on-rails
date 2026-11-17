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
}
