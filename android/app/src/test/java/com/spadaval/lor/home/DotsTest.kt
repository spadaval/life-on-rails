package com.spadaval.lor.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Which notifications make a dot (launcher polish 6.6), Launcher3's rule. */
class DotsTest {
    private val plain = Note("k", "com.a", "UserHandle{0}", groupSummary = false, ongoing = false, defaultChannel = false, canShowBadge = true, hasText = true)

    @Test
    fun launcher3sRule() {
        assertTrue(counts(plain))
        assertFalse(counts(plain.copy(groupSummary = true)))
        assertFalse(counts(plain.copy(canShowBadge = false)))
        assertFalse(counts(plain.copy(hasText = false)))
        assertFalse(counts(plain.copy(ongoing = true, defaultChannel = true)))
        // Ongoing on an app's own channel (a music player) still counts.
        assertTrue(counts(plain.copy(ongoing = true)))
    }

    @Test
    fun keyedByPackageAndUser() {
        val work = plain.copy(key = "w", user = "UserHandle{10}")
        val summary = plain.copy(key = "s", packageName = "com.b", groupSummary = true)
        assertEquals(setOf("com.a#UserHandle{0}", "com.a#UserHandle{10}"), dotKeys(listOf(plain, work, summary)))
    }
}
