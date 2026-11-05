package com.spadaval.lor.home

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/**
 * The home layout file (`layout.json` in the app's files directory), schema version 1. The schema
 * is written down in docs/missions/t3-launcher/plan.md, Increment 3.2. Agents edit this file
 * (Increment 7), so parsing is strict and every error names where it is.
 *
 * ```
 * {
 *   "version": 1,
 *   "active": "default",
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
data class LayoutFile(val active: String, val layouts: List<Layout>) {
    val current: Layout get() = layouts.first { it.name == active }

    fun toJson(): String = JSONObject()
        .put("version", VERSION)
        .put("active", active)
        .put("layouts", JSONArray(layouts.map { it.toJson() }))
        .toString(2)

    companion object {
        const val VERSION = 1
        const val MAX_HOME = 24
        const val MAX_DOCK = 5

        /** Parses and validates [text], or throws [LayoutError] naming the first problem. */
        fun parse(text: String): LayoutFile {
            val root = try {
                JSONTokener(text).nextValue()
            } catch (e: JSONException) {
                throw LayoutError("not JSON: ${e.message}")
            }
            if (root !is JSONObject) throw LayoutError("the file must be a JSON object")
            root.onlyKeys("", "version", "active", "layouts")
            val version = root.opt("version")
            if (version != VERSION) throw LayoutError("version: must be $VERSION, got ${version ?: "nothing"}")
            val layouts = root.array("", "layouts").mapIndexed { i, v -> parseLayout("layouts[$i]", v) }
            if (layouts.isEmpty()) throw LayoutError("layouts: must hold at least one layout")
            layouts.groupBy { it.name }.filterValues { it.size > 1 }.keys.firstOrNull()?.let {
                throw LayoutError("layouts: the name \"$it\" is used twice")
            }
            val active = root.string("", "active")
            if (layouts.none { it.name == active }) throw LayoutError("active: no layout is named \"$active\"")
            return LayoutFile(active, layouts)
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
