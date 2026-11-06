package com.spadaval.lor.home

import androidx.compose.ui.graphics.RectangleShape
import com.spadaval.lor.ui.*
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
internal fun openFound(value: Any, at: At, home: HomeContext, icon: IconSpot?) {
    when (value) {
        is HomeApp -> home.actions.open(value, at, icon)
        is SearchShortcut -> home.actions.openShortcut(value.owner, value.hit.info, at, icon)
        is SettingsPage -> home.actions.openSetting(value, at.query.orEmpty())
        is SearchContact -> home.actions.openContact(value, ContactWay.OPEN, at.query.orEmpty())
    }
}

@Composable
internal fun ResultRow(value: Any, go: Boolean, at: At, home: HomeContext, icon: IconSpot) {
    val shape = RectangleShape
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .then(if (go) Modifier.background(Or.copy(alpha = 0.08f), shape).border(1.dp, Or.copy(alpha = 0.55f), shape) else Modifier)
            .clickable(role = Role.Button) { openFound(value, at, home, icon) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (value) {
            is SearchShortcut -> {
                ShortcutIcon(value, home, Modifier.iconSpot(icon))
                RowText(value.hit.label, if (value.hit.conversation) "Chat · ${value.owner.label}" else value.owner.label)
            }
            is SettingsPage -> {
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) { Gear() }
                RowText(value.title, "Settings")
            }
            is SearchContact -> {
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) { Person() }
                val number = value.numbers.firstOrNull()
                RowText(value.name, number ?: "Contact", Modifier.weight(1f))
                if (number != null) {
                    val q = at.query.orEmpty()
                    RowButton("Call ${value.name}", { Handset() }) { home.actions.openContact(value, ContactWay.CALL, q) }
                    RowButton("Message ${value.name}", { Bubble() }) { home.actions.openContact(value, ContactWay.MESSAGE, q) }
                }
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
        drawCircle(Mute, radius = 0.16f * w, center = center, style = line)
        drawCircle(Mute, radius = 0.32f * w, center = center, style = line)
        for (k in 0 until 8) {
            val a = Math.toRadians(k * 45.0)
            val c = Offset(center.x + (0.32f * w * cos(a)).toFloat(), center.y + (0.32f * w * sin(a)).toFloat())
            val e = Offset(center.x + (0.46f * w * cos(a)).toFloat(), center.y + (0.46f * w * sin(a)).toFloat())
            drawLine(Mute, c, e, 2.4.dp.toPx(), StrokeCap.Round)
        }
    }
}

@Composable
private fun RowText(title: String, sub: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(start = 16.dp)) {
        Text(title, style = Type.body.copy(color = Txt, fontSize = 15.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(sub, style = Type.body.copy(color = Mute, fontSize = 12.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** The shortcut's icon, loaded when drawn; the app's icon until then (or when it has none). */
@Composable
private fun ShortcutIcon(s: SearchShortcut, home: HomeContext, modifier: Modifier) {
    var icon by remember(s.hit.info.id, s.owner.key) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(s.hit.info.id, s.owner.key) { icon = home.actions.shortcutIcon(s.hit.info) }
    Box(modifier.size(36.dp), contentAlignment = Alignment.Center) {
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
    val shape = Chamfer
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .framedPanel(StateFill)
            .clickable(role = Role.Button, onClickLabel = "Copy") {
                context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(answer.input, answer.value))
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(answer.value, style = Type.body.copy(color = Txt, fontSize = 22.sp))
        Text(answer.input, style = Type.body.copy(color = Mute, fontSize = 13.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/*
 * Contacts (launcher polish 5.6). Kvaesitso (GPL-3.0, `AndroidContactProvider.kt`, ideas only)
 * shows a contact with call and message actions; here `ACTION_DIAL` (the dialer opens with the
 * number, so no `CALL_PHONE`) and `ACTION_SENDTO` with `smsto:`.
 */

/** What a contact's row does: open the contact, dial its first number, or text it. */
enum class ContactWay { OPEN, CALL, MESSAGE }

/** The intent for [way] on [c]. */
internal fun contactIntent(c: SearchContact, way: ContactWay): Intent = when (way) {
    ContactWay.OPEN -> Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.getLookupUri(c.id, c.lookupKey))
    ContactWay.CALL -> Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", c.numbers.first(), null))
    ContactWay.MESSAGE -> Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", c.numbers.first(), null))
}

/** A 48 dp button at a row's end, read as [label]. */
@Composable
private fun RowButton(label: String, glyph: @Composable () -> Unit, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { glyph() }
}

/** A still person: a head and shoulders, in the muted colour. */
@Composable
private fun Person() {
    Canvas(Modifier.size(22.dp)) {
        val w = size.width
        val line = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(Mute, radius = 0.2f * w, center = Offset(0.5f * w, 0.32f * w), style = line)
        drawArc(Mute, 180f, 180f, false, Offset(0.14f * w, 0.62f * w), Size(0.72f * w, 0.6f * w), style = line)
    }
}

/** A handset, for Call. */
@Composable
private fun Handset() {
    Canvas(Modifier.size(20.dp)) {
        val w = size.width
        val line = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val p = Path().apply {
            moveTo(0.22f * w, 0.1f * w)
            lineTo(0.38f * w, 0.1f * w)
            lineTo(0.44f * w, 0.3f * w)
            lineTo(0.34f * w, 0.4f * w)
            quadraticTo(0.45f * w, 0.56f * w, 0.6f * w, 0.66f * w)
            lineTo(0.7f * w, 0.56f * w)
            lineTo(0.9f * w, 0.62f * w)
            lineTo(0.9f * w, 0.78f * w)
            quadraticTo(0.88f * w, 0.92f * w, 0.74f * w, 0.9f * w)
            quadraticTo(0.12f * w, 0.8f * w, 0.1f * w, 0.24f * w)
            quadraticTo(0.1f * w, 0.1f * w, 0.22f * w, 0.1f * w)
            close()
        }
        drawPath(p, Txt, style = line)
    }
}

/** A speech bubble, for Message. */
@Composable
private fun Bubble() {
    Canvas(Modifier.size(20.dp)) {
        val w = size.width
        val line = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val p = Path().apply {
            addRoundRect(RoundRect(0.1f * w, 0.14f * w, 0.9f * w, 0.7f * w, CornerRadius(0.12f * w)))
        }
        drawPath(p, Txt, style = line)
        drawLine(Txt, Offset(0.3f * w, 0.7f * w), Offset(0.22f * w, 0.9f * w), line.width, StrokeCap.Round)
        drawLine(Txt, Offset(0.22f * w, 0.9f * w), Offset(0.46f * w, 0.7f * w), line.width, StrokeCap.Round)
    }
}
