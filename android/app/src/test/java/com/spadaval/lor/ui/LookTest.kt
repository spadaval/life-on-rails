package com.spadaval.lor.ui

import com.spadaval.lor.ui.Palette.contrast
import org.junit.Assert.assertTrue
import org.junit.Test

/** DESIGN.md's text colours read on every surface they are drawn on (launcher polish 6.1). */
class LookTest {
    private fun reads(text: Long, on: Long, name: String) {
        val c = contrast(text, on)
        assertTrue("$name: %.2f:1 is under 4.5:1".format(c), c >= 4.5)
    }

    @Test
    fun everyTextColourReadsOnEverySurface() {
        val texts = mapOf("Starlight" to Palette.TXT, "Dust" to Palette.MUTE, "Far Dust" to Palette.DIM)
        val surfaces = mapOf("Deep Space" to Palette.BG, "Well" to Palette.WELL, "Panel Face" to Palette.PANEL, "Raised" to Palette.RAISED)
        for ((t, tc) in texts) for ((s, sc) in surfaces) reads(tc, sc, "$t on $s")
        reads(Palette.ON_OR, Palette.OR, "text on amber")
        reads(Palette.BAD, Palette.PANEL, "Coral on Panel Face")
        reads(Palette.OR, Palette.RAISED, "amber on Raised (a primary container)")
        reads(Palette.CY, Palette.PANEL, "cyan on Panel Face")
        reads(Palette.TXT, Palette.over(Palette.OR, 0.08, Palette.PANEL), "Starlight on the selection (a primary container)")
    }

    @Test
    fun eachWallpaperInkReadsOnItsWorstWallpaper() {
        // Dark ink, text and asides alike, on the darkest wallpaper Android gives the dark-text hint.
        reads(Palette.BG, 0xFF858585, "dark ink on #858585")
        // Light ink, without its shadow, on a near-black wallpaper.
        reads(Palette.TXT, 0xFF101010, "light ink on #101010")
        reads(Palette.MUTE, 0xFF101010, "light ink's aside on #101010")
    }

    @Test
    fun theAgentPagesFaintestTextReadsOnItsVeilOverWhite() {
        val veil = Palette.over(Palette.BG, Palette.VEIL_ALPHA, 0xFFFFFFFF)
        reads(Palette.DIM, veil, "Far Dust on the veil over white")
        // The old 85% veil didn't.
        assertTrue(contrast(Palette.DIM, Palette.over(Palette.BG, 0xD9 / 255.0, 0xFFFFFFFF)) < 4.5)
    }

    @Test
    fun theLaunchersOldDimWouldFail() {
        // 3.6's critique: the launcher's Dim (0xFF616871) read at 2.3 to 3.5:1. Far Dust replaces it.
        assertTrue(contrast(0xFF616871, Palette.PANEL) < 4.5)
    }

    @Test
    fun contrastIsWcags() {
        assertTrue(contrast(0xFF000000, 0xFFFFFFFF) in 20.99..21.01)
        assertTrue(contrast(0xFF777777, 0xFFFFFFFF) in 4.47..4.49)
        assertTrue(Palette.over(0xFF000000, 0.5, 0xFFFFFFFF) == 0xFF808080)
    }
}
