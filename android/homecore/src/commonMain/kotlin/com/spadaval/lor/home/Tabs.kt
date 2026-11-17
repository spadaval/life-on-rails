package com.spadaval.lor.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spadaval.lor.ui.Type

/*
 * The drawer's tabs as the screens draw them: their kinds, their icons and the glyphs for those.
 * The app's tab bar and Home settings draw from here, and so does the lab.
 */

/** What a drawer tab shows: quick dial, a list of apps, or All (A–Z). */
enum class TabKind(val id: String) { QUICK("quick"), LIST("list"), ALL("all") }

/**
 * The icons a tab's button draws (TabBar.kt), by the name `drawer.tabs[].icon` gives, after the
 * launcher-use mocks. layout-v2.md lists them.
 */
enum class TabIcon(val id: String) {
    BOLT("bolt"), STAR("star"), BRIEF("brief"), CHAT("chat"), PLAY("play"), COMPASS("compass"), WALLET("wallet"), ALL("all");

    companion object {
        /** The icon named [id]; null for a name the phone doesn't draw, which draws the tab name's first letter. */
        fun of(id: String): TabIcon? = entries.firstOrNull { it.id == id }
    }
}

/** What a tab's button draws when its icon is unknown: its name's first letter or digit, upper case. */
fun tabLetter(name: String): String = name.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?"

/** A tab's icon, or its name's first letter when the file names an icon the phone doesn't draw. */
@Composable
fun TabGlyph(name: String, icon: String, color: Color) {
    val drawn = TabIcon.of(icon)
    if (drawn == null) {
        Text(tabLetter(name), style = Type.title.copy(color = color, fontSize = 17.sp, fontWeight = FontWeight.SemiBold))
    } else {
        IconGlyph(drawn, color)
    }
}

/** One of [TabIcon]'s icons, 21 dp. */
@Composable
fun IconGlyph(icon: TabIcon, color: Color) {
    Canvas(Modifier.size(21.dp)) { drawTabIcon(icon, color) }
}

/** Draws [icon] after the mocks' symbols, in a 24-unit square scaled to this canvas, 1.8-unit lines. */
private fun DrawScope.drawTabIcon(icon: TabIcon, color: Color) {
    val u = size.minDimension / 24f
    val line = Stroke(1.8f * u)
    fun o(x: Float, y: Float) = Offset(x * u, y * u)
    fun rect(x: Float, y: Float, w: Float, h: Float) = drawRect(color, o(x, y), Size(w * u, h * u), style = line)
    fun seg(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(color, o(x1, y1), o(x2, y2), 1.8f * u)
    fun path(vararg xy: Float, fill: Boolean) {
        val p = Path().apply {
            moveTo(xy[0] * u, xy[1] * u)
            for (i in 2 until xy.size step 2) lineTo(xy[i] * u, xy[i + 1] * u)
            close()
        }
        if (fill) drawPath(p, color) else drawPath(p, color, style = line)
    }
    when (icon) {
        TabIcon.BOLT -> path(13f, 2f, 4f, 14f, 11f, 14f, 10f, 22f, 19f, 10f, 12f, 10f, fill = true)
        // The mock's star is drawn on 20 units, centred here.
        TabIcon.STAR -> path(12f, 1.2f, 13.9f, 10.1f, 22.8f, 12f, 13.9f, 13.9f, 12f, 22.8f, 10.1f, 13.9f, 1.2f, 12f, 10.1f, 10.1f, fill = true)
        TabIcon.BRIEF -> {
            rect(3f, 7f, 18f, 13f)
            val handle = Path().apply { moveTo(9f * u, 7f * u); lineTo(9f * u, 4f * u); lineTo(15f * u, 4f * u); lineTo(15f * u, 7f * u) }
            drawPath(handle, color, style = line)
            seg(3f, 12f, 21f, 12f)
        }
        TabIcon.CHAT -> path(4f, 5f, 20f, 5f, 20f, 16f, 9f, 16f, 4f, 20f, fill = false)
        TabIcon.PLAY -> {
            rect(3f, 5f, 18f, 14f)
            path(10f, 9f, 15f, 12f, 10f, 15f, fill = true)
        }
        TabIcon.COMPASS -> {
            drawCircle(color, 9f * u, o(12f, 12f), style = line)
            path(15.5f, 8.5f, 13.5f, 13.5f, 8.5f, 15.5f, 10.5f, 10.5f, fill = false)
        }
        TabIcon.WALLET -> {
            rect(3f, 6f, 18f, 13f)
            seg(15f, 12.5f, 18f, 12.5f)
            seg(3f, 9f, 21f, 9f)
        }
        TabIcon.ALL -> listOf(4f to 4f, 14f to 4f, 4f to 14f, 14f to 14f).forEach { (x, y) -> drawRect(color, o(x, y), Size(6f * u, 6f * u)) }
    }
}
