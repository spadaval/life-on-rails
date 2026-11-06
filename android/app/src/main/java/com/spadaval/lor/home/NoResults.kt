package com.spadaval.lor.home

import com.spadaval.lor.ui.*
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
 * Kvaesitso shows its actions on every search; here they show only when nothing matches. Who
 * takes them is resolved off the main thread, not per keystroke (5.1).
 */

/** One way out of an empty search; the intent is built when it is taken ([intentFor]). */
class Elsewhere(val kind: Kind, val text: String) {
    enum class Kind { WEB, STORE, AGENT }
}

/**
 * Who takes a search elsewhere, resolved by [HomeStore] when the app list loads, a package
 * changes and home resumes (launcher polish 5.1, R5), never per keystroke: [web] is the package
 * that takes a web search ("" when Android would ask), [store] the label of what takes
 * `market://search` ("app stores" when Android would ask); null when nothing does.
 */
data class SearchTargets(val web: String?, val store: String?)

/** The rows for [query]: each of web and store only when an app takes it, then the agent. */
internal fun elsewhere(targets: SearchTargets, query: String): List<Elsewhere> {
    val q = query.trim()
    return listOfNotNull(
        targets.web?.let { Elsewhere(Elsewhere.Kind.WEB, "Search the web for \"$q\"") },
        targets.store?.let { Elsewhere(Elsewhere.Kind.STORE, "Search $it for \"$q\"") },
        Elsewhere(Elsewhere.Kind.AGENT, "Ask the agent about \"$q\""),
    )
}

/** The intent for [way] and [query]; null for the agent, which opens quick send. */
internal fun intentFor(way: Elsewhere, query: String): Intent? = when (way.kind) {
    Elsewhere.Kind.WEB -> webSearch(query.trim())
    Elsewhere.Kind.STORE -> storeSearch(query.trim())
    Elsewhere.Kind.AGENT -> null
}

private fun webSearch(q: String) = Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, q)

private fun storeSearch(q: String) = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=" + Uri.encode(q)))

/** Resolves [SearchTargets]; asks the package manager, so it runs off the main thread. */
internal fun searchTargets(pm: PackageManager): SearchTargets {
    val web = webSearch("").takeIf { takes(pm, it) }?.let { takenBy(pm, it) ?: "" }
    val store = storeSearch("").takeIf { takes(pm, it) }?.let { intent ->
        val pkg = takenBy(pm, intent) ?: return@let "app stores"
        runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault("app stores")
    }
    return SearchTargets(web, store)
}

private fun takes(pm: PackageManager, intent: Intent) = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()

/** The package that opens [intent] without asking; null when Android asks (its chooser is `android`). */
internal fun takenBy(pm: PackageManager, intent: Intent): String? =
    pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName?.takeIf { it != "android" }

/** "Nothing matches …" (left out under an answer) and the rows under it. */
@Composable
internal fun NoResults(query: String, ways: List<Elsewhere>, line: Boolean, go: (Elsewhere) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        if (line) Text(
            "Nothing matches \"${query.trim()}\".",
            style = Type.body.copy(color = Mute, fontSize = 14.sp),
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
                    style = Type.body.copy(color = Txt, fontSize = 15.sp),
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
                drawCircle(Mute, radius = 0.32f * w, center = Offset(0.42f * w, 0.42f * w), style = line)
                drawLine(Mute, Offset(0.66f * w, 0.66f * w), Offset(0.92f * w, 0.92f * w), line.width, StrokeCap.Round)
            }
            Elsewhere.Kind.STORE -> {
                drawRoundRect(Mute, Offset(0.12f * w, 0.32f * w), Size(0.76f * w, 0.6f * w), CornerRadius(0.08f * w), style = line)
                drawArc(Mute, 180f, 180f, false, Offset(0.32f * w, 0.08f * w), Size(0.36f * w, 0.44f * w), style = line)
            }
            Elsewhere.Kind.AGENT -> {
                val plane = Path().apply {
                    moveTo(0.1f * w, 0.1f * w); lineTo(0.95f * w, 0.5f * w); lineTo(0.1f * w, 0.9f * w); lineTo(0.22f * w, 0.5f * w); close()
                }
                drawPath(plane, Mute, style = line)
            }
        }
    }
}
