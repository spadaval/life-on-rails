package com.spadaval.lor.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/* The Android half of HomeScreen.kt's open folder and minimap glass: the window's blur. */

/**
 * Blurs the wallpaper behind home's window by [shown] of DESIGN.md's 24 dp while [on], and clears
 * it when the folder has gone or blur goes off. Home's window is opaque (it shows the wallpaper),
 * and Android blurs only behind a translucent window's background (`setBackgroundBlurRadius`), so
 * this asks the window manager to blur what is behind the window (`FLAG_BLUR_BEHIND` with
 * `blurBehindRadius`, Android 12 and up), with no dim.
 */
@Composable
internal fun WindowBlurBehind(on: Boolean, view: android.view.View, shown: () -> Float) {
    val full = with(LocalDensity.current) { 24.dp.toPx() }
    LaunchedEffect(on, full) {
        val window = (view.context as? android.app.Activity)?.window
        if (!on || window == null || android.os.Build.VERSION.SDK_INT < 31) return@LaunchedEffect
        fun blur(radius: Int) {
            if (radius > 0) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.apply { blurBehindRadius = radius }
        }
        var radius = 0
        try {
            snapshotFlow { (shown() * full).roundToInt() }.collect {
                if (it != radius) blur(it)
                radius = it
            }
        } finally {
            if (radius != 0) blur(0)
        }
    }
}
