package com.spadaval.lor.home

import android.os.UserHandle
import android.util.Log
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * App icons, drawn to bitmaps off the main thread and kept in memory.
 *
 * After Kvaesitso (GPL-3.0): `LauncherApp.loadIcon` reads `LauncherActivityInfo.getIcon` on
 * `Dispatchers.IO`, and `IconService` keeps an `LruCache` keyed by the app. Here the icon is the
 * badged one (a work-profile app shows its briefcase) at one size, [sizePx], and every place that
 * draws an icon scales that bitmap. A changed package's icons are evicted by [HomeModel].
 */
class Icons(private val sizePx: Int) {
    private val cache = LruCache<String, ImageBitmap>(300)

    fun cached(app: HomeApp): ImageBitmap? = cache.get(app.key)

    suspend fun load(app: HomeApp): ImageBitmap? = cache.get(app.key) ?: withContext(Dispatchers.IO) {
        try {
            app.info.getBadgedIcon(0).toBitmap(sizePx, sizePx).asImageBitmap().also { cache.put(app.key, it) }
        } catch (e: Exception) {
            Log.w("LorHome", "no icon for ${app.key}", e)
            null
        }
    }

    fun evict(packages: List<String>, user: UserHandle) {
        val prefixes = packages.map { "$it/" }
        cache.snapshot().keys
            .filter { key -> key.endsWith("#$user") && prefixes.any { key.startsWith(it) } }
            .forEach { cache.remove(it) }
    }
}

val LocalIcons = staticCompositionLocalOf<Icons> { error("no Icons") }

@Composable
fun AppIcon(app: HomeApp, size: Dp, modifier: Modifier = Modifier) {
    val icons = LocalIcons.current
    // Keyed on the app: a slot that now shows another app must not keep the old one's icon.
    var bitmap by remember(app) { mutableStateOf(icons.cached(app)) }
    LaunchedEffect(app) { if (bitmap == null) bitmap = icons.load(app) }
    val b = bitmap
    if (b != null) Image(b, contentDescription = null, modifier = modifier.size(size)) else Spacer(modifier.size(size))
}
