package com.spadaval.lor.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** `look` in layout.json (launcher polish 6.4). */
class HomeLookTest {
    private fun file(look: String?) = v2Empty(look?.let { "\"look\": $it," } ?: "")

    private fun refused(look: String, message: String) {
        try {
            LayoutFile.parse(file(look))
            fail("parsed $look")
        } catch (e: LayoutError) {
            assertEquals(message, e.message)
        }
    }

    @Test
    fun aFileWithoutLookHasTheDefaultsAndWritesNone() {
        val f = LayoutFile.parse(file(null))
        assertEquals(HomeLook(), f.look)
        assertFalse(f.toJson().contains("look"))
    }

    @Test
    fun everyFieldRoundTripsAndNestedFieldsAreOptional() {
        val f = LayoutFile.parse(file("""{"grid": {"narrow": {"home": 5}, "wide": {"drawer": 8}}, "labels": false, "labelSize": 13, "iconSize": 48}"""))
        assertEquals(HomeLook.Grid(HomeLook.Columns(5, 4), HomeLook.Columns(5, 8)), f.look.grid)
        assertFalse(f.look.labels)
        assertEquals(13, f.look.labelSize)
        assertEquals(48, f.look.iconSize)
        assertEquals(f, LayoutFile.parse(f.toJson()))
    }

    @Test
    fun defaultsWrittenByHandAreDroppedOnTheNextWrite() {
        val f = LayoutFile.parse(file("""{"grid": {"narrow": {"home": 4}}, "labels": true, "iconSize": 52}"""))
        assertEquals(HomeLook(), f.look)
        assertFalse(f.toJson().contains("look"))
    }

    @Test
    fun refusalsNameTheirPlace() {
        refused("""{"grid": {"narrow": {"home": 9}}}""", "look.grid.narrow.home: 9 is not between 4 and 6")
        refused("""{"grid": {"wide": {"drawer": 11}}}""", "look.grid.wide.drawer: 11 is not between 4 and 10")
        refused("""{"labelSize": 11}""", "look.labelSize: 11 is not between 12 and 14")
        refused("""{"iconSize": 52.5}""", "look.iconSize: must be a whole number")
        refused("""{"labels": "no"}""", "look.labels: must be true or false")
        refused("""{"colour": 1}""", "look: unknown field \"colour\" (allowed: grid, labels, labelSize, iconSize, iconPack, themedIcons, iconShape, header)")
        refused("""{"grid": {"tall": {}}}""", "look.grid: unknown field \"tall\" (allowed: narrow, wide)")
        refused("""[]""", "look: must be an object")
    }

    @Test
    fun columnsBreakAt600dp() {
        val look = HomeLook()
        assertEquals(HomeLook.NARROW, look.columnsFor(599f))
        assertEquals(HomeLook.WIDE, look.columnsFor(600f))
        assertTrue(HomeLook.LABEL_SIZES.first >= 12)
    }
}
