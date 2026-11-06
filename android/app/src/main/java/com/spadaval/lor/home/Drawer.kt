package com.spadaval.lor.home

import org.json.JSONObject

/*
 * The organised drawer (launcher polish, Increment 2): categories, the launcher's own launch
 * counts, and the sorter that places each app in a section. No Android here, so the JVM tests
 * reach all of it. The drawer itself is drawn in HomeScreen.kt; the file's `drawer` field is
 * parsed in Layout.kt.
 */

/** The categories a drawer section can take, in the drawer's default order. */
enum class Category(val id: String, val title: String) {
    WORK("work", "Work"),
    COMMUNICATION("communication", "Communication"),
    MONEY("money", "Money"),
    TRAVEL("travel", "Travel"),
    MEDIA("media", "Media"),
    GAMES("games", "Games"),
    TOOLS("tools", "Tools"),
    SYSTEM("system", "System"),
    OTHER("other", "Other"),
    ;

    companion object {
        fun of(id: String): Category? = entries.firstOrNull { it.id == id }
    }
}

/**
 * An app's category: the table of packages below first, then the category the app declares
 * (`ApplicationInfo.category`, Android 8), then System for a system app, else Other.
 */
fun categoryOf(packageName: String, declared: Int, system: Boolean): Category =
    PACKAGES[packageName]
        ?: DECLARED[declared]
        ?: if (system) Category.SYSTEM else Category.OTHER

/** `ApplicationInfo.CATEGORY_*`, by value: GAME 0, AUDIO 1, VIDEO 2, IMAGE 3, SOCIAL 4, NEWS 5, MAPS 6, PRODUCTIVITY 7, ACCESSIBILITY 8. */
private val DECLARED = mapOf(
    0 to Category.GAMES,
    1 to Category.MEDIA,
    2 to Category.MEDIA,
    3 to Category.MEDIA,
    4 to Category.COMMUNICATION,
    5 to Category.MEDIA,
    6 to Category.TRAVEL,
    7 to Category.WORK,
    8 to Category.SYSTEM,
)

/**
 * Common packages, which mostly declare no category. Started from the emulator's apps and common
 * ones; sizing it on the owner's own app list waits for the real phone.
 */
private val PACKAGES: Map<String, Category> = buildMap {
    fun put(category: Category, vararg packages: String) = packages.forEach { put(it, category) }
    put(
        Category.COMMUNICATION,
        "com.google.android.dialer", "com.android.dialer", "com.samsung.android.dialer", "org.fossify.phone",
        "com.google.android.apps.messaging", "com.android.mms", "com.samsung.android.messaging", "org.fossify.messages",
        "com.google.android.contacts", "com.android.contacts", "com.samsung.android.app.contacts", "org.fossify.contacts",
        "com.google.android.gm", "com.whatsapp", "org.telegram.messenger", "org.thoughtcrime.securesms",
        "com.discord", "com.facebook.orca", "com.google.android.apps.dynamite", "com.instagram.android",
        "com.snapchat.android", "com.linkedin.android", "com.twitter.android",
    )
    put(
        Category.WORK,
        "com.slack", "com.microsoft.teams", "us.zoom.videomeetings", "com.microsoft.office.outlook",
        "com.google.android.apps.docs", "com.google.android.apps.docs.editors.docs",
        "com.google.android.apps.docs.editors.sheets", "com.google.android.apps.docs.editors.slides",
        "com.google.android.calendar", "com.samsung.android.calendar", "org.fossify.calendar",
        "com.google.android.keep", "org.fossify.notes", "net.gsantner.markor", "notion.id", "com.todoist",
        "com.github.android", "com.spadaval.lor", "com.t3tools.t3code", "com.t3tools.t3code.dev",
    )
    put(
        Category.MONEY,
        "com.google.android.apps.walletnfcrel", "com.paypal.android.p2pmobile", "com.venmo", "com.squareup.cash",
        "com.zellepay.zelle", "com.chase.sig.android", "com.wf.wellsfargomobile", "com.infonow.bofa",
        "com.americanexpress.android.acctsvcs.us", "com.citi.citimobile", "com.discoverfinancial.mobile",
        "com.robinhood.android", "com.splitwise.SplitwiseMobile", "org.totschnig.myexpenses",
    )
    put(
        Category.TRAVEL,
        "com.google.android.apps.maps", "com.waze", "com.ubercab", "me.lyft.android", "com.airbnb.android",
        "com.booking", "com.expedia.bookings", "com.united.mobile.android", "com.delta.mobile.android",
        "com.aa.android", "com.southwestairlines.mobile", "com.citymapper.app.release", "app.organicmaps",
        "net.osmand", "com.tripadvisor.tripadvisor", "de.grobox.liberario",
    )
    put(
        Category.MEDIA,
        "com.google.android.youtube", "com.google.android.apps.youtube.music", "com.spotify.music",
        "com.netflix.mediaclient", "com.google.android.apps.photos", "com.sec.android.gallery3d",
        "org.fossify.gallery", "org.fossify.musicplayer", "org.fossify.paint", "org.schabi.newpipe",
        "com.github.libretube", "de.danoeh.antennapod", "com.google.android.apps.podcasts",
        "com.audible.application", "com.reddit.frontpage", "com.google.android.apps.magazines",
    )
    put(Category.GAMES, "com.serwylo.lexica", "org.billthefarmer.crossword")
    put(
        Category.TOOLS,
        "com.android.chrome", "org.mozilla.firefox", "org.mozilla.fennec_fdroid", "com.android.camera2",
        "com.google.android.GoogleCamera", "com.sec.android.app.camera", "com.google.android.deskclock",
        "com.sec.android.app.clockpackage", "org.fossify.clock", "com.google.android.calculator",
        "com.sec.android.app.popupcalculator", "com.google.android.documentsui", "com.google.android.apps.nbu.files",
        "org.fossify.filemanager", "org.fossify.voicerecorder", "com.google.android.googlequicksearchbox",
        "com.android.vending", "com.vrem.wifianalyzer", "com.kunzisoft.keepass.libre", "ch.rmy.android.http_shortcuts",
    )
    put(Category.SYSTEM, "com.android.settings", "com.google.android.apps.safetyhub", "com.android.stk")
}

/**
 * How often and how lately each app was opened from the launcher, keyed by [HomeApp.key]
 * (activity and profile). After Kvaesitso (GPL-3.0, `SearchableDao.touch`): a plain count, and a
 * weight that each launch moves toward 1 (`w += α(1 − w)`) while every other app's decays
 * (`w *= 1 − α`), so the weight favours what is used often and lately. α is Kvaesitso's default
 * (`WEIGHT_FACTOR_MEDIUM`), and weights under its floor are dropped to 0.
 */
class LaunchStats(val entries: Map<String, Entry> = emptyMap()) {
    data class Entry(val count: Int, val weight: Double, val last: Long)

    fun launched(key: String, at: Long): LaunchStats {
        val decayed = entries.mapValues { (k, e) ->
            if (k == key) e else e.copy(weight = (e.weight * (1 - ALPHA)).let { if (it < FLOOR) 0.0 else it })
        }
        val old = entries[key] ?: Entry(0, 0.0, 0)
        return LaunchStats(decayed + (key to Entry(old.count + 1, old.weight + ALPHA * (1 - old.weight), at)))
    }

    /** Keys, the last launched first. */
    fun recent(): List<String> = entries.entries.sortedByDescending { it.value.last }.map { it.key }

    /** Keys with weight, the heaviest first; ties go to the most recent. */
    fun frequent(): List<String> =
        entries.entries.filter { it.value.weight > 0 }
            .sortedWith(compareByDescending<Map.Entry<String, Entry>> { it.value.weight }.thenByDescending { it.value.last })
            .map { it.key }

    /** Keys by launch count, the most first; ties go to the most recent. */
    fun mostUsed(): List<String> =
        entries.entries.sortedWith(compareByDescending<Map.Entry<String, Entry>> { it.value.count }.thenByDescending { it.value.last })
            .map { it.key }

    fun toJson(): String = JSONObject()
        .put("version", 1)
        .put("apps", JSONObject().apply {
            entries.forEach { (k, e) -> put(k, JSONObject().put("count", e.count).put("weight", e.weight).put("last", e.last)) }
        })
        .toString()

    companion object {
        const val ALPHA = 0.03
        private const val FLOOR = 0.001

        /** Parses [text]; anything unreadable starts the counts again rather than failing home. */
        fun parse(text: String): LaunchStats = try {
            val apps = JSONObject(text).getJSONObject("apps")
            LaunchStats(apps.keys().asSequence().associateWith { k ->
                val e = apps.getJSONObject(k)
                Entry(e.getInt("count"), e.getDouble("weight"), e.getLong("last"))
            })
        } catch (e: Exception) {
            LaunchStats()
        }
    }
}

/** A drawer section as drawn: the file's, or one made for a category no file section takes. */
class SortedSection<T>(
    val name: String,
    val category: Category?,
    val apps: List<T>,
    val hidden: Boolean,
    val reason: String?,
    /** Whether the file holds this section (only those can be deleted). */
    val inFile: Boolean,
)

/** The sections the drawer has: the file's in order, then one for each category none takes. */
fun Drawer.allSections(): List<DrawerSection> {
    val taken = sections.mapNotNull { it.category }.toSet()
    val names = sections.map { it.name.lowercase() }.toSet()
    return sections + Category.entries.filter { it !in taken }.map { c ->
        DrawerSection(if (c.title.lowercase() in names) "${c.title} (2)" else c.title, c)
    }
}

/**
 * Places each of [apps] (main-profile apps, in the order to show them) in a section: the first
 * section whose `apps` names it, else the section of its category. Every app lands in exactly
 * one section, since every category has one.
 */
fun <T> Drawer.sort(apps: List<T>, id: (T) -> AppId, category: (T) -> Category): List<SortedSection<T>> {
    val all = allSections()
    val inFile = sections.size
    val placed = apps.groupBy { app ->
        val a = id(app)
        all.indexOfFirst { s -> s.apps.any { it.matches(a.packageName, a.className) } }
            .takeIf { it >= 0 }
            ?: all.indexOfFirst { it.category == category(app) }
    }
    return all.mapIndexed { i, s -> SortedSection(s.name, s.category, placed[i].orEmpty(), s.hidden, s.reason, i < inFile) }
}
