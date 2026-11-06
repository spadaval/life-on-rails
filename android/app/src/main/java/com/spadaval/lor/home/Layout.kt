package com.spadaval.lor.home

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/**
 * The home layout file (`layout.json` in the app's files directory), schema version 1. The schema
 * is written down in docs/missions/t3-launcher/plan.md, Increment 3.2, and the optional fields
 * added since in docs/missions/launcher-polish/plan.md, "Schema additions". Agents edit this file
 * (Increment 7), so parsing is strict and every error names where it is.
 *
 * ```
 * {
 *   "version": 1,
 *   "active": "default",
 *   "hidden": ["com.example.game"],                     // optional (launcher polish 1.1)
 *   "layouts": [
 *     { "name": "default",
 *       "expires": "2026-11-18T00:00:00Z",          // optional; read, not acted on until Increment 10
 *       "home": ["com.whatsapp", "com.android.chrome/com.google.android.apps.chrome.Main",
 *                { "folder": "Dev", "apps": ["com.github.android"],
 *                  "reason": "...", "pinned": true }],   // reason and pinned optional (Increment 9)
 *       "dock": ["com.google.android.dialer"] }
 *   ]
 * }
 * ```
 *
 * Unlike Launcher3's SQLite workspace (cells with x/y on numbered screens), positions are list
 * order: home fills a grid in reading order, so an agent edits a list rather than coordinates.
 */
data class LayoutFile(val active: String, val layouts: List<Layout>, val hidden: List<AppRef> = emptyList()) {
    val current: Layout get() = layouts.first { it.name == active }

    fun toJson(): String = JSONObject()
        .put("version", VERSION)
        .put("active", active)
        .apply { if (hidden.isNotEmpty()) put("hidden", JSONArray(hidden.map { it.toString() })) }
        .put("layouts", JSONArray(layouts.map { it.toJson() }))
        .toString(2)

    /** Whether [app] is hidden: a `hidden` ref matches it. Main-profile apps only; callers check the profile. */
    fun isHidden(app: AppId): Boolean = hidden.any { it.matches(app.packageName, app.className) }

    companion object {
        const val VERSION = 1
        const val MAX_HOME = 24
        const val MAX_DOCK = 5

        /** The largest file home reads; a real layout is a few KB. */
        const val MAX_BYTES = 256 * 1024L

        /** Parses and validates [text], or throws [LayoutError] naming the first problem. */
        fun parse(text: String): LayoutFile {
            if (text.length > MAX_BYTES) throw LayoutError("the file is over ${MAX_BYTES / 1024} KB")
            val tokener = JSONTokener(text)
            val root = try {
                tokener.nextValue()
            } catch (e: JSONException) {
                throw LayoutError("not JSON: ${e.message}")
            } catch (e: StackOverflowError) {
                // Android's org.json recurses without a limit, so deep nesting would crash home.
                throw LayoutError("not JSON: nested too deeply")
            }
            if (root !is JSONObject) throw LayoutError("the file must be a JSON object")
            // nextValue stops after the first value; anything but whitespace after it is an error.
            val trailing = try {
                tokener.nextClean() != 0.toChar() || tokener.more()
            } catch (e: JSONException) {
                true
            }
            if (trailing) throw LayoutError("not JSON: more after the closing }")
            root.onlyKeys("", "version", "active", "hidden", "layouts")
            val version = root.opt("version")
            if (version != VERSION) throw LayoutError("version: must be $VERSION, got ${version ?: "nothing"}")
            val layouts = root.array("", "layouts").mapIndexed { i, v -> parseLayout("layouts[$i]", v) }
            if (layouts.isEmpty()) throw LayoutError("layouts: must hold at least one layout")
            layouts.groupBy { it.name }.filterValues { it.size > 1 }.keys.firstOrNull()?.let {
                throw LayoutError("layouts: the name \"$it\" is used twice")
            }
            val active = root.string("", "active")
            if (layouts.none { it.name == active }) throw LayoutError("active: no layout is named \"$active\"")
            val hidden = if (root.has("hidden")) root.array("", "hidden").mapIndexed { i, v -> parseRef("hidden[$i]", v) } else emptyList()
            return LayoutFile(active, layouts, hidden)
        }

        private fun parseLayout(at: String, v: Any): Layout {
            if (v !is JSONObject) throw LayoutError("$at: must be an object")
            v.onlyKeys(at, "name", "expires", "home", "dock")
            val name = v.string(at, "name")
            val expires = if (v.has("expires")) {
                val s = v.string(at, "expires")
                try {
                    OffsetDateTime.parse(s).toInstant()
                } catch (e: DateTimeParseException) {
                    throw LayoutError("$at.expires: \"$s\" is not a date-time like 2026-11-18T00:00:00Z")
                }
            } else null
            val home = if (v.has("home")) v.array(at, "home").mapIndexed { i, item -> parseItem("$at.home[$i]", item) } else emptyList()
            if (home.size > MAX_HOME) throw LayoutError("$at.home: at most $MAX_HOME items, got ${home.size}")
            val dock = if (v.has("dock")) v.array(at, "dock").mapIndexed { i, item -> parseRef("$at.dock[$i]", item) } else emptyList()
            if (dock.size > MAX_DOCK) throw LayoutError("$at.dock: at most $MAX_DOCK apps, got ${dock.size}")
            return Layout(name, home, dock, expires)
        }

        private fun parseItem(at: String, v: Any): LayoutItem = when (v) {
            is String -> parseRef(at, v)
            is JSONObject -> {
                if (!v.has("folder")) throw LayoutError("$at: an object here must be a folder (\"folder\": name)")
                v.onlyKeys(at, "folder", "apps", "reason", "pinned")
                val apps = v.array(at, "apps").mapIndexed { i, a -> parseRef("$at.apps[$i]", a) }
                if (apps.isEmpty()) throw LayoutError("$at.apps: a folder needs at least one app")
                val reason = if (v.has("reason")) v.string(at, "reason") else null
                val pinned = if (v.has("pinned")) {
                    v.opt("pinned") as? Boolean ?: throw LayoutError("$at.pinned: must be true or false")
                } else false
                Folder(v.string(at, "folder"), apps, reason, pinned)
            }
            else -> throw LayoutError("$at: must be an app (a package name) or a folder object")
        }

        private fun parseRef(at: String, v: Any): AppRef =
            (v as? String)?.let(AppRef::parse) ?: throw LayoutError("$at: \"$v\" is not a package name or package/activity")

        private fun JSONObject.onlyKeys(at: String, vararg allowed: String) {
            keys().asSequence().firstOrNull { it !in allowed }?.let {
                throw LayoutError("${if (at.isEmpty()) "" else "$at: "}unknown field \"$it\" (allowed: ${allowed.joinToString()})")
            }
        }

        private fun JSONObject.string(at: String, key: String): String {
            val s = opt(key) as? String ?: throw LayoutError("${path(at, key)}: must be a string")
            if (s.isBlank()) throw LayoutError("${path(at, key)}: must not be empty")
            return s
        }

        private fun JSONObject.array(at: String, key: String): List<Any> {
            val a = opt(key) as? JSONArray ?: throw LayoutError("${path(at, key)}: must be a list")
            return (0 until a.length()).map { a.get(it) }
        }

        private fun path(at: String, key: String) = if (at.isEmpty()) key else "$at.$key"
    }
}

/** One named layout. [expires] is kept for temporary layouts (Increment 10); nothing acts on it yet. */
data class Layout(val name: String, val home: List<LayoutItem>, val dock: List<AppRef>, val expires: Instant? = null) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        if (expires != null) put("expires", expires.toString())
        put("home", JSONArray(home.map { it.toJsonValue() }))
        put("dock", JSONArray(dock.map { it.toString() }))
    }
}

sealed interface LayoutItem {
    fun toJsonValue(): Any
}

/** A folder. [reason] and [pinned] are for agent-sorted folders (Increment 9): a pinned one is Sumeet's. */
data class Folder(val name: String, val apps: List<AppRef>, val reason: String? = null, val pinned: Boolean = false) : LayoutItem {
    override fun toJsonValue(): Any = JSONObject().apply {
        put("folder", name)
        put("apps", JSONArray(apps.map { it.toString() }))
        if (reason != null) put("reason", reason)
        if (pinned) put("pinned", true)
    }
}

/**
 * An app: a package name, which means that package's first launchable activity, or
 * `package/activity` for one activity (`package/.Short` as in `ComponentName.unflattenFromString`).
 */
data class AppRef(val packageName: String, val className: String? = null) : LayoutItem {
    fun matches(packageName: String, className: String): Boolean =
        packageName == this.packageName && (this.className == null || this.className == className)

    override fun toString() = if (className == null) packageName else "$packageName/$className"
    override fun toJsonValue(): Any = toString()

    companion object {
        private val PACKAGE = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")
        private val CLASS = Regex("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*")

        fun parse(s: String): AppRef? {
            val pkg = s.substringBefore('/')
            if (!PACKAGE.matches(pkg)) return null
            if ('/' !in s) return AppRef(pkg)
            val raw = s.substringAfter('/')
            val cls = if (raw.startsWith('.')) pkg + raw else raw
            return if (CLASS.matches(cls)) AppRef(pkg, cls) else null
        }
    }
}

class LayoutError(message: String) : Exception(message)

/** One launchable activity of a main-profile app, as the edits below match it against refs. */
data class AppId(val packageName: String, val className: String)

/** Where [LayoutFile.current] holds an app: on home itself, in the dock, in a folder. */
data class Places(val home: Boolean, val dock: Boolean, val folder: String?)

/*
 * Phone edits to the layout file (launcher polish 1.1). Each is a pure function from the parsed
 * file to the edited one, throwing a [LayoutError] whose message says why it refuses (a limit, a
 * duplicate, nothing to do). A ref names an app when [AppRef.matches] does, as home resolves it.
 * Edits act on the active layout, except hide, which acts on every layout. A folder an edit
 * touches becomes `pinned` (it is Sumeet's now); a folder an edit empties is removed.
 */

fun LayoutFile.placesOf(app: AppId): Places {
    val layout = current
    return Places(
        home = layout.home.any { it is AppRef && it.names(app) },
        dock = layout.dock.any { it.names(app) },
        folder = layout.home.filterIsInstance<Folder>().firstOrNull { f -> f.apps.any { it.names(app) } }?.name,
    )
}

/** Adds [ref] (written for [app]) to the end of home. */
fun LayoutFile.addToHome(app: AppId, ref: AppRef): LayoutFile {
    val layout = current
    val places = placesOf(app)
    if (places.home) throw LayoutError("it's already on home")
    places.folder?.let { throw LayoutError("it's already on home, in $it") }
    if (layout.home.size >= LayoutFile.MAX_HOME) throw LayoutError("home is full (${LayoutFile.MAX_HOME} items)")
    return withCurrent(layout.copy(home = layout.home + ref))
}

/** Adds [ref] (written for [app]) to the end of the dock. */
fun LayoutFile.addToDock(app: AppId, ref: AppRef): LayoutFile {
    val layout = current
    if (placesOf(app).dock) throw LayoutError("it's already in the dock")
    if (layout.dock.size >= LayoutFile.MAX_DOCK) throw LayoutError("the dock is full (${LayoutFile.MAX_DOCK} apps)")
    return withCurrent(layout.copy(dock = layout.dock + ref))
}

fun LayoutFile.removeFromHome(app: AppId): LayoutFile {
    val layout = current
    if (!placesOf(app).home) throw LayoutError("it isn't on home")
    return withCurrent(layout.copy(home = layout.home.filterNot { it is AppRef && it.names(app) }))
}

fun LayoutFile.removeFromDock(app: AppId): LayoutFile {
    val layout = current
    if (!placesOf(app).dock) throw LayoutError("it isn't in the dock")
    return withCurrent(layout.copy(dock = layout.dock.filterNot { it.names(app) }))
}

/** Removes [app] from the first folder on home that holds it. */
fun LayoutFile.removeFromFolder(app: AppId): LayoutFile {
    val layout = current
    val name = placesOf(app).folder ?: throw LayoutError("it isn't in a folder")
    var done = false
    val home = layout.home.mapNotNull { item ->
        if (done || item !is Folder || item.name != name) return@mapNotNull item
        done = true
        item.without { it.names(app) }
    }
    return withCurrent(layout.copy(home = home))
}

/**
 * Hides every activity of [packageName]: adds it to `hidden` and removes, in every layout, each
 * ref to that package from home, the dock and folders.
 */
fun LayoutFile.hide(packageName: String): LayoutFile {
    if (hidden.any { it.packageName == packageName && it.className == null }) throw LayoutError("it's already hidden")
    val gone = { ref: AppRef -> ref.packageName == packageName }
    val layouts = layouts.map { layout ->
        layout.copy(
            home = layout.home.mapNotNull { item ->
                when (item) {
                    is AppRef -> item.takeUnless(gone)
                    is Folder -> if (item.apps.any(gone)) item.without(gone) else item
                }
            },
            dock = layout.dock.filterNot(gone),
        )
    }
    return copy(layouts = layouts, hidden = hidden + AppRef(packageName))
}

/** Takes [packageName] out of `hidden`, nothing else: what hiding removed stays removed. */
fun LayoutFile.unhide(packageName: String): LayoutFile {
    if (hidden.none { it.packageName == packageName }) throw LayoutError("it isn't hidden")
    return copy(hidden = hidden.filterNot { it.packageName == packageName })
}

private fun AppRef.names(app: AppId) = matches(app.packageName, app.className)

private fun LayoutFile.withCurrent(layout: Layout) = copy(layouts = layouts.map { if (it.name == active) layout else it })

/** This folder without the apps [gone] picks, pinned; null when that leaves it empty. */
private fun Folder.without(gone: (AppRef) -> Boolean): Folder? =
    apps.filterNot(gone).takeIf { it.isNotEmpty() }?.let { copy(apps = it, pinned = true) }

/** The layout file on disk. No Android here, so the JVM tests can reach it. */
class LayoutStore(val file: File) {
    /** A parsed file and the modified time it was read or written at. */
    data class Read(val layout: LayoutFile, val stamp: Long)

    /**
     * Reads and parses the file. When it doesn't exist, writes [default] first; with no default
     * (the app list hasn't loaded), throws instead, so no layout is made from a partial list.
     */
    fun read(default: (() -> LayoutFile)?): Read {
        if (!file.exists()) {
            val make = default ?: throw LayoutError("the app list didn't load, so there is no default layout yet")
            write(make())
        }
        val stamp = file.lastModified()
        if (file.length() > LayoutFile.MAX_BYTES) throw LayoutError("the file is over ${LayoutFile.MAX_BYTES / 1024} KB")
        return Read(LayoutFile.parse(file.readText()), stamp)
    }

    /**
     * Rereads the file, applies [change], and writes the result if it parses back to the same
     * thing; otherwise the file is left as it was. Fails with the [LayoutError] or I/O error.
     */
    fun edit(change: (LayoutFile) -> LayoutFile): Result<Read> = runCatching {
        if (!file.exists()) throw LayoutError("there is no layout file yet")
        val changed = change(read(null).layout)
        Read(changed, write(changed))
    }

    /**
     * Serialises [layout], checks it parses back to itself, writes a temporary file and renames it
     * over the file. Returns the new modified time.
     */
    fun write(layout: LayoutFile): Long {
        val text = layout.toJson() + "\n"
        val back = LayoutFile.parse(text)
        if (back != layout) throw LayoutError("the edited layout doesn't read back the same")
        val tmp = File(file.path + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) throw IOException("can't replace ${file.path}")
        return file.lastModified()
    }
}
