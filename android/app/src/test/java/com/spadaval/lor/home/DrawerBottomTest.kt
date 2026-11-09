package com.spadaval.lor.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The drawer with its controls at the bottom (lor-sqfe): results laid out bottom up, and which swipes the list takes. */
class DrawerBottomTest {
    private fun found(kind: Kind, name: String) = Found<Any>(kind, "$kind:$name", emptyList(), name)

    @Test
    fun resultsRunUpwardWithEachHeaderAboveItsBlock() {
        val results = Results<Any>(
            "s",
            null,
            listOf(
                Block(Kind.SETTINGS, listOf(found(Kind.SETTINGS, "Sound"), found(Kind.SETTINGS, "Storage")), 0),
                Block(Kind.CONTACTS, listOf(found(Kind.CONTACTS, "Sam")), 0),
            ),
        )
        val built = searchEntries(results, goKey = "SETTINGS:Sound")
        // Index 0 is drawn nearest the field: the best result first, then its block, then the block's
        // header, which the reversed grid draws above them.
        assertEquals(
            listOf("q:SETTINGS:Sound", "q:SETTINGS:Storage", "qh:SETTINGS", "q:CONTACTS:Sam", "qh:CONTACTS"),
            built.entries.map { it.key },
        )
        assertTrue((built.entries[0] as Entry.Row).go)
        assertFalse((built.entries[1] as Entry.Row).go)
    }

    @Test
    fun oneBlockHasNoHeader() {
        val results = Results<Any>("s", null, listOf(Block(Kind.SETTINGS, listOf(found(Kind.SETTINGS, "Sound")), 0)))
        assertEquals(listOf("q:SETTINGS:Sound"), searchEntries(results, null).entries.map { it.key })
    }

    @Test
    fun aListTakesTheSwipeItCanScroll() {
        // A finger going up moves the grid down; a list in its usual order scrolls on toward its end.
        assertTrue(listTakes(Way.DOWN, forward = true, backward = false, reversed = false))
        assertFalse(listTakes(Way.UP, forward = true, backward = false, reversed = false))
        assertTrue(listTakes(Way.UP, forward = false, backward = true, reversed = false))
        assertFalse(listTakes(Way.DOWN, forward = false, backward = true, reversed = false))
    }

    @Test
    fun aReversedListTakesTheOtherWay() {
        // Results at their best, by the field: a finger going down scrolls up through them (forward);
        // a finger going up has nothing below to show, so it moves the grid.
        assertTrue(listTakes(Way.UP, forward = true, backward = false, reversed = true))
        assertFalse(listTakes(Way.DOWN, forward = true, backward = false, reversed = true))
        // Scrolled up into them, a finger going up comes back down toward the best.
        assertTrue(listTakes(Way.DOWN, forward = true, backward = true, reversed = true))
        // At their top, a finger going down moves the grid.
        assertFalse(listTakes(Way.UP, forward = false, backward = true, reversed = true))
    }
}
