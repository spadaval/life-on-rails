package com.spadaval.lor.concepts.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spadaval.lor.lab.App
import com.spadaval.lor.ui.Saira

/*
 * An app's icon in the lab: the phone draws the app's own, the lab a rounded square in a colour
 * like the app's brand with its first letter, so a grid reads as apps rather than as boxes. Every
 * direction uses the same, so icons are not what tells two concepts apart.
 */
@Composable
fun AppIcon(app: App, size: Dp = 52.dp, modifier: Modifier = Modifier) {
    val c = brand(app)
    Box(
        modifier.size(size).clip(RoundedCornerShape(size * 0.3f))
            .background(Brush.verticalGradient(listOf(lighten(c, 0.12f), c))),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            app.label.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?",
            style = TextStyle(color = Color.White.copy(alpha = 0.95f), fontSize = (size.value * 0.42f).sp, fontWeight = FontWeight.SemiBold, fontFamily = Saira),
        )
    }
}

/** A folder's icon: up to four of its apps, small, on a pane of glass. */
@Composable
fun FolderIcon(apps: List<App>, glass: Color, size: Dp = 52.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(RoundedCornerShape(size * 0.3f)).background(glass).padding(size * 0.12f), contentAlignment = Alignment.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(size * 0.06f)) {
            apps.take(4).chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(size * 0.06f)) {
                    row.forEach { a -> Box(Modifier.size(size * 0.33f).clip(RoundedCornerShape(size * 0.1f)).background(brand(a))) }
                }
            }
        }
    }
}

/** A colour like the app's brand: known apps by package, the rest from a pleasant set by the label's hash. */
fun brand(app: App): Color = KNOWN[app.pkg] ?: SET[(app.label.hashCode() and 0x7fffffff) % SET.size]

private fun lighten(c: Color, by: Float) = Color(c.red + (1 - c.red) * by, c.green + (1 - c.green) * by, c.blue + (1 - c.blue) * by, c.alpha)

private val KNOWN = mapOf(
    "com.google.android.dialer" to Color(0xFF3B82F6),
    "com.google.android.apps.messaging" to Color(0xFF5B6CF0),
    "com.whatsapp" to Color(0xFF25A35A),
    "com.android.chrome" to Color(0xFFE8B730),
    "com.google.android.GoogleCamera" to Color(0xFF4A8FE0),
    "com.google.android.apps.photos" to Color(0xFFCB5BD3),
    "com.google.android.gm" to Color(0xFFDB5146),
    "com.google.android.calendar" to Color(0xFF3F7FE8),
    "com.google.android.apps.maps" to Color(0xFF34A26B),
    "com.google.android.deskclock" to Color(0xFF5A6070),
    "com.android.settings" to Color(0xFF6B7280),
    "com.android.vending" to Color(0xFF1FA67A),
    "com.Slack" to Color(0xFF7C3F8C),
    "com.github.android" to Color(0xFF30363D),
    "app.linear" to Color(0xFF5E6AD2),
    "com.figma.mirror" to Color(0xFFE2553D),
    "com.spotify.music" to Color(0xFF1DB954),
    "com.google.android.youtube" to Color(0xFFE53935),
    "com.strava" to Color(0xFFFC5A1F),
    "com.duolingo" to Color(0xFF58B92A),
    "com.amazon.kindle" to Color(0xFF1F6F9E),
    "com.ubercab" to Color(0xFF222428),
    "com.airbnb.android" to Color(0xFFF0506E),
    "com.revolut.revolut" to Color(0xFF2E66E6),
    "notion.id" to Color(0xFF3A3A3A),
    "com.t3tools.t3code" to Color(0xFFE07B2E),
    "com.spadaval.lor" to Color(0xFFC9772B),
    "com.microsoft.office.outlook" to Color(0xFF1C6FD1),
    "de.komoot.android" to Color(0xFF4F8C2B),
    "com.flytap.android" to Color(0xFF2CA26A),
)

private val SET = listOf(0xFF4F7CE0, 0xFF6C5CE0, 0xFF2E9E8F, 0xFFD9822B, 0xFFC0504D, 0xFF5D8F3A, 0xFF8E5BC8, 0xFF3A86A8).map { Color(it) }
