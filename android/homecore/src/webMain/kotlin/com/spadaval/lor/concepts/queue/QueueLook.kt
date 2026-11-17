package com.spadaval.lor.concepts.queue

import androidx.compose.foundation.layout.PaddingValues
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
 * Queue's tokens, the dashboard prototype's (prototype/dashboard/src/directions/queue/tokens.css)
 * at phone size, so the two read as one product.
 *
 * Type: one workhorse sans (Saira) at five steps, each with one job: 13 labels and meta, 15 body
 * and controls, 17 row titles, 22 page titles and the lead card's title, and the readout. Saira
 * Condensed only for the timer's readout, home's clock and the reticle's label.
 * Colour: amber only for the lead card's edge, its primary action and the running timer; cyan for
 * the agent's rows; red for what failed; three greys for everything else.
 * Depth: rows flat on the page, the lead card raised with a two-part shadow, sheets, menus and the
 * drawer floating as glass over the blurred wallpaper.
 */
object Q {
    // ---- colour ----
    val ground = Color(0xFF0D0E12)
    val surface1 = Color(0xFF15161B)
    val surface2 = Color(0xFF1B1C23)
    val wash = Color.White.copy(alpha = 0.05f)
    val wash2 = Color.White.copy(alpha = 0.09f)
    val line = Color.White.copy(alpha = 0.07f)
    val line2 = Color.White.copy(alpha = 0.12f)
    /** Floating surfaces: translucent, over the blurred wallpaper. */
    val float = Color(0xD91E1F27)
    /** The queue page's veil over the blurred wallpaper. */
    val veil = Color(0xD00D0E12)

    val text1 = Color(0xFFECEEF2)
    val text2 = Color(0xFFA5A9B3)
    val text3 = Color(0xFF70747E)

    val amber = Color(0xFFF59E42)
    val onAmber = Color(0xFF1C1206)
    val amberEdge = amber.copy(alpha = 0.5f)
    val amberGlow = amber.copy(alpha = 0.10f)
    val cyan = Color(0xFF6CC6EC)
    val cyanSoft = cyan.copy(alpha = 0.10f)
    val red = Color(0xFFEF7A5F)
    val redSoft = red.copy(alpha = 0.10f)
    val redEdge = red.copy(alpha = 0.32f)

    /** Ink on home over a pale wallpaper. */
    val inkDark = Color(0xFF15161B)
    val inkDark2 = Color(0xFF3A3D46)

    // ---- type ----
    private val base = TextStyle(fontFamily = Saira, lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None))
    val label = base.copy(fontSize = 13.sp, lineHeight = 18.sp, color = text2)
    val body = base.copy(fontSize = 15.sp, lineHeight = 21.sp, color = text1)
    val row = base.copy(fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.Medium, color = text1)
    val title = base.copy(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium, color = text1)
    /** Small caps for kickers and section labels. */
    val caps = base.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.08.em, color = text3)
    val readout = TextStyle(fontFamily = SairaCondensed, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 26.sp, color = amber)
    val clock = TextStyle(fontFamily = SairaCondensed, fontWeight = FontWeight.SemiBold, fontSize = 64.sp, lineHeight = 64.sp, letterSpacing = (-0.01).em)

    // ---- space: generous, the lowest density of the four ----
    val s1 = 4.dp
    val s2 = 8.dp
    val s3 = 12.dp
    val s4 = 16.dp
    val s5 = 24.dp
    val s6 = 32.dp
    val rowMin = 56.dp
    val gutter = 16.dp
    val cardPad = PaddingValues(horizontal = 18.dp, vertical = 16.dp)

    // ---- corners ----
    val rSm = RoundedCornerShape(8.dp)
    val rMd = RoundedCornerShape(12.dp)
    val rLg = RoundedCornerShape(18.dp)
    val rPill = RoundedCornerShape(999.dp)

    val kit = KitLook(
        glass = float, glassLine = line2, scrim = Color(0x66000000), text = text1, muted = text2, wash = wash2,
        radius = 22.dp, body = body, label = label, accent = amber, motionMs = 220,
    )
}
