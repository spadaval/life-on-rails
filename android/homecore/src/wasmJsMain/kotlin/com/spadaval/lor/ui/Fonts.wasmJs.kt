package com.spadaval.lor.ui

import androidx.compose.ui.text.font.FontFamily

/** The lab's Saira, set by its main from the bundled font files before anything draws. */
object WebFonts {
    var saira: FontFamily? = null
    var condensed: FontFamily? = null
}

// Read before they are set, these throw: a default font would draw the whole lab wrong without a word.
actual val Saira: FontFamily get() = WebFonts.saira ?: error("Saira was read before the lab loaded it")
actual val SairaCondensed: FontFamily get() = WebFonts.condensed ?: error("Saira Condensed was read before the lab loaded it")
