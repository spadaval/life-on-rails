package com.spadaval.lor.home

import kotlin.test.Test
import kotlin.test.assertEquals

/** WallpaperTest's check of Android's constant, which only the Android target can read. */
class WallpaperHintTest {
    @Test
    fun theHintIsAndroids() {
        assertEquals(android.app.WallpaperColors.HINT_SUPPORTS_DARK_TEXT, HINT_DARK_TEXT)
    }
}
