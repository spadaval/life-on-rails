package com.spadaval.lor.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.Collator

/** One launchable activity, its label read once (`getLabel` loads it from the package each call). */
class HomeApp(val label: String, val info: LauncherActivityInfo) {
    val packageName: String get() = info.componentName.packageName
    val user: UserHandle get() = info.user
    val key = "${info.componentName.flattenToString()}#${info.user}"
}

/** What home shows from the layout file: the parsed file, or why it can't be used. */
sealed interface LayoutState {
    data object Loading : LayoutState
    data class Ready(val file: LayoutFile) : LayoutState
    data class Broken(val message: String) : LayoutState
}

/**
 * The app list and the layout file, loaded and updated off the main thread.
 *
 * The app list follows Kvaesitso (github.com/MM2-0/Kvaesitso, GPL-3.0,
 * `data/applications/AppRepository.kt`): `LauncherApps.getActivityList` for every profile, and a
 * `LauncherApps.Callback` that replaces only the changed package's activities, serialised (there
 * a mutex, here a one-thread dispatcher). Profile changes (a work profile added, removed, turned
 * on or off) reload the whole list, as Launcher3 does on its `ACTION_MANAGED_PROFILE_*` receiver.
 */
class HomeModel(private val context: Context, private val scope: CoroutineScope, val icons: Icons) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val serial = Dispatchers.IO.limitedParallelism(1)
    private val collator = Collator.getInstance()
    private val byLabel = Comparator<HomeApp> { a, b -> collator.compare(a.label, b.label) }
    private val file = File(context.filesDir, "layout.json")
    private var fileStamp = -1L

    private val _apps = MutableStateFlow<List<HomeApp>>(emptyList())
    val apps: StateFlow<List<HomeApp>> = _apps
    private val _layout = MutableStateFlow<LayoutState>(LayoutState.Loading)
    val layout: StateFlow<LayoutState> = _layout

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
            serially { _apps.value = loadAll() }
        }
    }

    fun start() {
        launcherApps.registerCallback(callback)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_MANAGED_PROFILE_ADDED)
            addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
            addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED)
        }
        ContextCompat.registerReceiver(context, profiles, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        serially { _apps.value = loadAll() }
        serially { readLayout() }
    }

    fun stop() {
        launcherApps.unregisterCallback(callback)
        context.unregisterReceiver(profiles)
    }

    /** Re-reads the layout file if it changed since the last read (an agent or adb edited it). */
    fun refreshLayout() = serially { if (file.lastModified() != fileStamp) readLayout() }

    private fun changed(packages: List<String>, user: UserHandle) = serially {
        icons.evict(packages, user)
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

    private fun loadAll(): List<HomeApp> =
        launcherApps.profiles.flatMap { activities(null, it) }.sortedWith(byLabel)

    private fun activities(pkg: String?, user: UserHandle): List<HomeApp> =
        launcherApps.getActivityList(pkg, user).map { HomeApp(it.label.toString(), it) }

    /**
     * Reads the layout file; any failure shows as Broken rather than leaving home on Loading.
     * Runs in its own [serially] step after the app list, which the first run's default reads.
     */
    private fun readLayout() {
        _layout.value = try {
            if (!file.exists()) {
                // A default built from no apps would be written for good; wait for the next refresh instead.
                if (_apps.value.isEmpty()) throw LayoutError("the app list didn't load, so there is no default layout yet")
                write(defaultLayout())
            }
            fileStamp = file.lastModified()
            if (file.length() > LayoutFile.MAX_BYTES) throw LayoutError("the file is over ${LayoutFile.MAX_BYTES / 1024} KB")
            LayoutState.Ready(LayoutFile.parse(file.readText()))
        } catch (e: LayoutError) {
            LayoutState.Broken(e.message ?: "invalid")
        } catch (e: IOException) {
            LayoutState.Broken("can't read ${file.path}: ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "layout read failed", e)
            LayoutState.Broken("can't be read: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    private fun write(layout: LayoutFile) {
        val tmp = File(file.path + ".tmp")
        tmp.writeText(layout.toJson() + "\n")
        tmp.renameTo(file)
    }

    /**
     * The first run's layout, from the apps on this phone. The dock is the default dialer, SMS
     * app, browser and camera, resolved from intents as Launcher3's default workspace does
     * (`res/xml/default_workspace_*.xml`, Apache-2.0). Home holds a few common apps, then a
     * Google folder with the rest of Google's.
     */
    private fun defaultLayout(): LayoutFile {
        val pm = context.packageManager
        val mine = _apps.value.filter { it.user == Process.myUserHandle() }.map { it.packageName }.toSet()
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
