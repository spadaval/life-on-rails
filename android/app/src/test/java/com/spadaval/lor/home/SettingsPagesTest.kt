package com.spadaval.lor.home

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsPagesTest {
    private val pages = SettingsPage.ALL.map { SettingsPage.found(it, it.title) }

    private fun first(q: String) = Search.run(q, pages).go?.value

    @Test
    fun titlesAreUnique() {
        assertEquals(SettingsPage.ALL.size, SettingsPage.ALL.map { it.title.lowercase() }.toSet().size)
    }

    @Test
    fun aliasesAndLettersFindTheirPage() {
        assertEquals("Wi-Fi", first("wifi"))
        assertEquals("Wi-Fi", first("wfi"))
        assertEquals("Bluetooth", first("bt"))
        assertEquals("Bluetooth", first("bluetooth"))
        assertEquals("Display", first("dark theme"))
        assertEquals("Display", first("dark"))
        assertEquals("Home settings", first("home settings"))
        assertEquals("Do Not Disturb", first("dnd"))
    }
}
