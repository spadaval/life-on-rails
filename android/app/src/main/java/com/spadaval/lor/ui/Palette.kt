package com.spadaval.lor.ui

import kotlin.math.pow

/**
 * DESIGN.md's colours as plain ARGB values, and the arithmetic to check them, with nothing of
 * Compose or Android in it so the JVM tests reach it. [Look] turns them into Compose colours.
 */
object Palette {
    const val BG = 0xFF0C0D12          // Deep Space
    const val WELL = 0xFF0F1016        // Instrument Well
    const val PANEL = 0xFF111218       // Panel Face
    const val RAISED = 0xFF181920      // Raised Plate
    const val LINE = 0xFF1E2029        // Engraved Line
    const val EDGE = 0xFF3A3634        // Bulkhead Edge
    const val FRAME = 0xFF4A2E14       // Frame Bronze
    const val TXT = 0xFFECE6DC         // Starlight
    const val MUTE = 0xFFB1A596        // Dust
    const val DIM = 0xFF928777         // Far Dust
    const val OR = 0xFFF08A2C          // Drive Amber
    const val OR_DEEP = 0xFF9A5C22     // Banked Amber
    const val CY = 0xFF6CC6EC          // Nav-Beacon Cyan
    const val BAD = 0xFFE0735A         // Hull-Breach Coral
    const val ON_OR = 0xFF1A0D02       // text on a solid amber fill

    /** The agent page's veil: Deep Space at 92% (launcher polish 6.3). */
    const val VEIL_ALPHA = 0xEB / 255.0

    /** WCAG relative luminance of an opaque colour. */
    fun luminance(argb: Long): Double {
        fun channel(shift: Int): Double {
            val c = ((argb shr shift) and 0xFF) / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    /** WCAG contrast ratio between two opaque colours, from 1 to 21. */
    fun contrast(a: Long, b: Long): Double {
        val (hi, lo) = luminance(a).let { la -> luminance(b).let { lb -> maxOf(la, lb) to minOf(la, lb) } }
        return (hi + 0.05) / (lo + 0.05)
    }

    /** [top] at [alpha] composited over the opaque [bottom], as the screen draws it (in sRGB). */
    fun over(top: Long, alpha: Double, bottom: Long): Long {
        fun mix(shift: Int): Long {
            val t = (top shr shift) and 0xFF
            val b = (bottom shr shift) and 0xFF
            return Math.round(t * alpha + b * (1 - alpha)) and 0xFF
        }
        return (0xFFL shl 24) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
    }
}
