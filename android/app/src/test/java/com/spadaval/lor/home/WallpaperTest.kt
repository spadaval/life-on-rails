package com.spadaval.lor.home

import org.junit.Assert.assertEquals
import org.junit.Test

/** Which ink home's text takes over the wallpaper (launcher polish 6.3). */
class WallpaperTest {
    private val darkTheme = 2 // WallpaperColors.HINT_SUPPORTS_DARK_THEME

    @Test
    fun darkInkOnlyWhenTheWallpaperSupportsDarkText() {
        assertEquals(Ink.DARK, inkFor(HINT_DARK_TEXT))
        assertEquals(Ink.DARK, inkFor(HINT_DARK_TEXT or darkTheme))
        assertEquals(Ink.LIGHT, inkFor(0))
        assertEquals(Ink.LIGHT, inkFor(darkTheme))
        // No colours at all (getWallpaperColors returned null) is no hints.
        assertEquals(Ink.LIGHT, inkFor(null))
    }

    @Test
    fun theHintIsAndroids() {
        assertEquals(android.app.WallpaperColors.HINT_SUPPORTS_DARK_TEXT, HINT_DARK_TEXT)
    }
}
