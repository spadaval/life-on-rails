package com.spadaval.lor.home

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.spadaval.lor.ui.Bg
import com.spadaval.lor.ui.Txt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/*
 * Over the wallpaper (launcher polish 6.3). Text home draws straight on the wallpaper (labels,
 * the date header, page dots) takes one of two inks, picked from the wallpaper's colour hints.
 * After Launcher3's `WallpaperColorHints` (Apache-2.0, via Lawnchair) and Kvaesitso's
 * `WallpaperColors.kt` (GPL-3.0, ideas only): the hints are read once and followed by a listener,
 * and a wallpaper Android says supports dark text gets dark text. DESIGN.md, "Over the wallpaper",
 * is Sumeet's written exception (2026-11-06) to "dark only" and No-Glow for this text.
 */

/** Which ink text standing on the wallpaper takes. */
enum class Ink { LIGHT, DARK }

/** `WallpaperColors.HINT_SUPPORTS_DARK_TEXT`, kept here so the JVM tests need no Android. */
const val HINT_DARK_TEXT = 1

/** Dark ink when the wallpaper supports dark text; light otherwise, and when there are no colours (null). */
fun inkFor(hints: Int?): Ink = if (hints != null && hints and HINT_DARK_TEXT != 0) Ink.DARK else Ink.LIGHT

/** An ink's colours: [text] for words, [mark] for dots, and a shadow for light ink. */
class WallInk(val ink: Ink, val text: Color, val mark: Color, val shadow: Shadow?)

/** Light text on a surface LoR draws (the drawer, a folder panel): no shadow. */
val SurfaceInk = WallInk(Ink.LIGHT, Txt, Txt, null)

/** What [AppTile]'s label and the header draw with; home's pages provide the wallpaper's, panels keep [SurfaceInk]. */
val LocalWallInk = staticCompositionLocalOf { SurfaceInk }

@Composable
fun wallInk(ink: Ink): WallInk {
    val density = LocalDensity.current
    return remember(ink, density) {
        when (ink) {
            // A still shadow in dp, so light text stays legible over a light patch of a photo.
            Ink.LIGHT -> with(density) {
                WallInk(Ink.LIGHT, Txt, Txt, Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 1.dp.toPx()), 3.dp.toPx()))
            }
            // Deep Space for all text: on the darkest wallpaper with the hint (about #858585) it
            // reads at about 5.3:1, which a grey would not.
            Ink.DARK -> WallInk(Ink.DARK, Bg, Bg, null)
        }
    }
}

/** The system wallpaper's ink, followed while home is composed. */
@Composable
fun rememberWallpaperInk(): Ink {
    val context = LocalContext.current
    var ink by remember { mutableStateOf(Ink.LIGHT) }
    DisposableEffect(context) {
        val manager = WallpaperManager.getInstance(context)
        fun take(colors: WallpaperColors?) {
            val now = inkFor(colors?.colorHints)
            if (now != ink) Log.i("LorLook", "wallpaper hints: ${if (now == Ink.DARK) "dark text" else "light text"}")
            ink = now
        }
        val listener = WallpaperManager.OnColorsChangedListener { colors, which ->
            if (which and WallpaperManager.FLAG_SYSTEM != 0) take(colors)
        }
        manager.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper()))
        onDispose { manager.removeOnColorsChangedListener(listener) }
    }
    // The first read off the main thread: getWallpaperColors may compute them.
    androidx.compose.runtime.LaunchedEffect(context) {
        val colors = withContext(Dispatchers.IO) { WallpaperManager.getInstance(context).getWallpaperColors(WallpaperManager.FLAG_SYSTEM) }
        val now = inkFor(colors?.colorHints)
        Log.i("LorLook", "wallpaper hints: ${if (now == Ink.DARK) "dark text" else "light text"}")
        ink = now
    }
    return ink
}
