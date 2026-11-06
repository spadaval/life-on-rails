package com.spadaval.lor.home

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/*
 * Icon packs (launcher polish 6.5). After Lawnchair (Apache-2.0, 15-dev): `CustomIconPack` reads
 * the pack's `appfilter` (res/xml, else assets) into a map from component to drawable, unwrapping
 * `ComponentInfo{…}`; `IconPackProvider` keeps one parsed pack for the process and an app the pack
 * lacks keeps its own icon. After Kvaesitso (GPL-3.0, ideas only): `AppFilterIconPackInstaller`
 * finds packs by ADW's and Nova's theme intents and also reads res/raw. Not borrowed: a pack's
 * iconback/iconmask/upon/scale, calendar and clock icons, and storing packs in a database.
 */

/** The intents icon packs answer: ADW's two and Nova's. */
private val PACK_ACTIONS = listOf("org.adw.launcher.THEMES", "org.adw.ActivityStarter.THEMES", "com.novalauncher.THEME")

/** An installed icon pack: its package and label. */
data class InstalledPack(val packageName: String, val label: String)

/** Packs installed now, one per package, by label. Read when Home settings asks, not kept. */
fun installedPacks(pm: PackageManager): List<InstalledPack> =
    PACK_ACTIONS.flatMap { pm.queryIntentActivities(Intent(it), 0) }
        .distinctBy { it.activityInfo.packageName }
        .map { InstalledPack(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
        .sortedBy { it.label.lowercase() }

/**
 * `appfilter`'s (component, drawable) pairs as a map from `package/class` to the drawable's name.
 * `ComponentInfo{…}` is unwrapped, a `.Short` class gets its package, an entry that doesn't parse
 * is skipped, and the first entry for a component wins.
 */
fun appfilterIndex(pairs: Sequence<Pair<String, String>>): Map<String, String> {
    val out = LinkedHashMap<String, String>()
    for ((component, drawable) in pairs) {
        val c = component.trim().removePrefix("ComponentInfo{").removeSuffix("}")
        val slash = c.indexOf('/')
        if (slash <= 0 || slash == c.lastIndex || drawable.isBlank()) continue
        val pkg = c.substring(0, slash)
        val cls = c.substring(slash + 1).let { if (it.startsWith(".")) pkg + it else it }
        out.putIfAbsent("$pkg/$cls", drawable)
    }
    return out
}

/** Where an icon comes from: the pack, the app's monochrome layer, or the app's own icon. */
enum class IconSource { PACK, THEMED, OWN }

/** The pack's drawable if it lists the app, else the monochrome layer if themed icons are on and the app has one, else its own. */
fun iconSource(inPack: Boolean, themed: Boolean, hasMonochrome: Boolean): IconSource = when {
    inPack -> IconSource.PACK
    themed && hasMonochrome -> IconSource.THEMED
    else -> IconSource.OWN
}

/**
 * A parsed pack: its resources and, per component, the drawable's name. A name becomes an id when
 * an icon is first drawn (on IO) and is kept: a big pack lists tens of thousands of components
 * (Arcticons about 49,000), and resolving them all at parse time took 18 s on the emulator.
 */
class PackIndex(val packageName: String, val version: Int, private val res: Resources, private val names: Map<String, String>) {
    private val ids = java.util.concurrent.ConcurrentHashMap<String, Int>()

    fun has(component: String): Boolean = component in names

    @Suppress("DiscouragedApi")
    fun drawable(component: String, densityDpi: Int): Drawable? {
        val name = names[component] ?: return null
        val id = ids.getOrPut(name) { res.getIdentifier(name, "drawable", packageName) }
        if (id == 0) return null
        return try {
            res.getDrawableForDensity(id, densityDpi, null)
        } catch (e: Resources.NotFoundException) {
            null
        }
    }
}

/**
 * The process's icon pack index, built on its own thread (not the search sources', which contacts
 * and shortcuts share) when the look names a pack, and again or dropped when that pack's package
 * changes. Until it is in, icons draw as the apps' own; nothing waits for it.
 */
class IconPackHolder(private val context: Context, private val scope: CoroutineScope) {
    private val thread = Dispatchers.IO.limitedParallelism(1)
    private val _index = MutableStateFlow<PackIndex?>(null)
    val index: StateFlow<PackIndex?> = _index
    @Volatile private var wanted: String? = null
    private var version = 0

    /** The look names [pack] (or none); builds or drops the index if that changed. */
    fun want(pack: String?) {
        if (pack == wanted) return
        wanted = pack
        rebuild()
    }

    /** A package changed: if it is the pack, read it again (or drop it, uninstalled). */
    fun changed(packages: List<String>) {
        if (wanted != null && wanted in packages) rebuild()
    }

    private fun rebuild() = scope.launch(thread) {
        val pack = wanted
        val built = pack?.let { build(it) }
        if (pack != wanted) return@launch
        version++
        _index.value = built?.let { PackIndex(pack, version, it.first, it.second) }
    }

    private fun build(pack: String): Pair<Resources, Map<String, String>>? {
        val start = SystemClock.elapsedRealtime()
        val res = try {
            context.packageManager.getResourcesForApplication(pack)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.i("LorIcons", "pack $pack isn't installed")
            return null
        }
        val names = try {
            appfilterIndex(read(res, pack))
        } catch (e: Exception) {
            Log.w("LorIcons", "pack $pack: appfilter didn't parse", e)
            return null
        }
        Log.i("LorIcons", "pack $pack: ${names.size} entries in ${SystemClock.elapsedRealtime() - start} ms")
        return res to names
    }

    /** `appfilter`'s item entries, from res/xml, else res/raw, else assets. */
    @Suppress("DiscouragedApi")
    private fun read(res: Resources, pack: String): Sequence<Pair<String, String>> {
        val xmlId = res.getIdentifier("appfilter", "xml", pack)
        val parser: XmlPullParser = if (xmlId != 0) res.getXml(xmlId) else {
            val rawId = res.getIdentifier("appfilter", "raw", pack)
            val input = if (rawId != 0) res.openRawResource(rawId) else res.assets.open("appfilter.xml")
            XmlPullParserFactory.newInstance().newPullParser().apply { setInput(input, "UTF-8") }
        }
        val out = ArrayList<Pair<String, String>>()
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG || parser.name != "item") continue
            val component = parser.getAttributeValue(null, "component") ?: continue
            val drawable = parser.getAttributeValue(null, "drawable") ?: continue
            out += component to drawable
        }
        return out.asSequence()
    }
}
