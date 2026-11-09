package com.spadaval.lor.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SectionEditTest {
    private val base = LayoutFile("a", listOf(Layout.grid("a", emptyList())))
    private val chrome = AppId("com.android.chrome", "com.android.chrome.Main")
    private val chromeRef = AppRef("com.android.chrome")

    private fun refused(edit: () -> LayoutFile): String = try {
        edit()
        fail("not refused")
        ""
    } catch (e: LayoutError) {
        e.message!!
    }

    private fun LayoutFile.names() = drawer.allSections().map { it.name }
    private fun LayoutFile.section(name: String) = drawer.sections.first { it.name == name }
    private fun roundTrips(f: LayoutFile) = assertEquals(f, LayoutFile.parse(f.toJson()))

    @Test
    fun movingAnAppWritesEverySectionInOrderAndPinsOnlyTheTarget() {
        val f = base.moveToSection(chrome, chromeRef, Category.TOOLS, "Work")
        assertEquals("nothing moves", base.names(), f.names())
        assertEquals(Category.entries.size, f.drawer.sections.size)
        assertEquals(listOf(chromeRef), f.section("Work").apps)
        assertTrue(f.section("Work").pinned)
        assertEquals(listOf("Work"), f.drawer.sections.filter { it.pinned }.map { it.name })
        assertEquals("Work", f.drawer.sectionOf(chrome, Category.TOOLS))
        roundTrips(f)
    }

    @Test
    fun aWorkAppMovesToASectionApartFromItsTwin() {
        // lor-72c3: a work app no section names is under the Work profile header, in no section.
        val work = chrome.copy(work = true)
        val workRef = chromeRef.copy(work = true)
        assertEquals(null, base.drawer.sectionOf(work, Category.TOOLS))
        val f = base.moveToSection(work, workRef, Category.TOOLS, "Tools")
        assertEquals(listOf(workRef), f.section("Tools").apps)
        assertTrue(f.drawer.lists(work))
        assertFalse("its twin stays in its category's section", f.drawer.lists(chrome))
        assertEquals("Tools", f.drawer.sectionOf(work, Category.TOOLS))
        assertEquals("it's already in Tools", refused { f.moveToSection(work, workRef, Category.TOOLS, "Tools") })
        val both = f.moveToSection(chrome, chromeRef, Category.TOOLS, "Media")
        assertEquals(listOf(workRef), both.section("Tools").apps)
        assertEquals(listOf(chromeRef), both.section("Media").apps)
        roundTrips(both)
    }

    @Test
    fun movingAgainTakesItOutOfTheFirstAndPinsBoth() {
        val f = base.moveToSection(chrome, chromeRef, Category.TOOLS, "Work").moveToSection(chrome, chromeRef, Category.TOOLS, "Media")
        assertEquals(emptyList<AppRef>(), f.section("Work").apps)
        assertTrue("an emptied section stays, pinned", f.section("Work").pinned)
        assertEquals(listOf(chromeRef), f.section("Media").apps)
        assertEquals("Media", f.drawer.sectionOf(chrome, Category.TOOLS))
        assertEquals("it's already in Media", refused { f.moveToSection(chrome, chromeRef, Category.TOOLS, "media") })
        assertEquals("it's already in Tools", refused { base.moveToSection(chrome, chromeRef, Category.TOOLS, "Tools") })
        assertEquals("there is no section called \"Nope\"", refused { base.moveToSection(chrome, chromeRef, Category.TOOLS, "Nope") })
        roundTrips(f)
    }

    @Test
    fun renames() {
        val f = base.renameSection("Media", "Watch")
        assertEquals("Watch", f.names()[4])
        assertEquals(Category.MEDIA, f.section("Watch").category)
        assertTrue(f.section("Watch").pinned)
        assertEquals("a section is already called \"Work\"", refused { base.renameSection("Media", "work") })
        assertEquals("a section needs a name", refused { base.renameSection("Media", "  ") })
        assertEquals("Changing only the case is a rename", "MEDIA", base.renameSection("Media", "MEDIA").names()[4])
        roundTrips(f)
    }

    @Test
    fun refusesACategoryAnotherSectionTakes() {
        assertEquals("Tools takes Tools already", refused { base.newSection("Daily").setSectionCategory("Daily", Category.TOOLS) })
    }

    @Test
    fun givesACategoryBackAndTakesAFreeOne() {
        // Tools gives up its category; a new Tools section is made for it, and Daily can then take it.
        val freed = base.setSectionCategory("Tools", null)
        assertEquals(null, freed.section("Tools").category)
        assertTrue("Tools (2)" in freed.names())
        // New writes Tools (2) into the file, which then holds the category until it is deleted.
        val withNew = freed.newSection("Daily")
        assertEquals("Tools (2) takes Tools already", refused { withNew.setSectionCategory("Daily", Category.TOOLS) })
        val daily = withNew.deleteSection("Tools (2)").setSectionCategory("Daily", Category.TOOLS)
        // Taking a category from a section that isn't in the file needs it free in the file too.
        assertEquals(Category.TOOLS, freed.setSectionCategory("Tools", Category.TOOLS).section("Tools").category)
        assertEquals(Category.TOOLS, daily.section("Daily").category)
        assertFalse("Tools (2)" in daily.names())
        roundTrips(daily)
    }

    @Test
    fun movesUpAndDown() {
        val f = base.moveSection("Media", -1)
        assertEquals(listOf("Work", "Communication", "Money", "Media", "Travel"), f.names().take(5))
        assertEquals(listOf("Media"), f.drawer.sections.filter { it.pinned }.map { it.name })
        assertEquals("it's already first", refused { base.moveSection("Work", -1) })
        assertEquals("it's already last", refused { base.moveSection("Other", 1) })
        roundTrips(f)
    }

    @Test
    fun hidesAndShows() {
        val f = base.setSectionHidden("Games", true)
        assertTrue(f.section("Games").hidden)
        assertFalse(f.setSectionHidden("Games", false).section("Games").hidden)
        assertEquals("it's already hidden", refused { f.setSectionHidden("Games", true) })
        assertEquals("it's already shown", refused { base.setSectionHidden("Games", false) })
    }

    @Test
    fun deletesOnlyWhatTheFileHolds() {
        val moved = base.moveToSection(chrome, chromeRef, Category.TOOLS, "Work")
        val f = moved.deleteSection("Work")
        assertFalse(f.drawer.sections.any { it.name == "Work" })
        assertTrue("its category gets a section of its own", "Work" in f.names())
        assertEquals("its listed app goes back to its category", "Tools", f.drawer.sectionOf(chrome, Category.TOOLS))
        assertEquals("\"Work\" is made from its category and can't be deleted", refused { base.deleteSection("Work") })
    }

    @Test
    fun newSectionsGoLastAndNeedAFreshName() {
        val f = base.newSection(" Daily ")
        assertEquals("Daily", f.names().last())
        assertTrue(f.section("Daily").pinned)
        assertEquals(null, f.section("Daily").category)
        assertEquals("a section is already called \"Daily\"", refused { f.newSection("daily") })
        assertEquals("a section is already called \"Games\"", refused { base.newSection("games") })
        assertEquals("a section needs a name", refused { base.newSection("") })
        roundTrips(f)
    }

    @Test
    fun anAutomaticNameNeverRepeatsAFileName() {
        // On the phone alone: a section called "Work (2)", then Work gives up its category.
        val f = base.newSection("Work (2)").setSectionCategory("Work", null)
        val names = f.names().map { it.lowercase() }
        assertEquals(names.size, names.toSet().size)
        assertTrue("Work (3)" in f.names())
        roundTrips(f)
        // And every later edit still reads back.
        roundTrips(f.renameSection("Media", "Watch"))
    }

    @Test
    fun movingWithAPackageRefDropsEveryRefOfThePackage() {
        // A section lists an activity an update removed; the package now has one activity.
        val stale = base.copy(drawer = Drawer(sections = listOf(DrawerSection("Daily", apps = listOf(AppRef("com.android.chrome", "com.android.chrome.Old"))))))
        val f = stale.moveToSection(chrome, chromeRef, Category.TOOLS, "Work")
        assertEquals(emptyList<AppRef>(), f.section("Daily").apps)
        assertEquals(listOf(chromeRef), f.section("Work").apps)
        roundTrips(f)
    }

    @Test
    fun editsToAFileSectionWriteOnlyIt() {
        val daily = base.copy(drawer = Drawer(sections = listOf(DrawerSection("Daily"))))
        assertEquals(1, daily.renameSection("Daily", "Every day").drawer.sections.size)
        assertEquals(1, daily.setSectionHidden("Daily", true).drawer.sections.size)
        assertEquals(1, daily.moveToSection(chrome, chromeRef, Category.TOOLS, "Daily").drawer.sections.size)
        assertEquals(1, daily.setSectionCategory("Daily", Category.TOOLS).drawer.sections.size)
    }
}
