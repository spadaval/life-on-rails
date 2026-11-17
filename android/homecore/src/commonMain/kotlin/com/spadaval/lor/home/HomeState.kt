package com.spadaval.lor.home

/*
 * What home draws from: the app list, the layout's state, search's sources. The app fills these
 * from Android (HomeStore.kt); the lab from a fixture.
 */

/**
 * One launchable activity as home draws it, its label read once. The app's version
 * (`LauncherApp`) wraps Android's `LauncherActivityInfo`; the lab's is plain.
 */
open class HomeApp(
    val label: String,
    val packageName: String,
    className: String,
    /** In the work profile: as refs name it, any profile but the main one (a phone has at most one, lor-72c3). */
    work: Boolean,
    /** The activity and its profile, unique among the apps (`component#user` on the phone). */
    val key: String,
    /** Its profile, as `UserHandle.toString()` writes it on the phone. */
    val profile: String,
    /**
     * When the package was first installed. On the phone it is the main profile's:
     * `LauncherActivityInfo.getFirstInstallTime` ignores the profile.
     */
    val firstInstalled: Long,
) {
    val mainProfile: Boolean get() = !id.work
    val id = AppId(packageName, className, work)

    /** Whether the app's menu offers Uninstall: not a system app. */
    open val removable: Boolean get() = true

    /** The label prepared for search once (launcher polish 5.1), on the first search that needs it. */
    val searchName: Prepared by lazy { Match.prepare(label) }
}

/**
 * An app's shortcut as home draws it (its menu, search). The app's version wraps Android's
 * `ShortcutInfo`, which launching it needs.
 */
open class AppShortcut(
    val id: String,
    val packageName: String,
    /** The activity it belongs to, when it names one. */
    val activityClass: String?,
    /** Its profile, as `UserHandle.toString()` writes it on the phone. */
    val profile: String,
    val shortLabel: String?,
    val longLabel: String?,
    /** A cached chat, not one the app publishes now. */
    val isCached: Boolean = false,
    val isDeclaredInManifest: Boolean = false,
    /** The app's own order. */
    val rank: Int = 0,
    val lastChangedTimestamp: Long = 0,
)

/** A shortcut search can find (launcher polish 5.2): [conversation] for a messaging app's chat. */
class ShortcutHit(val info: AppShortcut, val conversation: Boolean) {
    val label: String get() = (info.shortLabel ?: info.longLabel) ?: info.id
}

/** A contact search can find (launcher polish 5.6): its [numbers] as stored, the first shown. */
class SearchContact(val id: Long, val lookupKey: String, val name: String, val numbers: List<String>)

/**
 * What search finds besides apps, prepared off the main thread and held in memory (launcher
 * polish 5.1): no keystroke asks Android for anything.
 */
data class SearchSources(
    val targets: SearchTargets = SearchTargets(null, null),
    val shortcuts: List<Found<Any>> = emptyList(),
    val settings: List<Found<Any>> = emptyList(),
    val contacts: List<Found<Any>> = emptyList(),
)

/** What home shows from the layout file: the parsed file, or why it can't be used. */
sealed interface LayoutState {
    data object Loading : LayoutState
    data class Ready(val file: LayoutFile) : LayoutState
    /**
     * The file can't be used; home shows [lastGood], the last file that parsed, if there is one
     * (launcher polish 3.1). [left] names the old dock apps it has no room for (lor-bdus): shown
     * without them, it is not offered as a restore.
     */
    data class Broken(val message: String, val lastGood: LayoutFile?, val left: List<AppRef> = emptyList()) : LayoutState
}

/** The layout home draws: the file, or while it is broken, the last good one. */
val LayoutState.shown: LayoutFile?
    get() = when (this) {
        is LayoutState.Ready -> file
        is LayoutState.Broken -> lastGood
        LayoutState.Loading -> null
    }

/** How the app list's last load went: not done yet, done, or back with no apps (tried again on resume). */
enum class AppsLoad { LOADING, LOADED, EMPTY }

/** What home's grid shows (launcher polish 3.2). */
enum class HomeBody {
    /** The date header alone: nothing to shape a skeleton with yet. */
    NOTHING,
    /** A still slot where each of the file's items will be. */
    SKELETON,
    /** The app list came back empty. */
    APPS_DIDNT_LOAD,
    /** The layout places nothing on home. */
    EMPTY,
    GRID,
}

/** What home's grid shows, from the app list's load, the layout home draws, and how many home items it placed. */
fun homeBody(apps: AppsLoad, layout: LayoutFile?, placed: Int): HomeBody = when {
    apps == AppsLoad.LOADING -> if (layout == null) HomeBody.NOTHING else HomeBody.SKELETON
    apps == AppsLoad.EMPTY -> HomeBody.APPS_DIDNT_LOAD
    layout == null -> HomeBody.NOTHING
    placed == 0 -> HomeBody.EMPTY
    else -> HomeBody.GRID
}
