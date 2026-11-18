package com.spadaval.lor.concepts.panes

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.spadaval.lor.concepts.kit.KitLook
import com.spadaval.lor.lab.ConceptFonts
import com.spadaval.lor.ui.SairaCondensed

/*
 * Panes' tokens: the dashboard prototype's (prototype/dashboard/src/directions/panes/tokens.css) at
 * phone size, so the phone and the dashboard read as one product.
 *
 * Type: one sans, Hanken Grotesk, at five steps with one job each: 12 meta (labels, times, counts),
 * 14 secondary text and controls, 15 body and a row's title, 18 a section head in an open item, a
 * sheet's title and the timer's readout, 28 the open item's title on the inner screen and home's
 * clock. Saira Condensed caps only for pane titles (TODAY, AGENT), as on the dashboard.
 * Colour: panes are layers of the ground's colour at rising lightness. Amber marks the selected
 * row, the open item's edge and the running timer; cyan marks the agent; one red for errors and
 * LoR not answering. Everything else is three warm greys.
 * Depth: the most layered of the four. Ground (the blurred wallpaper under a veil), pane (the list,
 * frosted), raised row (pressed and selected), sheet (the open item, frosted and lifted), and what
 * floats over everything (menus, the toast). A pane covered by another steps back and blurs.
 */
object P {
    // ---- the ground and its layers ----
    val ground = Color(0xFF0A0B10)
    /** Over the blurred wallpaper behind a panel page: most of the ground, so the panes read. */
    val veil = Color(0xE60A0B10)
    val glowBlue = Color(0x8C28365C)
    val glowAmber = Color(0x525C3412)
    val pane = Color.White.copy(alpha = 0.035f)
    val paneRest = Color.White.copy(alpha = 0.018f)
    /** The open item's sheet and pane: frosted, a step above the list. */
    val sheet = Color(0xE01E1F29)
    val float = Color(0xEB22232E)
    val raise = Color.White.copy(alpha = 0.05f)
    val raise2 = Color.White.copy(alpha = 0.085f)
    val well = Color.Black.copy(alpha = 0.22f)
    val line = Color.White.copy(alpha = 0.06f)
    val line2 = Color.White.copy(alpha = 0.11f)
    val line3 = Color.White.copy(alpha = 0.2f)

    // ---- the three greys, warm ----
    val text1 = Color(0xFFECE8E2)
    val text2 = Color(0xFFAAA39A)
    val text3 = Color(0xFF8A8379)

    // ---- jobs ----
    val amber = Color(0xFFF08A2C)
    val amberInk = Color(0xFF1A0D02)
    val amberEdge = amber.copy(alpha = 0.55f)
    val amberSoft = amber.copy(alpha = 0.12f)
    val cyan = Color(0xFF6CC6EC)
    val cyanSoft = cyan.copy(alpha = 0.10f)
    val cyanEdge = cyan.copy(alpha = 0.35f)
    val red = Color(0xFFEF7A5F)
    val redSoft = red.copy(alpha = 0.10f)
    val redEdge = red.copy(alpha = 0.36f)
    val green = Color(0xFF6CC58F)
    val focus = Color(0xFF9CC7FF)

    /** Ink on home over a pale wallpaper. */
    val inkDark = Color(0xFF16171D)
    val inkDark2 = Color(0xFF3C3A36)

    // ---- type ----
    private val center = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)
    private val sans get() = TextStyle(fontFamily = ConceptFonts.hankenGrotesk, lineHeightStyle = center, fontFeatureSettings = "tnum")
    val meta get() = sans.copy(fontSize = 12.sp, lineHeight = 16.sp, color = text2)
    val small get() = sans.copy(fontSize = 14.sp, lineHeight = 20.sp, color = text2)
    val body get() = sans.copy(fontSize = 15.sp, lineHeight = 22.sp, color = text1)
    val rowTitle get() = body.copy(fontWeight = FontWeight.Medium)
    val head get() = sans.copy(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, color = text1)
    val title get() = sans.copy(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold, color = text1, letterSpacing = (-0.01).em)
    /** A pane's title: condensed caps, the one place Saira Condensed is used, as on the dashboard. */
    val paneTitle get() = TextStyle(fontFamily = SairaCondensed, fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.14.em, color = text1, lineHeightStyle = center)
    /** A small label over a group (Needs you, Due): sentence case at the meta size, as the dashboard has it. */
    val group get() = meta.copy(fontWeight = FontWeight.Medium, color = text2)

    // ---- space: medium. 16 pane padding, 12 between groups, rows 56 dp at the thumb ----
    val s1 = 4.dp
    val s2 = 8.dp
    val s3 = 12.dp
    val s4 = 16.dp
    val s5 = 24.dp
    val s6 = 32.dp
    val row = 56.dp
    /** Between panes, and between a pane and the screen's edge. */
    val gap = 8.dp

    // ---- corners: as the dashboard's ----
    val rXs = RoundedCornerShape(6.dp)
    val rSm = RoundedCornerShape(10.dp)
    val rMd = RoundedCornerShape(14.dp)
    val rLg = RoundedCornerShape(18.dp)
    val rXl = RoundedCornerShape(26.dp)
    val rPill = RoundedCornerShape(999.dp)

    val kit get() = KitLook(
        glass = float, glassLine = line2, scrim = Color(0x59000000), text = text1, muted = text2, wash = raise2,
        radius = 26.dp, body = body, label = meta, accent = amber, motionMs = 220,
    )
}
