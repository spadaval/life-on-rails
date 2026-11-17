package com.spadaval.lor.home

import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.ShortcutInfo
import android.os.Process
import android.os.UserHandle

/** A [HomeApp] on the phone: one launchable activity, with Android's record of it. */
class LauncherApp(label: String, val info: LauncherActivityInfo) : HomeApp(
    label,
    info.componentName.packageName,
    info.componentName.className,
    work = info.user != Process.myUserHandle(),
    key = "${info.componentName.flattenToString()}#${info.user}",
    profile = info.user.toString(),
    firstInstalled = info.firstInstallTime,
) {
    override val removable: Boolean get() = (info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
}

/** The app's activity record; every [HomeApp] on the phone is a [LauncherApp]. */
val HomeApp.info: LauncherActivityInfo get() = (this as LauncherApp).info

val HomeApp.user: UserHandle get() = info.user

/** An [AppShortcut] on the phone, with Android's record of it. */
class LauncherShortcut(val shortcut: ShortcutInfo) : AppShortcut(
    shortcut.id,
    shortcut.`package`,
    shortcut.activity?.className,
    shortcut.userHandle.toString(),
    shortcut.shortLabel?.toString(),
    shortcut.longLabel?.toString(),
    isCached = shortcut.isCached,
    isDeclaredInManifest = shortcut.isDeclaredInManifest,
    rank = shortcut.rank,
    lastChangedTimestamp = shortcut.lastChangedTimestamp,
)

/** The shortcut's Android record; every [AppShortcut] on the phone is a [LauncherShortcut]. */
val AppShortcut.android: ShortcutInfo get() = (this as LauncherShortcut).shortcut
