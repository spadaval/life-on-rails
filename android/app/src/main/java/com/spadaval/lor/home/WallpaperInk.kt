package com.spadaval.lor.home

import android.app.WallpaperManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/*
 * The Android half of Wallpaper.kt (launcher polish 6.3): the system wallpaper's colour hints, read
 * and followed for the process, and whether the window can blur. Its ink is drawn in homecore.
 */

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
