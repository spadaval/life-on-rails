package com.spadaval.lor.concepts.kit

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spadaval.lor.lab.LocalWallpaper

/*
 * What the kit's pieces draw with. A direction makes one KitLook from its own tokens and provides
 * it around its tree; the pieces read nothing else, so they carry no direction's look.
 */
class KitLook(
    /** A floating surface (a sheet, a menu, the drawer), translucent over the blurred wallpaper. */
    val glass: Color,
    /** The hairline at a floating surface's edge. */
    val glassLine: Color,
    /** Over what is behind an open sheet. */
    val scrim: Color,
    val text: Color,
    val muted: Color,
    /** A pressed or hovered row's wash. */
    val wash: Color,
    /** The corner of floating surfaces. */
    val radius: Dp,
    val body: TextStyle,
    val label: TextStyle,
    /** Undo's colour on the toast. */
    val accent: Color,
    /** How long a surface takes to arrive or leave, in ms (the brief: 150 to 250). */
    val motionMs: Int = 220,
)

val LocalKitLook = staticCompositionLocalOf<KitLook> { error("A direction provides its KitLook around its tree") }

/**
 * The wallpaper again, blurred, at [shown] (0 to 1): what the phone's window blur gives behind a
 * sheet or the drawer. Draw it under the floating surface and over the page it covers.
 */
@Composable
fun BlurredWallpaper(shown: () -> Float, radius: Dp = 28.dp) {
    val image = LocalWallpaper.current ?: return
    Image(
        image, contentDescription = null, contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize().graphicsLayer { alpha = shown() }.blur(radius),
    )
}

/**
 * What floats blurs what it covers: wrap the pages in this, and while [shown] (0 to 1) rises they
 * blur and the wallpaper under them turns to its blurred copy, as the phone's window blur does
 * behind a sheet. A [Sheet] draws only its scrim and itself, so this is what gives it depth.
 */
@Composable
fun Backdrop(shown: () -> Float, radius: Dp = 18.dp, content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().graphicsLayer {
            val r = radius.toPx() * shown()
            renderEffect = if (r > 0.5f) BlurEffect(r, r, TileMode.Decal) else null
        },
    ) {
        BlurredWallpaper(shown)
        content()
    }
}
