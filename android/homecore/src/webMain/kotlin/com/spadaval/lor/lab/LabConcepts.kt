package com.spadaval.lor.lab

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontFamily
import com.spadaval.lor.concepts.Concepts

/*
 * Where the lab page (Main.kt) meets the design concepts: the registry's list, and the wallpaper a
 * concept blurs behind what floats. The concepts' kit and directions read these; Main.kt provides them.
 */

/** The concepts' versions, for the registry. */
fun labConcepts(): List<Version> = Concepts.all

/** The lab's wallpaper behind the phone, which a concept blurs behind what floats. */
val LocalWallpaper = staticCompositionLocalOf<ImageBitmap?> { null }

/** Which wallpaper it is: a concept picks home's ink from it (dark words over the pale one). */
val LocalWallpaperKind = staticCompositionLocalOf { Wallpaper.Dark }

/**
 * Faces a concept uses beyond the app's Saira, loaded by the lab page from `fonts/` before anything
 * draws: Hanken Grotesk, the Panes direction's sans on the dashboard prototype (OFL, beside it in
 * `fonts/`). Read before it is set, it throws, as Saira does: a default face would draw a concept
 * wrong without a word.
 */
object ConceptFonts {
    var hanken: FontFamily? = null
    val hankenGrotesk: FontFamily get() = hanken ?: error("Hanken Grotesk was read before the lab loaded it")
}
