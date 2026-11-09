package com.spadaval.lor.home

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Process
import android.os.SystemClock
import android.os.UserHandle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.Collator
import java.time.ZoneId

/** One launchable activity, its label read once (`getLabel` loads it from the package each call). */
class HomeApp(val label: String, val info: LauncherActivityInfo) {
    val packageName: String get() = info.componentName.packageName
    val user: UserHandle get() = info.user
    val key = "${info.componentName.flattenToString()}#${info.user}"
    val mainProfile: Boolean get() = user == Process.myUserHandle()
    /** As refs name it: any profile but the main one is the work profile (a phone has at most one, lor-72c3). */
    val id = AppId(packageName, info.componentName.className, work = !mainProfile)
    /**
     * When the package was first installed, read once (each read asks the package manager). It
     * is the main profile's: `LauncherActivityInfo.getFirstInstallTime` ignores the profile.
     */
    val firstInstalled: Long = info.firstInstallTime

    /** The label prepared for search once (launcher polish 5.1), on the first search that needs it. */
    val searchName: Prepared by lazy { Match.prepare(label) }
}

/** A shortcut search can find (launcher polish 5.2): [conversation] for a messaging app's chat. */
class ShortcutHit(val info: ShortcutInfo, val conversation: Boolean) {
    val label: String get() = (info.shortLabel ?: info.longLabel)?.toString() ?: info.id
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

/**
 * The app list and the layout file for the whole process (launcher polish, P1): created once by
 * [com.spadaval.lor.LorApp], read by home and Home settings, and the one writer of `layout.json`.
 * Every load and write runs on one serial dispatcher, so writes never interleave.
 *
 * The app list follows Kvaesitso (github.com/MM2-0/Kvaesitso, GPL-3.0,
 * `data/applications/AppRepository.kt`): `LauncherApps.getActivityList` for every profile, and a
 * `LauncherApps.Callback` that replaces only the changed package's activities, serialised (there
 * a mutex, here a one-thread dispatcher). Profile changes (a work profile added, removed, turned
 * on or off) reload the whole list, as Launcher3 does on its `ACTION_MANAGED_PROFILE_*` receiver.
 */
class HomeStore(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val serial = Dispatchers.IO.limitedParallelism(1)
    /** Search's sources are built here, so a slow query never holds up a layout edit on [serial]. */
    private val sourcesSerial = Dispatchers.IO.limitedParallelism(1)
    private val collator = Collator.getInstance()
    private val byLabel = Comparator<HomeApp> { a, b -> collator.compare(a.label, b.label) }
    private val store = LayoutStore(File(context.filesDir, "layout.json"))
    private val statsFile = File(context.filesDir, "launch-stats.json")
    private val opensFile = File(context.filesDir, "quick-dial.json")
    private var stamp = -1L

    /** The app list loaded in full at least once; until then no default layout is written. */
    private var loaded = false
    private val _appsLoad = MutableStateFlow(AppsLoad.LOADING)
    /** How the app list's last load went; home shows its skeleton until the first one ends. */
    val appsLoad: StateFlow<AppsLoad> = _appsLoad

    private val _apps = MutableStateFlow<List<HomeApp>>(emptyList())
    val apps: StateFlow<List<HomeApp>> = _apps
    private val _layout = MutableStateFlow<LayoutState>(LayoutState.Loading)
    val layout: StateFlow<LayoutState> = _layout
    private val _reads = MutableStateFlow(0)
    /** Counts reads of the file (not the phone's own writes), so home can close what a change from outside may have moved. */
    val reads: StateFlow<Int> = _reads
    private val _stats = MutableStateFlow(LaunchStats())
    val stats: StateFlow<LaunchStats> = _stats
    private val _opens = MutableStateFlow(AppOpens())
    /** Quick dial's open counts (lor-2imw), from usage events and work apps' launcher taps. */
    val opens: StateFlow<AppOpens> = _opens
    private val _search = MutableStateFlow(SearchSources())
    /** Search's sources besides the app list (launcher polish 5.1). */
    val search: StateFlow<SearchSources> = _search
    /** App icons for the process (lor-ozj2), drawn at the size each screen asks for. */
    val icons = Icons(context)
    /** The process's widget host (lor-r0f9); swept the first time `layout.json` parses. */
    val widgets = WidgetHost(context)

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = changed(listOf(packageName), user)
        override fun onPackageAdded(packageName: String, user: UserHandle) = changed(listOf(packageName), user)
        override fun onPackageChanged(packageName: String, user: UserHandle) = changed(listOf(packageName), user)
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
            changed(packageNames.toList(), user)
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
            changed(packageNames.toList(), user)
        override fun onShortcutsChanged(packageName: String, shortcuts: MutableList<ShortcutInfo>, user: UserHandle) =
            refreshShortcuts(packageName, user)
    }

    private val profiles = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            serially { loadApps() }
            refreshShortcuts()
        }
    }

    /** Set when the contacts change; the next [resume] reads them again (launcher polish 5.6). */
    @Volatile private var contactsStale = true
    /** Whether the last read of contacts had the permission. */
    @Volatile private var contactsAllowed = false
    /** The observer is registered; that needs the permission, so it waits for the first read with it. */
    private var observingContacts = false
    private val contactsObserver = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) {
            contactsStale = true
        }
    }

    /** The wallpaper's ink, read from when the store is made and followed (launcher polish 7.7). */
    val ink = WallpaperInk(context, scope)

    /** The icon pack the look names, parsed once for the process (launcher polish 6.5). */
    val packs = IconPackHolder(context, scope)

    init {
        launcherApps.registerCallback(callback)
        scope.launch { layout.collect { state -> state.shown?.let { packs.want(it.look.iconPack) } } }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_MANAGED_PROFILE_ADDED)
            addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
            addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED)
        }
        ContextCompat.registerReceiver(context, profiles, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        // The file first, so home's skeleton has its shape while the app list loads (launcher
        // polish 3.2); with no file, the default waits for the list (loadApps reads it then).
        serially { if (store.file.exists()) readLayout() }
        serially { loadApps() }
        serially { if (statsFile.exists()) _stats.value = LaunchStats.parse(statsFile.readText()) }
        serially { if (opensFile.exists()) _opens.value = AppOpens.parse(opensFile.readText()) }
    }

    /**
     * Counts a launch of [app] from the launcher, for search's weight; a work app's also for quick
     * dial, whose usage events the main profile can't read (a main app's open is a usage event).
     */
    fun launched(app: HomeApp) {
        val at = System.currentTimeMillis()
        serially {
            _stats.value = _stats.value.launched(app.key, at)
            writeAtomically(statsFile, _stats.value.toJson())
            if (!app.mainProfile) {
                // Rebased first for a clock set since the last read, so the open lands on the right basis.
                _opens.value = _opens.value.corrected(at, SystemClock.elapsedRealtime(), bootCount()).opened(opensKey(app.id), at, ZoneId.systemDefault())
                writeAtomically(opensFile, _opens.value.toJson())
            }
        }
    }

    /**
     * Reads the usage events since the last read into quick dial's counts (lor-2imw), when the
     * drawer opens: Android keeps them only a few days, so the counts live in the launcher's file.
     * A first read looks back a week. Without Usage access the system returns none.
     */
    fun readUsage() = serially {
        val now = System.currentTimeMillis()
        val clock = SystemClock.elapsedRealtime()
        val boot = bootCount()
        val old = _opens.value.corrected(now, clock, boot)
        val from = if (old.cursor > 0) old.cursor else now - USAGE_FIRST_MS
        val usm = context.getSystemService(UsageStatsManager::class.java)
        val events = mutableListOf<Resumed>()
        val it = usm.queryEvents(from, now)
        val e = UsageEvents.Event()
        while (it.hasNextEvent()) {
            it.getNextEvent(e)
            if (e.eventType == UsageEvents.Event.ACTIVITY_RESUMED && e.packageName != null) events += Resumed(e.packageName, e.timeStamp)
        }
        val next = old.withUsage(events, ignoredForOpens(), now, clock, boot, ZoneId.systemDefault())
        _opens.value = next
        writeAtomically(opensFile, next.toJson())
        Log.i(TAG, "quick dial read ${events.size} app resumes since ${if (old.cursor > 0) "the last read" else "a week ago"} in ${System.currentTimeMillis() - now} ms (${next.counts.size} apps counted)")
    }

    /** Which boot this is (`Settings.Global.BOOT_COUNT`), so quick dial compares `elapsedRealtime` only within one; 0 when unknown. */
    private fun bootCount(): Int = android.provider.Settings.Global.getInt(context.contentResolver, android.provider.Settings.Global.BOOT_COUNT, 0)

    /** LoR (this launcher), the default home if another, and the system UI: being in front of them isn't opening an app. */
    private fun ignoredForOpens(): Set<String> {
        val home = context.packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY)
        return setOfNotNull(context.packageName, "com.android.systemui", home?.activityInfo?.packageName)
    }

    /** Records [query] as a recent search (launcher polish 5.5): something was opened from it. */
    fun searched(query: String) = serially {
        val next = _stats.value.searched(query)
        if (next.searches == _stats.value.searches) return@serially
        _stats.value = next
        writeAtomically(statsFile, next.toJson())
    }

    /** Empties the recent searches. */
    fun clearSearches() = serially {
        _stats.value = _stats.value.clearSearches()
        writeAtomically(statsFile, _stats.value.toJson())
    }

    /**
     * Home came to the front: reloads the app list if it never loaded, and rereads the layout file
     * if it changed since the last read or write (an agent or adb edited it).
     */
    fun resume() {
        serially {
            if (!loaded) loadApps()
            if (store.file.lastModified() != stamp) readLayout()
        }
        // A new default browser or store fires no package callback.
        resolveTargets()
        // Becoming the default home gives the shortcut permission, with no callback.
        onSources { if (launcherApps.hasShortcutHostPermission() != shortcutsAllowed) refreshShortcutsNow(null, null) }
        // Contacts that changed while away, or a permission granted or taken back.
        onSources { if (contactsStale || contactsGranted() != contactsAllowed) readContacts() }
    }

    private fun contactsGranted() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    /**
     * Reads every contact's name and phone numbers into search's sources (launcher polish 5.6),
     * or empties them without the permission. Kvaesitso queries contacts on each search; here
     * they are held in memory, as the other sources are.
     */
    private fun readContacts() {
        // Cleared first, so a change during the read marks it stale again; a failed read is tried again on the next resume.
        contactsStale = false
        try {
            readContactsNow()
        } catch (e: Exception) {
            contactsStale = true
            throw e
        }
    }

    private fun readContactsNow() {
        val start = System.nanoTime()
        contactsAllowed = contactsGranted()
        if (!contactsAllowed) {
            if (_search.value.contacts.isNotEmpty()) _search.value = _search.value.copy(contacts = emptyList())
            Log.i(SEARCH_TAG, "contacts: no permission")
            return
        }
        val cr = context.contentResolver
        if (!observingContacts) {
            // Taking the permission back kills the process, so it is never unregistered.
            cr.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contactsObserver)
            observingContacts = true
        }
        val numbers = HashMap<Long, MutableList<String>>()
        cr.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.CONTACT_ID, ContactsContract.CommonDataKinds.Phone.NUMBER),
            null, null, ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY + " DESC",
        )?.use { c ->
            while (c.moveToNext()) {
                val n = c.getString(1) ?: continue
                numbers.getOrPut(c.getLong(0)) { mutableListOf() }.let { if (n !in it) it.add(n) }
            }
        }
        val found = ArrayList<Found<Any>>()
        cr.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.LOOKUP_KEY, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
            null, null, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
        )?.use { c ->
            while (c.moveToNext()) {
                val name = c.getString(2)?.takeIf { it.isNotBlank() } ?: continue
                val contact = SearchContact(c.getLong(0), c.getString(1) ?: "", name, numbers[c.getLong(0)].orEmpty())
                found.add(Found(Kind.CONTACTS, "c:${contact.id}", listOf(Match.prepare(name)), contact, contact.numbers.map(::digitsOf)))
            }
        }
        _search.value = _search.value.copy(contacts = found)
        Log.i(SEARCH_TAG, "read ${found.size} contacts in ${(System.nanoTime() - start) / 1_000_000} ms")
    }

    /** Whether the last full read of shortcuts had the permission (only the default home has it). */
    @Volatile private var shortcutsAllowed = false

    /** Reads the shortcuts of [pkg] in [user] again, or every shortcut when [pkg] is null (launcher polish 5.2). */
    private fun refreshShortcuts(pkg: String? = null, user: UserHandle? = null) = onSources { refreshShortcutsNow(pkg, user) }

    private fun refreshShortcutsNow(pkg: String?, user: UserHandle?) {
        val start = System.nanoTime()
        val allowed = launcherApps.hasShortcutHostPermission()
        if (pkg == null) shortcutsAllowed = allowed
        val users = if (user != null) listOf(user) else launcherApps.profiles
        val fresh = if (!allowed) emptyList() else users.flatMap { u ->
            val query = LauncherApps.ShortcutQuery().setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_CACHED,
            )
            if (pkg != null) query.setPackage(pkg)
            try {
                launcherApps.getShortcuts(query, u).orEmpty()
            } catch (e: RuntimeException) {
                // A locked profile (IllegalStateException), or no longer the default home (SecurityException).
                emptyList()
            }
        }.filter { it.isEnabled }.map { info ->
            val hit = ShortcutHit(info, isConversation(info.categories, info.isCached))
            val names = listOfNotNull(info.shortLabel, info.longLabel).map { Match.prepare(it.toString()) }
            Found<Any>(Kind.SHORTCUTS, "s:${info.`package`}/${info.id}#${info.userHandle}", names, hit)
        }
        val kept = if (pkg == null) emptyList() else _search.value.shortcuts.filterNot {
            val i = (it.value as ShortcutHit).info
            i.`package` == pkg && (user == null || i.userHandle == user)
        }
        _search.value = _search.value.copy(shortcuts = kept + fresh)
        Log.i(SEARCH_TAG, "read ${fresh.size} shortcuts${pkg?.let { " of $it" } ?: ""} in ${(System.nanoTime() - start) / 1_000_000} ms (${_search.value.shortcuts.size} in all)")
    }

    /**
     * Works out who takes a search elsewhere (launcher polish 5.1, R5), and which settings pages
     * this phone opens (5.3), logging only a change.
     */
    private fun resolveTargets() = onSources {
        val pm = context.packageManager
        val t = searchTargets(pm)
        if (t != _search.value.targets) {
            _search.value = _search.value.copy(targets = t)
            Log.i(SEARCH_TAG, "search targets: web ${t.web ?: "none"}, store ${t.store ?: "none"}")
        }
        val (kept, dropped) = SettingsPage.ALL.partition { it.lor || pm.resolveActivity(Intent(it.action), 0) != null }
        val settings = kept.map { SettingsPage.found<Any>(it, it) }
        if (settings.map { it.key } != _search.value.settings.map { it.key }) {
            _search.value = _search.value.copy(settings = settings)
            Log.i(SEARCH_TAG, "settings pages: ${kept.size} kept${if (dropped.isEmpty()) "" else ", dropped ${dropped.joinToString { it.title }}"}")
        }
    }

    /** Like [serially], on search's own thread. */
    private fun onSources(block: () -> Unit) {
        scope.launch(sourcesSerial) {
            try {
                block()
            } catch (e: Exception) {
                Log.w(SEARCH_TAG, "search sources update failed", e)
            }
        }
    }

    /** The last edit's copy for [undo] (launcher polish 4.2): kept in memory until the next edit or an undo. */
    private var lastUndo: LayoutStore.Undo? = null

    /** An edit's outcome: the file now, and whether it wrote something (which [undo] can take back). */
    data class Edited(val layout: LayoutFile, val undoable: Boolean)

    /**
     * Applies [change] to the file (see [LayoutStore.edit]) and shows the result. The new modified
     * time is recorded, so the next [resume] doesn't reread this write. An edit that wrote can be
     * taken back by [undo]. Fails with the reason.
     */
    suspend fun edit(change: (LayoutFile) -> LayoutFile): Result<Edited> = withContext(serial) {
        store.edit(change).map {
            if (it.undo == null) return@map Edited(it.layout, false)
            stamp = it.stamp
            _layout.value = LayoutState.Ready(it.layout)
            lastUndo = it.undo
            Log.i(TAG, "wrote layout.json (an edit from the phone)")
            Edited(it.layout, true)
        }.onFailure {
            Log.i(TAG, "layout edit refused: ${it.message}")
            // A file changed from outside since the last read (home rereads only on resume): read
            // it now, so trying again works on what is there (the open folder closes through reads).
            if (store.file.lastModified() != stamp) readLayout()
        }
    }

    /** Puts the file back as it was before the last edit ([LayoutStore.undo]). Fails with the reason. */
    suspend fun undo(): Result<LayoutFile> = withContext(serial) {
        runCatching {
            val undo = lastUndo ?: throw LayoutError("there is nothing to undo")
            lastUndo = null
            val read = store.undo(undo)
            stamp = read.stamp
            _layout.value = LayoutState.Ready(read.layout)
            Log.i(TAG, "undid the last edit")
            read.layout
        }.onFailure { Log.i(TAG, "undo refused: ${it.message}") }
    }

    /**
     * Puts a broken file right ([LayoutStore.restore]): the last good layout, else the default
     * once the app list has loaded. Fails with the reason.
     */
    suspend fun restore(): Result<LayoutFile> = withContext(serial) {
        runCatching {
            // A last good file shown without dock apps that didn't fit is not written (lor-bdus).
            store.readLastGood()?.left?.takeIf { it.isNotEmpty() }?.let { throw LayoutError(leftOutLine(it)) }
            val fallback = store.readGood()
                ?: if (loaded) defaultLayout() else throw LayoutError("the app list hasn't loaded, so there is no default layout yet")
            stamp = store.restore(fallback)
            _layout.value = LayoutState.Ready(fallback)
            Log.i(TAG, "restored layout.json (the broken file is layout.json.broken)")
            fallback
        }.onFailure { Log.i(TAG, "layout restore failed: ${it.message}") }
    }

    private fun changed(packages: List<String>, user: UserHandle) = serially {
        icons.evict(packages, user)
        packs.changed(packages)
        val fresh = packages.flatMap { pkg -> activities(pkg, user) }
        _apps.value = (_apps.value.filterNot { it.packageName in packages && it.user == user } + fresh).sortedWith(byLabel)
        resolveTargets()
        packages.forEach { refreshShortcuts(it, user) }
    }

    /** Serialised on [serial]; a failure is logged rather than taking home down. */
    private fun serially(block: () -> Unit) {
        scope.launch(serial) {
            try {
                block()
            } catch (e: Exception) {
                Log.w(TAG, "home update failed", e)
            }
        }
    }

    private fun loadApps() {
        val all = launcherApps.profiles.flatMap { activities(null, it) }.sortedWith(byLabel)
        _apps.value = all
        // An empty list means the load didn't really happen (a phone always has apps): try again on resume.
        loaded = all.isNotEmpty()
        _appsLoad.value = if (loaded) AppsLoad.LOADED else AppsLoad.EMPTY
        Log.i(TAG, "loaded the app list (${all.size} activities)")
        // A layout that was waiting on the app list for its default can be made now.
        if (loaded && !store.file.exists()) readLayout()
        resolveTargets()
        refreshShortcuts()
    }

    private fun activities(pkg: String?, user: UserHandle): List<HomeApp> =
        launcherApps.getActivityList(pkg, user).map { HomeApp(it.label.toString(), it) }

    private fun broken(message: String): LayoutState.Broken {
        val good = store.readLastGood()
        return LayoutState.Broken(message, good?.layout, good?.left.orEmpty())
    }

    /** Reads the layout file; any failure shows as Broken rather than leaving home on Loading. */
    private fun readLayout() {
        // No file and no app list yet: the default can't be made, so home is still loading.
        if (!loaded && !store.file.exists()) {
            _layout.value = LayoutState.Loading
            return
        }
        _reads.value++
        _layout.value = try {
            val wrote = !store.file.exists()
            val read = store.read(if (loaded) ::defaultLayout else null)
            stamp = read.stamp
            Log.i(TAG, if (wrote) "wrote and read layout.json (the first run's default)" else "read layout.json")
            // Ids no layout names go once per process, at the first read that parses (decision lor-9wlx).
            widgets.sweep(listOfNotNull(read.layout, store.readGood()))
            LayoutState.Ready(read.layout)
        } catch (e: LayoutError) {
            stamp = store.file.lastModified()
            broken(e.message ?: "invalid")
        } catch (e: IOException) {
            broken("can't read ${store.file.path}: ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "layout read failed", e)
            broken("can't be read: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    /**
     * The first run's layout, from the apps on this phone ([LayoutFile.firstRun]): the default
     * dialer, SMS app, browser and camera, resolved from intents as Launcher3's default workspace
     * does (`res/xml/default_workspace_*.xml`, Apache-2.0), then a few common apps.
     */
    private fun defaultLayout(): LayoutFile {
        val pm = context.packageManager
        val mine = _apps.value.filter { it.mainProfile }.map { it.packageName }.toSet()
        fun resolve(intent: Intent): String? =
            pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
                ?.takeIf { it != "android" }
                ?: pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).firstOrNull()?.activityInfo?.packageName
        val essentials = listOfNotNull(
            resolve(Intent(Intent.ACTION_DIAL)),
            Telephony.Sms.getDefaultSmsPackage(context),
            resolve(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).addCategory(Intent.CATEGORY_BROWSABLE)),
            resolve(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)),
        )
        return LayoutFile.firstRun(mine, essentials)
    }

    companion object {
        private const val TAG = "LorHome"
        const val SEARCH_TAG = "LorSearch"
        /** How far back quick dial's first read of usage events looks; Android keeps less. */
        private const val USAGE_FIRST_MS = 7L * 24 * 60 * 60 * 1000
    }
}
