package com.spadaval.lor.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
import android.os.UserHandle
import android.provider.MediaStore
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.Collator

/** One launchable activity, its label read once (`getLabel` loads it from the package each call). */
class HomeApp(val label: String, val info: LauncherActivityInfo) {
    val packageName: String get() = info.componentName.packageName
    val user: UserHandle get() = info.user
    val key = "${info.componentName.flattenToString()}#${info.user}"
    val id = AppId(packageName, info.componentName.className)
    val mainProfile: Boolean get() = user == Process.myUserHandle()
    val category: Category by lazy {
        val a = info.applicationInfo
        categoryOf(packageName, a.category, a.flags and ApplicationInfo.FLAG_SYSTEM != 0)
    }
    /**
     * When the package was first installed, read once (each read asks the package manager). It
     * is the main profile's: `LauncherActivityInfo.getFirstInstallTime` ignores the profile.
     */
    val firstInstalled: Long = info.firstInstallTime
}

/** What home shows from the layout file: the parsed file, or why it can't be used. */
sealed interface LayoutState {
    data object Loading : LayoutState
    data class Ready(val file: LayoutFile) : LayoutState
    data class Broken(val message: String) : LayoutState
}

/** A package whose activities changed, so its icons are stale. */
data class PackageChange(val packages: List<String>, val user: UserHandle)

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
    private val collator = Collator.getInstance()
    private val byLabel = Comparator<HomeApp> { a, b -> collator.compare(a.label, b.label) }
    private val store = LayoutStore(File(context.filesDir, "layout.json"))
    private val statsFile = File(context.filesDir, "launch-stats.json")
    private var stamp = -1L

    /** The app list loaded in full at least once; until then no default layout is written. */
    private var loaded = false

    private val _apps = MutableStateFlow<List<HomeApp>>(emptyList())
    val apps: StateFlow<List<HomeApp>> = _apps
    private val _layout = MutableStateFlow<LayoutState>(LayoutState.Loading)
    val layout: StateFlow<LayoutState> = _layout
    private val _stats = MutableStateFlow(LaunchStats())
    val stats: StateFlow<LaunchStats> = _stats
    private val _changes = MutableSharedFlow<PackageChange>(extraBufferCapacity = 64)
    val changes: SharedFlow<PackageChange> = _changes

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = changed(listOf(packageName), user)
        override fun onPackageAdded(packageName: String, user: UserHandle) = changed(listOf(packageName), user)
        override fun onPackageChanged(packageName: String, user: UserHandle) = changed(listOf(packageName), user)
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
            changed(packageNames.toList(), user)
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
            changed(packageNames.toList(), user)
    }

    private val profiles = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            serially { loadApps() }
        }
    }

    init {
        launcherApps.registerCallback(callback)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_MANAGED_PROFILE_ADDED)
            addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
            addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED)
        }
        ContextCompat.registerReceiver(context, profiles, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        serially { loadApps() }
        serially { readLayout() }
        serially { if (statsFile.exists()) _stats.value = LaunchStats.parse(statsFile.readText()) }
    }

    /** Counts a launch of [app] from the launcher, for the drawer's Recent, Frequent and Most used. */
    fun launched(app: HomeApp) {
        val at = System.currentTimeMillis()
        serially {
            _stats.value = _stats.value.launched(app.key, at)
            writeAtomically(statsFile, _stats.value.toJson())
        }
    }

    /**
     * Home came to the front: reloads the app list if it never loaded, and rereads the layout file
     * if it changed since the last read or write (an agent or adb edited it).
     */
    fun resume() = serially {
        if (!loaded) loadApps()
        if (store.file.lastModified() != stamp) readLayout()
    }

    /**
     * Applies [change] to the file (see [LayoutStore.edit]) and shows the result. The new modified
     * time is recorded, so the next [resume] doesn't reread this write. Fails with the reason.
     */
    suspend fun edit(change: (LayoutFile) -> LayoutFile): Result<LayoutFile> = withContext(serial) {
        store.edit(change).map {
            stamp = it.stamp
            _layout.value = LayoutState.Ready(it.layout)
            Log.i(TAG, "wrote layout.json (an edit from the phone)")
            it.layout
        }.onFailure { Log.i(TAG, "layout edit refused: ${it.message}") }
    }

    private fun changed(packages: List<String>, user: UserHandle) = serially {
        _changes.tryEmit(PackageChange(packages, user))
        val fresh = packages.flatMap { pkg -> activities(pkg, user) }
        _apps.value = (_apps.value.filterNot { it.packageName in packages && it.user == user } + fresh).sortedWith(byLabel)
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
        // A layout that was waiting on the app list for its default can be made now.
        if (loaded && _layout.value is LayoutState.Broken && !store.file.exists()) readLayout()
    }

    private fun activities(pkg: String?, user: UserHandle): List<HomeApp> =
        launcherApps.getActivityList(pkg, user).map { HomeApp(it.label.toString(), it) }

    /** Reads the layout file; any failure shows as Broken rather than leaving home on Loading. */
    private fun readLayout() {
        _layout.value = try {
            val wrote = !store.file.exists()
            val read = store.read(if (loaded) ::defaultLayout else null)
            stamp = read.stamp
            Log.i(TAG, if (wrote) "wrote and read layout.json (the first run's default)" else "read layout.json")
            LayoutState.Ready(read.layout)
        } catch (e: LayoutError) {
            stamp = store.file.lastModified()
            LayoutState.Broken(e.message ?: "invalid")
        } catch (e: IOException) {
            LayoutState.Broken("can't read ${store.file.path}: ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "layout read failed", e)
            LayoutState.Broken("can't be read: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    /**
     * The first run's layout, from the apps on this phone. The dock is the default dialer, SMS
     * app, browser and camera, resolved from intents as Launcher3's default workspace does
     * (`res/xml/default_workspace_*.xml`, Apache-2.0). Home holds a few common apps, then a
     * Google folder with the rest of Google's.
     */
    private fun defaultLayout(): LayoutFile {
        val pm = context.packageManager
        val mine = _apps.value.filter { it.mainProfile }.map { it.packageName }.toSet()
        fun resolve(intent: Intent): String? =
            pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
                ?.takeIf { it != "android" }
                ?: pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).firstOrNull()?.activityInfo?.packageName
        val dock = listOfNotNull(
            resolve(Intent(Intent.ACTION_DIAL)),
            Telephony.Sms.getDefaultSmsPackage(context),
            resolve(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).addCategory(Intent.CATEGORY_BROWSABLE)),
            resolve(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)),
        ).distinct().filter { it in mine }
        val home = COMMON.filter { it in mine && it !in dock }
        val google = mine.filter { it.startsWith("com.google.") && it !in dock && it !in home }.sorted()
        val items = home.map(::AppRef) + listOfNotNull(google.takeIf { it.isNotEmpty() }?.let { Folder("Google", it.map(::AppRef)) })
        return LayoutFile("default", listOf(Layout("default", items.take(LayoutFile.MAX_HOME), dock.map(::AppRef))))
    }

    companion object {
        private const val TAG = "LorHome"
        private val COMMON = listOf(
            "com.whatsapp", "com.google.android.gm", "com.google.android.calendar", "com.samsung.android.calendar",
            "com.google.android.apps.maps", "com.google.android.apps.photos", "com.sec.android.gallery3d",
            "com.spotify.music", "com.google.android.youtube", "com.spadaval.lor", "com.android.settings",
        )
    }
}
