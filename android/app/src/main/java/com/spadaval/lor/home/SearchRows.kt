package com.spadaval.lor.home

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/*
 * Search results drawn as rows (launcher polish 5.2 on): shortcuts and conversations, settings
 * pages, contacts. Apps stay tiles. Go's target takes DESIGN.md's selection.
 */

/** A shortcut found by search, with the app it belongs to. */
class SearchShortcut(val hit: ShortcutHit, val owner: HomeApp)

/** Opens what search found: an app, a shortcut, … */
internal fun openFound(value: Any, at: At, home: HomeContext) {
    when (value) {
        is HomeApp -> home.actions.open(value, at)
        is SearchShortcut -> home.actions.openShortcut(value.owner, value.hit.info, at)
        is SettingsPage -> home.actions.openSetting(value, at.query.orEmpty())
    }
}

@Composable
internal fun ResultRow(value: Any, go: Boolean, at: At, home: HomeContext) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .then(if (go) Modifier.background(Amber.copy(alpha = 0.08f), shape).border(1.dp, Amber.copy(alpha = 0.55f), shape) else Modifier)
            .clickable(role = Role.Button) { openFound(value, at, home) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (value) {
            is SearchShortcut -> {
                ShortcutIcon(value, home)
                RowText(value.hit.label, if (value.hit.conversation) "Chat · ${value.owner.label}" else value.owner.label)
            }
            is SettingsPage -> {
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) { Gear() }
                RowText(value.title, "Settings")
            }
        }
    }
}

/** A still gear in the muted colour, drawn like the no-results rows' glyphs (3.4). */
@Composable
private fun Gear() {
    Canvas(Modifier.size(22.dp)) {
        val w = size.width
        val line = Stroke(width = 1.6.dp.toPx())
        drawCircle(Muted, radius = 0.16f * w, center = center, style = line)
        drawCircle(Muted, radius = 0.32f * w, center = center, style = line)
        for (k in 0 until 8) {
            val a = Math.toRadians(k * 45.0)
            val c = Offset(center.x + (0.32f * w * cos(a)).toFloat(), center.y + (0.32f * w * sin(a)).toFloat())
            val e = Offset(center.x + (0.46f * w * cos(a)).toFloat(), center.y + (0.46f * w * sin(a)).toFloat())
            drawLine(Muted, c, e, 2.4.dp.toPx(), StrokeCap.Round)
        }
    }
}

@Composable
private fun RowText(title: String, sub: String) {
    Column(Modifier.padding(start = 16.dp)) {
        Text(title, style = TextStyle(color = Ink, fontSize = 15.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(sub, style = TextStyle(color = Muted, fontSize = 12.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** The shortcut's icon, loaded when drawn; the app's icon until then (or when it has none). */
@Composable
private fun ShortcutIcon(s: SearchShortcut, home: HomeContext) {
    var icon by remember(s.hit.info.id, s.owner.key) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(s.hit.info.id, s.owner.key) { icon = home.actions.shortcutIcon(s.hit.info) }
    Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
        val b = icon
        if (b != null) Image(b, contentDescription = null, modifier = Modifier.size(36.dp)) else AppIcon(s.owner, 36.dp)
    }
}

/**
 * A quick answer (launcher polish 5.4): the value, and the query under it. A tap copies the value;
 * Android 13 and later confirm a copy themselves, so there is no toast.
 */
@Composable
internal fun AnswerCard(answer: Answer) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .background(StateFill, shape)
            .border(1.dp, Line, shape)
            .clickable(role = Role.Button, onClickLabel = "Copy") {
                context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(answer.input, answer.value))
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(answer.value, style = TextStyle(color = Ink, fontSize = 22.sp))
        Text(answer.input, style = TextStyle(color = Muted, fontSize = 13.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
