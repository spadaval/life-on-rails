package com.spadaval.lor.home

import android.app.SearchManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Where a search that matches no app can go (launcher polish 3.4): the web, the app store, or the
 * agent through quick send. The web row is Kvaesitso's `WebsearchAction` (GPL-3.0, ideas only:
 * `Intent.ACTION_WEB_SEARCH` with `SearchManager.QUERY`); the store row is the market-search row
 * Launcher3 once had under its empty search (`VIEW_TYPE_SEARCH_MARKET`, Lawnchair, Apache-2.0).
 * Kvaesitso shows its actions on every search; here they show only when nothing matches.
 */

/** One way out of an empty search. [intent] is null for [Kind.AGENT], which opens quick send. */
class Elsewhere(val kind: Kind, val text: String, val intent: Intent?) {
    enum class Kind { WEB, STORE, AGENT }
}

/** The rows for [query]: each of web and store only when an app takes it, then the agent. */
internal fun elsewhere(pm: PackageManager, query: String): List<Elsewhere> {
    val q = query.trim()
    val web = Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, q)
    val store = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=" + Uri.encode(q)))
    return listOfNotNull(
        Elsewhere(Elsewhere.Kind.WEB, "Search the web for \"$q\"", web).takeIf { takes(pm, web) },
        storeName(pm, store)?.let { Elsewhere(Elsewhere.Kind.STORE, "Search $it for \"$q\"", store) },
        Elsewhere(Elsewhere.Kind.AGENT, "Ask the agent", null),
    )
}

private fun takes(pm: PackageManager, intent: Intent) = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()

/** The app that takes [intent], "app stores" when Android would ask which, or null when none does. */
private fun storeName(pm: PackageManager, intent: Intent): String? {
    if (!takes(pm, intent)) return null
    val pkg = takenBy(pm, intent) ?: return "app stores"
    return runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault("app stores")
}

/** The package that opens [intent] without asking; null when Android asks (its chooser is `android`). */
internal fun takenBy(pm: PackageManager, intent: Intent): String? =
    pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName?.takeIf { it != "android" }

/** "No app called …" and the rows under it. */
@Composable
internal fun NoResults(query: String, ways: List<Elsewhere>, go: (Elsewhere) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(
            "No app called \"${query.trim()}\".",
            style = TextStyle(color = Muted, fontSize = 14.sp),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        ways.forEach { way ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { go(way) }
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Glyph(way.kind)
                Text(
                    way.text,
                    style = TextStyle(color = Ink, fontSize = 15.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
        }
    }
}

/** A still line drawing for each row: a magnifier, a bag, the send plane. */
@Composable
private fun Glyph(kind: Elsewhere.Kind) {
    Canvas(Modifier.size(20.dp)) {
        val w = size.width
        val line = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
        when (kind) {
            Elsewhere.Kind.WEB -> {
                drawCircle(Muted, radius = 0.32f * w, center = Offset(0.42f * w, 0.42f * w), style = line)
                drawLine(Muted, Offset(0.66f * w, 0.66f * w), Offset(0.92f * w, 0.92f * w), line.width, StrokeCap.Round)
            }
            Elsewhere.Kind.STORE -> {
                drawRoundRect(Muted, Offset(0.12f * w, 0.32f * w), Size(0.76f * w, 0.6f * w), CornerRadius(0.08f * w), style = line)
                drawArc(Muted, 180f, 180f, false, Offset(0.32f * w, 0.08f * w), Size(0.36f * w, 0.44f * w), style = line)
            }
            Elsewhere.Kind.AGENT -> {
                val plane = Path().apply {
                    moveTo(0.1f * w, 0.1f * w); lineTo(0.95f * w, 0.5f * w); lineTo(0.1f * w, 0.9f * w); lineTo(0.22f * w, 0.5f * w); close()
                }
                drawPath(plane, Muted, style = line)
            }
        }
    }
}
