package com.spadaval.lor.concepts.context

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.foundation.*
import androidx.compose.ui.draw.*
import com.spadaval.lor.concepts.kit.ConceptStore
import com.spadaval.lor.concepts.kit.Glyph
import com.spadaval.lor.concepts.kit.Icon
import com.spadaval.lor.concepts.kit.MenuItem
import com.spadaval.lor.lab.labAnchor
import com.spadaval.lor.lab.Load
import com.spadaval.lor.lab.T3Status
import com.spadaval.lor.lab.Wallpaper
import com.spadaval.lor.lab.LocalWallpaperKind
import com.spadaval.lor.ui.LocalClock
import com.spadaval.lor.agent.T3Card
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.BasicText
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.spadaval.lor.concepts.kit.KitLook
import com.spadaval.lor.ui.Saira
import com.spadaval.lor.ui.SairaCondensed

/*
 * Context's tokens: the dashboard prototype's (prototype/dashboard/src/directions/context/tokens.css)
 * at phone size, so the phone and the dashboard read as one product.
 *
 * Type: one legible sans, Saira, at five steps with one job each: 13 meta (labels, region titles,
 * times, chips), 15 body (rows, buttons), 17 the next task and a card's title, 20 a sheet's title and
 * the next event, 30 the context's title and the running clock, which lead. Saira Condensed only for
 * the timer's numerals.
 * Colour: amber for the current context's timer and selection; cyan for the next fixed event and the
 * agent's work; red for late and for errors; green for done. Each region of the context sits on its
 * own tone of the ground. Everything else is four cool greys.
 * Depth: three levels. Flat on the ground (what waits elsewhere, the next context); the current
 * context raised as one large surface; floating with blur (sheets, menus, the notice, home's card).
 */
object C {
    // ---- the ground and its tones ----
    val ground = Color(0xFF0B0E14)
    val ground2 = Color(0xFF0F131B)
    /** Over the blurred wallpaper behind the Now page: most of the ground, so the surface reads. */
    val veil = Color(0xEB0B0E14)
    /** The current context: one raised surface. */
    val surface = Color(0xFF151A24)
    val surfaceEdge = Color.White.copy(alpha = 0.07f)
    /** A region inside the surface. */
    val region = Color.White.copy(alpha = 0.028f)
    /** A hovered row, a control's face. */
    val region2 = Color.White.copy(alpha = 0.05f)
    /** Pressed, a chip that is on. */
    val region3 = Color.White.copy(alpha = 0.085f)
    val float = Color(0xD1171C27)
    val floatHi = Color(0xF5151A24)
    val well = Color.Black.copy(alpha = 0.25f)
    val line = Color.White.copy(alpha = 0.06f)
    val line2 = Color.White.copy(alpha = 0.11f)
    val line3 = Color.White.copy(alpha = 0.2f)

    // ---- greys ----
    val text1 = Color(0xFFEDF1F7)
    val text2 = Color(0xFFAAB4C3)
    val text3 = Color(0xFF7C8697)
    val text4 = Color(0xFF5B6474)

    // ---- jobs ----
    val amber = Color(0xFFF2A446)
    val amberHi = Color(0xFFFFBD6B)
    val amberInk = Color(0xFF211203)
    val amberSoft = amber.copy(alpha = 0.14f)
    val amberFaint = amber.copy(alpha = 0.07f)
    val amberEdge = amber.copy(alpha = 0.5f)
    val cyan = Color(0xFF6FC8E8)
    val cyanInk = Color(0xFF04161D)
    val cyanSoft = cyan.copy(alpha = 0.11f)
    val cyanFaint = cyan.copy(alpha = 0.05f)
    val cyanEdge = cyan.copy(alpha = 0.4f)
    val red = Color(0xFFF07A6C)
    val redSoft = red.copy(alpha = 0.11f)
    val redEdge = red.copy(alpha = 0.38f)
    val green = Color(0xFF7FD1A3)
    val greenSoft = green.copy(alpha = 0.12f)
    val focus = amber.copy(alpha = 0.8f)

    // Names the copied pieces use, mapped onto these tones.
    val raise = region2
    val raise2 = region3
    val sheet = Color(0xF2171C27)
    val redSoftEdge = redEdge
    val cyanLine = cyanEdge

    /** Ink on home over a pale wallpaper. */
    val inkDark = Color(0xFF12151C)
    val inkDark2 = Color(0xFF3A4150)

    // ---- type ----
    private val center = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)
    private val sans get() = TextStyle(fontFamily = Saira, lineHeightStyle = center, fontFeatureSettings = "tnum")
    val meta get() = sans.copy(fontSize = 13.sp, lineHeight = 18.sp, color = text2)
    val small get() = meta
    val body get() = sans.copy(fontSize = 15.sp, lineHeight = 22.sp, color = text1)
    val rowTitle get() = body.copy(fontWeight = FontWeight.Medium)
    val title get() = sans.copy(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium, color = text1)
    val head get() = sans.copy(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, color = text1)
    val ctx get() = sans.copy(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold, color = text1, letterSpacing = (-0.01).em)
    /** The timer's numerals: condensed, tabular. */
    val num get() = TextStyle(fontFamily = SairaCondensed, fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold, color = amber, lineHeightStyle = center, fontFeatureSettings = "tnum")
    /** A region's title: small caps-tracked label, as on the dashboard. */
    val label get() = meta.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.08.em, color = text3)
    val group get() = label
    val paneTitle get() = label

    // ---- space: medium-high density inside the surface, 26 between its regions ----
    val s1 = 4.dp
    val s2 = 8.dp
    val s3 = 12.dp
    val s4 = 16.dp
    val s5 = 20.dp
    val regionGap = 26.dp
    val s6 = 32.dp
    val row = 52.dp
    val gap = 8.dp

    // ---- corners: rounder the larger and the nearer it is ----
    val rXs = RoundedCornerShape(6.dp)
    val rChip = RoundedCornerShape(8.dp)
    val rSm = RoundedCornerShape(10.dp)
    val rMd = RoundedCornerShape(12.dp)
    val rLg = RoundedCornerShape(16.dp)
    val rRegion = RoundedCornerShape(16.dp)
    val rFloat = RoundedCornerShape(20.dp)
    val rSurface = RoundedCornerShape(24.dp)
    val rPill = RoundedCornerShape(999.dp)

    val kit get() = KitLook(
        glass = Color(0xEB171C27), glassLine = line2, scrim = Color(0x8C05070B), text = text1, muted = text2, wash = region3,
        radius = 24.dp, body = body, label = meta, accent = amber, motionMs = 220,
    )
}
