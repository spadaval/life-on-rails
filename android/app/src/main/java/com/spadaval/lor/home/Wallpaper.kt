package com.spadaval.lor.home

import android.app.WallpaperManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.spadaval.lor.LorApp
import com.spadaval.lor.ui.Bg
import com.spadaval.lor.ui.Txt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/*
 * Over the wallpaper (launcher polish 6.3). Text home draws straight on the wallpaper (labels,
 * the date header, page dots) takes one of two inks, picked from the wallpaper's colour hints.
 * After Launcher3's `WallpaperColorHints` (Apache-2.0, via Lawnchair) and Kvaesitso's
 * `WallpaperColors.kt` (GPL-3.0, ideas only): the hints are read once and followed by a listener,
 * and a wallpaper Android says supports dark text gets dark text. DESIGN.md, "Over the wallpaper",
 * is Sumeet's written exception (2026-11-06) to "dark only" for this text; its shadow is for legibility.
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

/**
 * The system wallpaper's ink for the process (launcher polish 7.7), held by [HomeStore] and
 * followed by a listener for the process's life, so an activity made again (a theme change) has
 * the right ink in its first frame. The first read is off the main thread, started when the store
 * is made: `getWallpaperColors` is a binder call for colours the system keeps, but on the first
 * read after a boot the system extracts them during the call (34 ms on the emulator, over a
 * frame), so a cold start may draw one frame in light ink before it lands. The `LorLook` line
 * logs how long the read took.
 */
class WallpaperInk(context: Context, scope: CoroutineScope) {
    private val _ink = MutableStateFlow(Ink.LIGHT)
    val ink: StateFlow<Ink> = _ink

    init {
        val manager = WallpaperManager.getInstance(context)
        manager.addOnColorsChangedListener({ colors, which ->
            if (which and WallpaperManager.FLAG_SYSTEM != 0) {
                val now = inkFor(colors?.colorHints)
                if (now != _ink.value) Log.i("LorLook", "wallpaper hints: ${now.words}")
                _ink.value = now
            }
        }, Handler(Looper.getMainLooper()))
        scope.launch(Dispatchers.IO) {
            val started = SystemClock.elapsedRealtimeNanos()
            val first = inkFor(manager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)?.colorHints)
            val ms = (SystemClock.elapsedRealtimeNanos() - started) / 1_000_000.0
            Log.i("LorLook", "wallpaper hints: ${first.words} (read in ${"%.1f".format(ms)} ms)")
            _ink.value = first
        }
    }

    private val Ink.words: String get() = if (this == Ink.DARK) "dark text" else "light text"
}

/** The system wallpaper's ink, right from the first composition (launcher polish 7.7). */
@Composable
fun rememberWallpaperInk(): Ink {
    val store = (LocalContext.current.applicationContext as LorApp).home
    return store.ink.ink.collectAsState().value
}

/**
 * Whether home's window can blur the wallpaper behind it now (lor-gnk0's open folder): Android 12
 * and up with cross-window blur on, which the system turns off in battery saver, by the developer
 * option, and on devices without it. Followed by the system's listener while home is drawn.
 */
@Composable
fun rememberCrossWindowBlur(): Boolean {
    if (android.os.Build.VERSION.SDK_INT < 31) return false
    val context = LocalContext.current
    val windows = remember(context) { context.getSystemService(android.view.WindowManager::class.java) }
    var on by remember(windows) { mutableStateOf(windows.isCrossWindowBlurEnabled) }
    DisposableEffect(windows) {
        val listener = java.util.function.Consumer<Boolean> { on = it }
        windows.addCrossWindowBlurEnabledListener(listener)
        onDispose { windows.removeCrossWindowBlurEnabledListener(listener) }
    }
    return on
}
