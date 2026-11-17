package com.spadaval.lor.concepts.kit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Line icons drawn on a 24-unit grid with round ends, so a concept needs no icon font. Each is a
 * few strokes; [Glyph] names them by what they mean, not by what they look like.
 */
enum class Glyph { Search, More, Check, Close, Play, Stop, Swap, Chevron, ChevronDown, Back, Gear, Send, Warning, Sync, Plus, Agent, Clock, Calendar, Person, Task, Settings, Grid, Undo, Phone, Wifi, Battery }

@Composable
fun Icon(glyph: Glyph, color: Color, modifier: Modifier = Modifier, size: Dp = 20.dp, weight: Float = 1.7f) {
    Canvas(modifier.size(size)) {
        val u = this.size.minDimension / 24f
        draw(glyph, color, u, Stroke(width = weight * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

private fun DrawScope.draw(g: Glyph, c: Color, u: Float, s: Stroke) {
    fun p(x: Float, y: Float) = Offset(x * u, y * u)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(c, p(x1, y1), p(x2, y2), s.width, StrokeCap.Round)
    fun path(vararg pts: Pair<Float, Float>, close: Boolean = false) {
        val path = Path().apply {
            moveTo(pts[0].first * u, pts[0].second * u)
            for (i in 1 until pts.size) lineTo(pts[i].first * u, pts[i].second * u)
            if (close) close()
        }
        drawPath(path, c, style = s)
    }
    fun ring(x: Float, y: Float, r: Float) = drawCircle(c, r * u, p(x, y), style = s)
    fun dot(x: Float, y: Float, r: Float = 1.6f) = drawCircle(c, r * u, p(x, y))
    when (g) {
        Glyph.Search -> { ring(10.5f, 10.5f, 6.5f); line(15.5f, 15.5f, 20f, 20f) }
        Glyph.More -> { dot(5.5f, 12f); dot(12f, 12f); dot(18.5f, 12f) }
        Glyph.Check -> path(5f to 12.5f, 10f to 17.5f, 19f to 7f)
        Glyph.Close -> { line(6f, 6f, 18f, 18f); line(18f, 6f, 6f, 18f) }
        Glyph.Play -> { val t = Path().apply { moveTo(8f * u, 5.5f * u); lineTo(18.5f * u, 12f * u); lineTo(8f * u, 18.5f * u); close() }; drawPath(t, c) }
        Glyph.Stop -> drawRoundRect(c, p(6.5f, 6.5f), Size(11f * u, 11f * u), androidx.compose.ui.geometry.CornerRadius(2.5f * u))
        Glyph.Swap -> { path(4f to 9f, 18f to 9f); path(14f to 5f, 18f to 9f, 14f to 13f); path(20f to 15f, 6f to 15f); path(10f to 11f, 6f to 15f, 10f to 19f) }
        Glyph.Chevron -> path(9.5f to 6f, 15.5f to 12f, 9.5f to 18f)
        Glyph.ChevronDown -> path(6f to 9.5f, 12f to 15.5f, 18f to 9.5f)
        Glyph.Back -> { path(10f to 6f, 4f to 12f, 10f to 18f); line(4.5f, 12f, 20f, 12f) }
        Glyph.Gear, Glyph.Settings -> { ring(12f, 12f, 3.2f); for (i in 0 until 8) { val a = i * kotlin.math.PI / 4; val x1 = 12f + 6.2f * kotlin.math.cos(a).toFloat(); val y1 = 12f + 6.2f * kotlin.math.sin(a).toFloat(); val x2 = 12f + 8.6f * kotlin.math.cos(a).toFloat(); val y2 = 12f + 8.6f * kotlin.math.sin(a).toFloat(); line(x1, y1, x2, y2) }; ring(12f, 12f, 6.2f) }
        Glyph.Send -> { path(4f to 11.5f, 20f to 4f, 13f to 20f, 11f to 13f, close = true); line(11f, 13f, 20f, 4f) }
        Glyph.Warning -> { path(12f to 4f, 21f to 19.5f, 3f to 19.5f, close = true); line(12f, 10f, 12f, 14f); dot(12f, 16.8f, 1.2f) }
        Glyph.Sync -> { drawArc(c, 200f, 230f, false, p(5f, 5f), Size(14f * u, 14f * u), style = s); path(16.5f to 3.5f, 18.6f to 7.4f, 14.6f to 8.8f) }
        Glyph.Plus -> { line(12f, 5f, 12f, 19f); line(5f, 12f, 19f, 12f) }
        Glyph.Agent -> { path(12f to 3.5f, 14f to 10f, 20.5f to 12f, 14f to 14f, 12f to 20.5f, 10f to 14f, 3.5f to 12f, 10f to 10f, close = true) }
        Glyph.Clock -> { ring(12f, 12f, 8f); path(12f to 7.5f, 12f to 12f, 15f to 14f) }
        Glyph.Calendar -> { drawRoundRect(c, p(4f, 5.5f), Size(16f * u, 14.5f * u), androidx.compose.ui.geometry.CornerRadius(2.5f * u), style = s); line(4f, 10f, 20f, 10f); line(8.5f, 3.5f, 8.5f, 7f); line(15.5f, 3.5f, 15.5f, 7f) }
        Glyph.Person -> { ring(12f, 8.5f, 3.6f); drawArc(c, 200f, 140f, false, p(5f, 13.5f), Size(14f * u, 12f * u), style = s) }
        Glyph.Task -> { drawRoundRect(c, p(4.5f, 4.5f), Size(15f * u, 15f * u), androidx.compose.ui.geometry.CornerRadius(4f * u), style = s); path(8.5f to 12f, 11f to 14.5f, 15.5f to 9.5f) }
        Glyph.Grid -> { for (x in listOf(5f, 13.5f)) for (y in listOf(5f, 13.5f)) drawRoundRect(c, p(x, y), Size(5.5f * u, 5.5f * u), androidx.compose.ui.geometry.CornerRadius(1.5f * u), style = s) }
        Glyph.Undo -> { drawArc(c, 180f, 200f, false, p(5f, 7f), Size(14f * u, 12f * u), style = s); path(4f to 8f, 5f to 13f, 10f to 11.5f) }
        Glyph.Phone -> path(7f to 4f, 10f to 4f, 11.5f to 8f, 9.5f to 10f, 14f to 14.5f, 16f to 12.5f, 20f to 14f, 20f to 17f, 17.5f to 20f, 4f to 6.5f, close = true)
        Glyph.Wifi -> { drawArc(c, 225f, 90f, false, p(3f, 6f), Size(18f * u, 18f * u), style = s); drawArc(c, 225f, 90f, false, p(7f, 10f), Size(10f * u, 10f * u), style = s); dot(12f, 17.5f, 1.4f) }
        Glyph.Battery -> { drawRoundRect(c, p(3f, 8f), Size(16f * u, 9f * u), androidx.compose.ui.geometry.CornerRadius(2f * u), style = s); drawRect(c, p(5.2f, 10.2f), Size(9f * u, 4.6f * u)); line(21f, 11f, 21f, 14f) }
    }
}
