package com.spadaval.lor.home

import org.json.JSONArray
import org.json.JSONObject

/*
 * The drawer's tabs (lor-iyc3) and the launcher's own launch counts. No Android here, so the JVM
 * tests reach all of it. The drawer is drawn in DrawerScreen.kt and its bar in TabBar.kt; the
 * file's `drawer` field is parsed in Layout.kt.
 */

/**
 * The icons a tab's button draws (TabBar.kt), by the name `drawer.tabs[].icon` gives, after the
 * launcher-use mocks. layout-v2.md lists them.
 */
enum class TabIcon(val id: String) {
    BOLT("bolt"), STAR("star"), BRIEF("brief"), CHAT("chat"), PLAY("play"), COMPASS("compass"), WALLET("wallet"), ALL("all");

    companion object {
        /** The icon named [id]; null for a name the phone doesn't draw, which draws the tab name's first letter. */
        fun of(id: String): TabIcon? = entries.firstOrNull { it.id == id }
    }
}

/**
 * The floating bar's sizes in dp (TabBar.kt), kept here so [tabWidth] is tested on the JVM: the
 * search button (square, as tall as the bar), the gap before it and the bar's own padding.
 */
object BarSize {
    const val SEARCH = 44f
    const val GAP = 8f
    /** The bar's padding, both sides together. */
    const val PADDING = 6f
    /** A tab button's width when there is room, as in the mocks. */
    const val TAB = 40f
}

/**
 * A tab button's width in dp when the bar and the search button have [room] dp: [BarSize.TAB], or
 * narrower so [tabs] buttons fit beside the search button, which always keeps its size.
 */
fun tabWidth(room: Float, tabs: Int): Float =
    minOf(BarSize.TAB, (room - BarSize.SEARCH - BarSize.GAP - BarSize.PADDING) / tabs.coerceAtLeast(1))

/** What a tab's button draws when its icon is unknown: its name's first letter or digit, upper case. */
fun tabLetter(name: String): String = name.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?"

/**
 * How often and how lately each app was opened from the launcher, keyed by [HomeApp.key]
 * (activity and profile). After Kvaesitso (GPL-3.0, `SearchableDao.touch`): a plain count, and a
 * weight that each launch moves toward 1 (`w += α(1 − w)`) while every other app's decays
 * (`w *= 1 − α`), so the weight favours what is used often and lately. α is Kvaesitso's default
 * (`WEIGHT_FACTOR_MEDIUM`), and weights under its floor are dropped to 0.
 */
class LaunchStats(
    val entries: Map<String, Entry> = emptyMap(),
    /** The last [SEARCHES] searches that opened something, newest first, one per text ignoring case (launcher polish 5.5). */
    val searches: List<String> = emptyList(),
) {
    data class Entry(val count: Int, val weight: Double, val last: Long)

    /** Records [query] as the newest recent search; a blank one changes nothing. */
    fun searched(query: String): LaunchStats {
        val q = query.trim()
        if (q.isEmpty()) return this
        return LaunchStats(entries, (listOf(q) + searches.filterNot { it.equals(q, ignoreCase = true) }).take(SEARCHES))
    }

    fun clearSearches() = LaunchStats(entries)

    fun launched(key: String, at: Long): LaunchStats {
        val decayed = entries.mapValues { (k, e) ->
            if (k == key) e else e.copy(weight = (e.weight * (1 - ALPHA)).let { if (it < FLOOR) 0.0 else it })
        }
        val old = entries[key] ?: Entry(0, 0.0, 0)
        return LaunchStats(decayed + (key to Entry(old.count + 1, old.weight + ALPHA * (1 - old.weight), at)), searches)
    }

    fun toJson(): String = JSONObject()
        .put("version", 1)
        .put("apps", JSONObject().apply {
            entries.forEach { (k, e) -> put(k, JSONObject().put("count", e.count).put("weight", e.weight).put("last", e.last)) }
        })
        .apply { if (searches.isNotEmpty()) put("searches", JSONArray(searches)) }
        .toString()

    companion object {
        const val ALPHA = 0.03
        const val SEARCHES = 5
        private const val FLOOR = 0.001

        /** Parses [text]; anything unreadable starts the counts again rather than failing home. */
        fun parse(text: String): LaunchStats = try {
            val root = JSONObject(text)
            val apps = root.getJSONObject("apps")
            val searches = root.optJSONArray("searches")?.let { a -> (0 until a.length()).map { a.getString(it) } }.orEmpty()
            LaunchStats(
                apps.keys().asSequence().associateWith { k ->
                    val e = apps.getJSONObject(k)
                    Entry(e.getInt("count"), e.getDouble("weight"), e.getLong("last"))
                },
                searches.take(SEARCHES),
            )
        } catch (e: Exception) {
            LaunchStats()
        }
    }
}

/** The [apps] (in label order) a list tab shows, in the order its refs give; a package ref gives its activities in label order. */
fun <T> DrawerTab.shown(apps: List<T>, id: (T) -> AppId): List<T> {
    val seen = HashSet<AppId>()
    return this.apps.flatMap { r -> apps.filter { r.matches(id(it)) && seen.add(id(it)) } }
}

/** The list tab whose apps name [app], if any. */
fun Drawer.tabOf(app: AppId): DrawerTab? = tabs.firstOrNull { t -> t.kind == TabKind.LIST && t.apps.any { it.matches(app) } }

/*
 * Hand edits to the drawer's tabs (lor-iyc3), pure like the layout edits in Layout.kt: each
 * returns the edited file or throws a [LayoutError] saying why it refuses. Quick dial stays first
 * and All last, so only list tabs move, are deleted or are added (before All).
 */

private fun LayoutFile.withDrawer(change: (Drawer) -> Drawer) = copy(drawer = change(drawer))

private fun Drawer.indexOf(name: String): Int =
    tabs.indexOfFirst { it.name.equals(name, ignoreCase = true) }.takeIf { it >= 0 } ?: throw LayoutError("there is no tab called \"$name\"")

private fun Drawer.checkName(name: String, except: Int = -1): String {
    val n = name.trim()
    if (n.isEmpty()) throw LayoutError("a tab needs a name")
    tabs.withIndex().firstOrNull { (i, t) -> i != except && t.name.equals(n, ignoreCase = true) }?.let {
        throw LayoutError("a tab is already called \"${it.value.name}\"")
    }
    return n
}

private fun Drawer.listTab(name: String): Int {
    val i = indexOf(name)
    if (tabs[i].kind != TabKind.LIST) throw LayoutError("${tabs[i].name} isn't a list of apps")
    return i
}

/**
 * Moves [app] (written as [ref]) into the list tab named [target], out of any other. A tab that
 * listed [app]'s package by name, when [app] is one of several activities, lists the package's
 * other [installed] activities instead (launcher polish 4.1), so they stay where they were.
 */
fun LayoutFile.moveToTab(app: AppId, ref: AppRef, target: String, installed: List<AppId> = emptyList()): LayoutFile = withDrawer { d ->
    val to = d.listTab(target)
    if (d.tabOf(app) === d.tabs[to]) throw LayoutError("it's already in ${d.tabs[to].name}")
    // Refs naming the app go; a package ref also takes every other ref of its package, so no two
    // refs ever name one activity (a ref to an activity an update removed included).
    val names = { r: AppRef -> r.matches(app) || (ref.className == null && r.work == app.work && r.packageName == app.packageName) }
    d.copy(tabs = d.tabs.mapIndexed { i, t ->
        when {
            i == to -> t.copy(apps = t.apps.filterNot(names) + ref)
            t.apps.any(names) -> {
                // Only an activity ref is moved while a package ref stays elsewhere: keep its siblings.
                val others = if (ref.className != null && t.apps.any { it.work == app.work && it.packageName == app.packageName && it.className == null }) {
                    installed.filter { it.work == app.work && it.packageName == app.packageName && it.className != app.className }.map { AppRef(it.packageName, it.className, it.work) }
                } else emptyList()
                t.copy(apps = t.apps.filterNot(names) + others)
            }
            else -> t
        }
    })
}

fun LayoutFile.renameTab(name: String, to: String): LayoutFile = withDrawer { d ->
    val i = d.indexOf(name)
    val n = d.checkName(to, except = i)
    d.copy(tabs = d.tabs.mapIndexed { j, t -> if (j == i) t.copy(name = n) else t })
}

fun LayoutFile.setTabIcon(name: String, icon: TabIcon): LayoutFile = withDrawer { d ->
    val i = d.indexOf(name)
    d.copy(tabs = d.tabs.mapIndexed { j, t -> if (j == i) t.copy(icon = icon.id) else t })
}

/** Moves the list tab [name] one place left ([by] -1) or right (+1), never past quick dial or All. */
fun LayoutFile.moveTab(name: String, by: Int): LayoutFile = withDrawer { d ->
    val i = d.listTab(name)
    val j = i + by
    if (j <= 0 || j >= d.tabs.lastIndex) throw LayoutError("it's already ${if (by < 0) "first" else "last"} of the lists")
    val list = d.tabs.toMutableList()
    list[i] = list[j].also { list[j] = list[i] }
    d.copy(tabs = list)
}

/** Removes the list tab [name]; its apps are still in All. */
fun LayoutFile.deleteTab(name: String): LayoutFile = withDrawer { d ->
    val i = d.listTab(name)
    d.copy(tabs = d.tabs.filterIndexed { j, _ -> j != i })
}

/** Adds an empty list tab before All. */
fun LayoutFile.newTab(name: String, icon: TabIcon = TabIcon.STAR): LayoutFile = withDrawer { d ->
    if (d.tabs.size >= Drawer.MAX_TABS) throw LayoutError("the drawer has ${Drawer.MAX_TABS} tabs, the most it takes")
    val n = d.checkName(name)
    d.copy(tabs = d.tabs.dropLast(1) + DrawerTab(n, icon.id) + d.tabs.last())
}
