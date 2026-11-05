package com.spadaval.lor.home

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSearchTest {
    private val apps = listOf("Calendar", "Café Nero", "Camera", "Google Maps", "Lyft", "Prime Video", "Settings", "T-Mobile", "WhatsApp", "YouTube", "YT Music")

    private fun find(q: String) = AppSearch.search(q, apps) { it }

    @Test
    fun emptyQueryIsTheWholeList() {
        assertEquals(apps, find(""))
        assertEquals(apps, find("  "))
    }

    @Test
    fun prefixAndWordStartsMatch() {
        assertEquals(listOf("Calendar", "Café Nero", "Camera"), find("ca"))
        assertEquals(listOf("Google Maps"), find("maps"))
        assertEquals(listOf("Google Maps"), find("google m"))
        assertEquals(listOf("YouTube"), find("tube"))
        assertEquals(listOf("WhatsApp"), find("app"))
        assertEquals(listOf("T-Mobile"), find("mob"))
        assertEquals(listOf("YouTube", "YT Music"), find("y"))
    }

    @Test
    fun caseAndAccentsDoNotMatter() {
        assertEquals(listOf("Café Nero"), find("CAFE"))
        assertEquals(listOf("Café Nero"), find("café n"))
    }

    @Test
    fun aStartBeatsAWordBeatsTheMiddle() {
        val list = listOf("Contacts", "Messages", "My Actions", "Activity Log")
        // "Activity Log" starts with it, "My Actions" has it as a word, "Contacts" only contains it.
        assertEquals(listOf("Activity Log", "My Actions", "Contacts"), AppSearch.search("act", list) { it })
        // Short queries don't match the middle of a word: "out" finds nothing in YouTube.
        assertEquals(emptyList<String>(), find("ou"))
        assertEquals(listOf("YouTube"), find("out"))
    }
}
