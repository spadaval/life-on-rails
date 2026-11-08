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
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap
import com.spadaval.lor.ui.Line
import com.spadaval.lor.ui.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * App icons, drawn to bitmaps off the main thread and kept in memory for the process (lor-ozj2):
 * one [Icons] in [HomeStore], used by home and Home settings alike, so a recreated activity, a
 * fold or a font change draws nothing again.
 *
 * After Kvaesitso (GPL-3.0, ideas only): `LauncherApp.loadIcon` reads the icon on
 * `Dispatchers.IO`, and `IconService` keeps an `LruCache`. After Lawnchair (Apache-2.0):
 * `IconPackProvider` draws a pack's drawable for apps the pack lists, and `wrapThemedData` an
 * adaptive icon's monochrome layer on a plain background.
 *
 * What an icon is drawn from is [IconStyle] (launcher polish 6.5): the pack and the state of its
 * index, themed icons and the shape. Each icon is drawn at a size in px that the caller works out
 * from the density at the time it draws ([AppIcon]), so the Fold's two screens each get their own,
 * and every place that shows it scales the bitmap. The cache ([IconCache]) is keyed by app, style
 * and size; a changed package's icons are evicted by the store.
 */
class Icons(private val context: Context) {
    val cache = IconCache<ImageBitmap>(MAX_BYTES) { it.asAndroidBitmap().byteCount }

    fun cached(app: HomeApp, style: IconStyle, sizePx: Int): ImageBitmap? = cache.get(keyOf(app, style, sizePx))

    suspend fun load(app: HomeApp, style: IconStyle, pack: PackIndex?, sizePx: Int): ImageBitmap? {
        val key = keyOf(app, style, sizePx)
        cache.get(key)?.let { return it }
        // Taken before drawing: a package change while it draws keeps the result out of the cache.
        val ticket = cache.ticket(key)
        return withContext(Dispatchers.IO) {
            try {
                draw(app, style, pack, sizePx).asImageBitmap().also {
                    Log.d("LorIcons", "drew ${app.key} at $sizePx px")
                    cache.put(key, it, ticket)
                }
            } catch (e: Exception) {
                Log.w("LorHome", "no icon for ${app.key}", e)
                null
            }
        }
    }

    fun evict(packages: List<String>, user: UserHandle) = cache.evict(packages, user.toString())

    private fun keyOf(app: HomeApp, style: IconStyle, sizePx: Int) = IconKey(app.key, app.packageName, app.user.toString(), style, sizePx)

    private fun draw(app: HomeApp, style: IconStyle, pack: PackIndex?, sizePx: Int): Bitmap {
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
        val shaped = shape(drawable, style.shape, sizePx)
        if (app.info.user == Process.myUserHandle()) return shaped
        val badged = context.packageManager.getUserBadgedIcon(BitmapDrawable(context.resources, shaped), app.info.user)
        return badged.toBitmap(sizePx, sizePx)
    }

    /** An adaptive icon under [shape]'s mask; any other drawable as it is. */
    private fun shape(d: Drawable, shape: HomeLook.IconShape, sizePx: Int): Bitmap {
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

    companion object {
        /** Room for every app at three sizes (home's on each screen, Home settings'), at about 85 KB an icon. */
        private const val MAX_BYTES = 48 * 1024 * 1024
    }
}

/** An icon in the cache: the app ([HomeApp.key]), its package and profile for eviction, the style and the size in px. */
data class IconKey(val app: String, val pkg: String, val user: String, val style: IconStyle, val sizePx: Int)

/**
 * The icon cache's rules, with no Android types so the JVM tests reach them: least recently used
 * first out past [maxBytes], and a package change ([evict]) both drops that package's icons and
 * keeps out any icon of it whose draw began before (the icon-cache race, T3 Launcher 3.2): a draw
 * takes a [ticket] when it starts and [put] refuses it if the package was evicted since.
 */
class IconCache<V : Any>(private val maxBytes: Int, private val sizeOf: (V) -> Int) {
    private val map = LinkedHashMap<IconKey, V>(64, 0.75f, true)
    /** How many times each package (`pkg#user`) has been evicted. */
    private val evictions = HashMap<String, Int>()
    private var bytes = 0

    @Synchronized fun get(key: IconKey): V? = map[key]

    /** Taken when a draw of [key] starts. */
    @Synchronized fun ticket(key: IconKey): Int = evictions[key.pkg + "#" + key.user] ?: 0

    /** Keeps [value] unless its package was evicted since [ticket]; returns whether it was kept. */
    @Synchronized fun put(key: IconKey, value: V, ticket: Int): Boolean {
        if (ticket(key) != ticket) return false
        map.put(key, value)?.let { bytes -= sizeOf(it) }
        bytes += sizeOf(value)
        val oldest = map.entries.iterator()
        while (bytes > maxBytes && oldest.hasNext()) {
            val e = oldest.next()
            if (e.key == key) continue
            bytes -= sizeOf(e.value)
            oldest.remove()
        }
        return true
    }

    /** [packages] of [user] changed: their icons go, and draws already under way aren't kept. */
    @Synchronized fun evict(packages: List<String>, user: String) {
        for (pkg in packages) evictions.merge("$pkg#$user", 1, Int::plus)
        val gone = map.entries.iterator()
        while (gone.hasNext()) {
            val e = gone.next()
            if (e.key.user == user && e.key.pkg in packages) {
                bytes -= sizeOf(e.value)
                gone.remove()
            }
        }
    }

    @get:Synchronized val size: Int get() = map.size
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
/** The size icons are drawn at, before scaling to where they show: home's largest, Home settings' rows. */
val LocalIconDrawSize = staticCompositionLocalOf { HomeLook.ICON_SIZES.last.dp }

@Composable
fun AppIcon(app: HomeApp, size: Dp, modifier: Modifier = Modifier) {
    val icons = LocalIcons.current
    val style = LocalIconStyle.current
    val pack = LocalPackIndex.current
    // From the density now, so an unfold to a screen of another density draws its own (lor-ozj2).
    val px = with(LocalDensity.current) { LocalIconDrawSize.current.roundToPx() }
    // Keyed on the app, the style and the size: a slot that now shows another app, or a new style, must not keep the old icon.
    var bitmap by remember(app, style, px) { mutableStateOf(icons.cached(app, style, px)) }
    LaunchedEffect(app, style, px) { if (bitmap == null) bitmap = icons.load(app, style, pack, px) }
    val b = bitmap
    // Until it loads, the still slot home's skeleton draws (launcher polish 3.2), so the icon arrives in place.
    if (b != null) Image(b, contentDescription = null, modifier = modifier.size(size))
    else Box(modifier.size(size).background(Line, CircleShape))
}

/** Provides [icons] with the style the [look] and the pack [index] give, drawn at [drawSize]. */
@Composable
fun ProvideIcons(icons: Icons, look: HomeLook?, index: PackIndex?, drawSize: Dp = HomeLook.ICON_SIZES.last.dp, content: @Composable () -> Unit) {
    val style = IconStyle.of(look ?: HomeLook(), index)
    androidx.compose.runtime.CompositionLocalProvider(
        LocalIcons provides icons, LocalIconStyle provides style, LocalPackIndex provides index, LocalIconDrawSize provides drawSize, content = content,
    )
}
