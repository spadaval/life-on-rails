package com.spadaval.lor.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

/** The drawer tabs' hand edits (lor-iyc3): Move to tab… and Home settings' Drawer tabs. */
class TabEditTest {
    private val base = LayoutFile("a", listOf(Layout.grid("a", emptyList())))
        .newTab("Work", TabIcon.BRIEF).newTab("Watch", TabIcon.PLAY)
    private val chrome = AppId("com.android.chrome", "com.android.chrome.Main")
    private val chromeRef = AppRef("com.android.chrome")

    private fun refused(edit: () -> LayoutFile): String = try {
        edit()
        fail("not refused")
        ""
    } catch (e: LayoutError) {
        e.message!!
    }

    private fun LayoutFile.names() = drawer.tabs.map { it.name }
    private fun LayoutFile.tab(name: String) = drawer.tabs.first { it.name == name }
    private fun roundTrips(f: LayoutFile) = assertEquals(f, LayoutFile.parse(f.toJson()))

    @Test
    fun aNewTabGoesBeforeAllAndTheDrawerTakesEight() {
        assertEquals(listOf("Quick dial", "Work", "Watch", "All"), base.names())
        assertEquals(DrawerTab("Work", "brief"), base.tab("Work"))
        assertEquals("star", base.newTab("Fun").tab("Fun").icon)
        assertEquals("a tab is already called \"Work\"", refused { base.newTab(" work ") })
        assertEquals("a tab needs a name", refused { base.newTab("  ") })
        val full = (1..4).fold(base) { f, i -> f.newTab("T$i") }
        assertEquals(8, full.drawer.tabs.size)
        assertEquals("the drawer has 8 tabs, the most it takes", refused { full.newTab("T5") })
        roundTrips(full)
    }

    @Test
    fun movingAnAppWritesItIntoTheTabOutOfAnyOther() {
        val f = base.moveToTab(chrome, chromeRef, "Work")
        assertEquals(listOf(chromeRef), f.tab("Work").apps)
        assertEquals("Work", f.drawer.tabOf(chrome)?.name)
        val again = f.moveToTab(chrome, chromeRef, "watch")
        assertEquals(emptyList<AppRef>(), again.tab("Work").apps)
        assertEquals(listOf(chromeRef), again.tab("Watch").apps)
        assertEquals("it's already in Watch", refused { again.moveToTab(chrome, chromeRef, "Watch") })
        assertEquals("there is no tab called \"Nope\"", refused { base.moveToTab(chrome, chromeRef, "Nope") })
        assertEquals("All isn't a list of apps", refused { base.moveToTab(chrome, chromeRef, "All") })
        assertEquals("Quick dial isn't a list of apps", refused { base.moveToTab(chrome, chromeRef, "Quick dial") })
        roundTrips(again)
    }

    @Test
    fun aWorkAppMovesApartFromItsTwin() {
        val work = chrome.copy(work = true)
        val workRef = chromeRef.copy(work = true)
        val f = base.moveToTab(work, workRef, "Work").moveToTab(chrome, chromeRef, "Watch")
        assertEquals(listOf(workRef), f.tab("Work").apps)
        assertEquals(listOf(chromeRef), f.tab("Watch").apps)
        roundTrips(f)
    }

    @Test
    fun renamesAndIcons() {
        val f = base.renameTab("Watch", "Media").renameTab("All", "Everything").setTabIcon("Quick dial", TabIcon.STAR)
        assertEquals(listOf("Quick dial", "Work", "Media", "Everything"), f.names())
        assertEquals("star", f.tab("Quick dial").icon)
        assertEquals(TabKind.ALL, f.tab("Everything").kind)
        assertEquals("a tab is already called \"Work\"", refused { base.renameTab("Watch", "work") })
        assertEquals("Changing only the case is a rename", "WATCH", base.renameTab("Watch", "WATCH").names()[2])
        assertEquals("there is no tab called \"Nope\"", refused { base.setTabIcon("Nope", TabIcon.BOLT) })
        roundTrips(f)
    }

    @Test
    fun listTabsMoveOnlyBetweenQuickDialAndAll() {
        val f = base.moveTab("Watch", -1)
        assertEquals(listOf("Quick dial", "Watch", "Work", "All"), f.names())
        assertEquals(base.names(), f.moveTab("Watch", 1).names())
        assertEquals("it's already first of the lists", refused { base.moveTab("Work", -1) })
        assertEquals("it's already last of the lists", refused { base.moveTab("Watch", 1) })
        assertEquals("All isn't a list of apps", refused { base.moveTab("All", -1) })
        roundTrips(f)
    }

    @Test
    fun deletingAListTabLeavesItsAppsInNoTab() {
        val f = base.moveToTab(chrome, chromeRef, "Work").deleteTab("work")
        assertEquals(listOf("Quick dial", "Watch", "All"), f.names())
        assertNull(f.drawer.tabOf(chrome))
        assertEquals("Quick dial isn't a list of apps", refused { base.deleteTab("Quick dial") })
        // Back to quick dial and All, the file writes no tabs.
        assertEquals(Drawer(), f.deleteTab("Watch").drawer)
        roundTrips(f)
    }
}
