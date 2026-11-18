package com.spadaval.lor.concepts.timeline

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
 * Timeline's tokens: the dashboard prototype's (prototype/dashboard/src/directions/timeline/
 * tokens.css) at phone size, so the phone and the dashboard read as one product.
 *
 * Type: Saira Condensed for times and readouts, with tabular figures; Saira for everything else,
 * at five sizes with one job each: 12 meta (labels, times on the route, counts), 14 body (rows,
 * notes, buttons), 16 a row's or block's title, 20 page and sheet titles, 48 the one number that
 * leads (the running clock, home's time).
 * Colour, the most of the four directions, all of it doing a job: amber is time tracked, what is
 * live and the main action; cyan is what is planned (calendar events, a task planned into a gap);
 * a dim neutral is time nobody has named; red is late and errors. Warm greys for text.
 * Depth: the route flat on the ground, the selected block and the pinned cards lifted, sheets,
 * menus and the drawer floating as glass over the blurred wallpaper.
 */
object T {
    // ---- colour ----
    val ground = Color(0xFF0A0B10)
    val surface1 = Color(0xFF13141B)
    val surface2 = Color(0xFF1A1C25)
    val surface3 = Color(0xFF23252F)
    val wash = Color.White.copy(alpha = 0.045f)
    val wash2 = Color.White.copy(alpha = 0.085f)
    val line = Color.White.copy(alpha = 0.06f)
    val line2 = Color.White.copy(alpha = 0.11f)
    /** Floating surfaces: translucent, over the blurred wallpaper. */
    val float = Color(0xD9161820)
    /** The day page's veil over the blurred wallpaper. */
    val veil = Color(0xF00A0B10)

    val text1 = Color(0xFFEFE9DF)
    val text2 = Color(0xFFB7AD9F)
    val text3 = Color(0xFF8A8174)

    val amber = Color(0xFFF49B3F)
    val amberHi = Color(0xFFFFB867)
    val onAmber = Color(0xFF1E1004)
    val amberEdge = amber.copy(alpha = 0.55f)
    val amberSoft = amber.copy(alpha = 0.15f)
    val amberGlow = amber.copy(alpha = 0.07f)
    val cyan = Color(0xFF74C9EE)
    val cyanSoft = cyan.copy(alpha = 0.09f)
    val cyanLine = cyan.copy(alpha = 0.45f)
    /** Time nobody has named: phone and place time with no label. */
    val unl = Color(0xFF6D707C)
    val unlSoft = Color.White.copy(alpha = 0.03f)
    val unlLine = Color.White.copy(alpha = 0.13f)
    val red = Color(0xFFF0766A)
    val redSoft = red.copy(alpha = 0.10f)
    val redEdge = red.copy(alpha = 0.35f)

    /** Ink on home over a pale wallpaper. */
    val inkDark = Color(0xFF16171D)
    val inkDark2 = Color(0xFF3C3A36)

    // ---- type ----
    private val base = TextStyle(fontFamily = Saira, lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None))
    private val num = TextStyle(fontFamily = SairaCondensed, fontFeatureSettings = "tnum", lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None))
    val label = base.copy(fontSize = 12.sp, lineHeight = 16.sp, color = text2)
    val body = base.copy(fontSize = 14.sp, lineHeight = 20.sp, color = text1)
    val row = base.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium, color = text1)
    val title = base.copy(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, color = text1)
    /** Small caps for kickers and section labels, at the meta size. */
    val caps = base.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.em, color = text3)
    /** Times on the route and in rows: condensed, tabular, at the meta size. */
    val time = num.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, color = text3)
    /** A time that is read, not scanned: the readout in a row. */
    val timeLg = num.copy(fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, color = amber)
    val readout = num.copy(fontSize = 48.sp, lineHeight = 50.sp, fontWeight = FontWeight.SemiBold, color = amber)
    val clock = num.copy(fontSize = 48.sp, lineHeight = 50.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.01).em)

    // ---- space: 8, 12, 16 and 24 for everything; time sets the route's vertical scale ----
    val s1 = 4.dp
    val s2 = 8.dp
    val s3 = 12.dp
    val s4 = 16.dp
    val s5 = 24.dp
    val s6 = 32.dp
    val rowMin = 52.dp
    val gutter = 16.dp
    val cardPad = PaddingValues(horizontal = 16.dp, vertical = 14.dp)

    // ---- corners: rounded everywhere, larger the further it floats ----
    val rSm = RoundedCornerShape(8.dp)
    val rMd = RoundedCornerShape(10.dp)
    val rLg = RoundedCornerShape(14.dp)
    val rFloat = RoundedCornerShape(18.dp)
    val rPill = RoundedCornerShape(999.dp)

    val kit = KitLook(
        glass = float, glassLine = line2, scrim = Color(0x66000000), text = text1, muted = text2, wash = wash2,
        radius = 22.dp, body = body, label = label, accent = amber, motionMs = 200,
    )
}
