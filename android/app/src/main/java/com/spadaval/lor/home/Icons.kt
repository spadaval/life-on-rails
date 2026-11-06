package com.spadaval.lor.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.util.Log
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap
import com.spadaval.lor.ui.Line
import com.spadaval.lor.ui.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * App icons, drawn to bitmaps off the main thread and kept in memory.
 *
 * After Kvaesitso (GPL-3.0, ideas only): `LauncherApp.loadIcon` reads the icon on
 * `Dispatchers.IO`, and `IconService` keeps an `LruCache` and clears it when the icon settings
 * change. After Lawnchair (Apache-2.0): `IconPackProvider` draws a pack's drawable for apps the
 * pack lists, and `wrapThemedData` an adaptive icon's monochrome layer on a plain background.
 *
 * What an icon is drawn from is [IconStyle] (launcher polish 6.5): the pack and the state of its
 * index, themed icons and the shape. A change of style clears the cache, and [AppIcon] is keyed
 * on the style, so composed tiles redraw. Each icon is drawn at one size, [sizePx], and every place
 * that draws it scales the bitmap. A changed package's icons are evicted by [HomeModel].
 * Increment 8 moves this cache into `HomeStore`, keyed on size too.
 */
class Icons(private val context: Context, private val sizePx: Int) {
    private val cache = LruCache<String, ImageBitmap>(300)
    @Volatile private var style: IconStyle? = null

    /** Starts drawing with [now]; a different style than before drops every cached icon. */
    fun restyle(now: IconStyle) {
        if (now != style) {
            style = now
            cache.evictAll()
        }
    }

    fun cached(app: HomeApp, style: IconStyle): ImageBitmap? = if (style == this.style) cache.get(app.key) else null

    suspend fun load(app: HomeApp, style: IconStyle, pack: PackIndex?): ImageBitmap? = cached(app, style) ?: withContext(Dispatchers.IO) {
        try {
            draw(app, style, pack).asImageBitmap().also {
                Log.d("LorIcons", "loaded ${app.key}")
                if (style == this@Icons.style) cache.put(app.key, it)
            }
        } catch (e: Exception) {
            Log.w("LorHome", "no icon for ${app.key}", e)
            null
        }
    }

    private fun draw(app: HomeApp, style: IconStyle, pack: PackIndex?): Bitmap {
        val component = "${app.packageName}/${app.info.componentName.className}"
        val packIndex = pack?.takeIf { it.packageName == style.pack }
        val own by lazy { app.info.getIcon(0) }
        val mono = if (style.themed && Build.VERSION.SDK_INT >= 33) (own as? AdaptiveIconDrawable)?.monochrome else null
        val drawable: Drawable = when (iconSource(packIndex?.has(component) == true, style.themed, mono != null)) {
            IconSource.PACK -> packIndex?.drawable(component, context.resources.displayMetrics.densityDpi) ?: own
            // DESIGN.md: the glyph in Dust on Panel Face, no colour for decoration.
            IconSource.THEMED -> AdaptiveIconDrawable(ColorDrawable(Palette.PANEL.toInt()), mono!!.mutate().apply { setTint(Palette.MUTE.toInt()) })
            IconSource.OWN -> {
                // The app's own icon in the system's shape: as before, badged by Android.
                if (style.shape == HomeLook.IconShape.SYSTEM) return app.info.getBadgedIcon(0).toBitmap(sizePx, sizePx)
                own
            }
        }
        val shaped = shape(drawable, style.shape)
        if (app.info.user == Process.myUserHandle()) return shaped
        val badged = context.packageManager.getUserBadgedIcon(BitmapDrawable(context.resources, shaped), app.info.user)
        return badged.toBitmap(sizePx, sizePx)
    }

    /** An adaptive icon under [shape]'s mask; any other drawable as it is. */
    private fun shape(d: Drawable, shape: HomeLook.IconShape): Bitmap {
        if (d !is AdaptiveIconDrawable || shape == HomeLook.IconShape.SYSTEM) return d.toBitmap(sizePx, sizePx)
        val out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val s = sizePx.toFloat()
        canvas.clipPath(Path().apply {
            if (shape == HomeLook.IconShape.CIRCLE) addCircle(s / 2, s / 2, s / 2, Path.Direction.CW)
            else addRect(0f, 0f, s, s, Path.Direction.CW)
        })
        d.setBounds(0, 0, sizePx, sizePx)
        d.background?.draw(canvas)
        d.foreground?.draw(canvas)
        return out
    }

    fun evict(packages: List<String>, user: UserHandle) {
        val prefixes = packages.map { "$it/" }
        cache.snapshot().keys
            .filter { key -> key.endsWith("#$user") && prefixes.any { key.startsWith(it) } }
            .forEach { cache.remove(it) }
    }
}

/**
 * What icons are drawn from (launcher polish 6.5): the look's pack, themed icons and shape, and
 * [packVersion], the pack index's state (0 while it isn't in, else its version), so icons drawn
 * before the index arrived are drawn again when it does, and a rebuilt or dropped index never
 * leaves stale ones.
 */
data class IconStyle(val pack: String?, val packVersion: Int, val themed: Boolean, val shape: HomeLook.IconShape) {
    companion object {
        fun of(look: HomeLook, index: PackIndex?): IconStyle =
            IconStyle(look.iconPack, index?.takeIf { it.packageName == look.iconPack }?.version ?: 0, look.themedIcons, look.iconShape)
    }
}

val LocalIcons = staticCompositionLocalOf<Icons> { error("no Icons") }
val LocalIconStyle = staticCompositionLocalOf { IconStyle(null, 0, false, HomeLook.IconShape.SYSTEM) }
val LocalPackIndex = staticCompositionLocalOf<PackIndex?> { null }

@Composable
fun AppIcon(app: HomeApp, size: Dp, modifier: Modifier = Modifier) {
    val icons = LocalIcons.current
    val style = LocalIconStyle.current
    val pack = LocalPackIndex.current
    // Keyed on the app and the style: a slot that now shows another app, or a new style, must not keep the old icon.
    var bitmap by remember(app, style) { mutableStateOf(icons.cached(app, style)) }
    LaunchedEffect(app, style) { if (bitmap == null) bitmap = icons.load(app, style, pack) }
    val b = bitmap
    // Until it loads, the still slot home's skeleton draws (launcher polish 3.2), so the icon arrives in place.
    if (b != null) Image(b, contentDescription = null, modifier = modifier.size(size))
    else Box(modifier.size(size).background(Line, CircleShape))
}

/** Provides [icons] with the style the [look] and the pack [index] give, restyling the cache when it changes. */
@Composable
fun ProvideIcons(icons: Icons, look: HomeLook?, index: PackIndex?, content: @Composable () -> Unit) {
    val style = IconStyle.of(look ?: HomeLook(), index)
    remember(style) { icons.restyle(style) }
    androidx.compose.runtime.CompositionLocalProvider(LocalIcons provides icons, LocalIconStyle provides style, LocalPackIndex provides index, content = content)
}
