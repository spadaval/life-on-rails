package com.spadaval.lor.home

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.io.FileOutputStream
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
 *   "drawer": { "view": "sections",                      // optional (launcher polish 2.1)
 *               "sections": [{ "name": "Work", "category": "work", "apps": ["com.slack"] }] },
 *   "layouts": [
 *     { "name": "default",
 *       "expires": "2026-11-18T00:00:00Z",          // optional; read, not acted on until Increment 10
 *       "home": ["com.whatsapp", "com.android.chrome/com.google.android.apps.chrome.Main",
 *                { "folder": "Dev", "apps": ["com.github.android"],
 *                  "reason": "...", "pinned": true,      // reason and pinned optional (Increment 9)
 *                  "cover": "com.github.android" }],     // optional (launcher polish 4.1)
 *       "dock": ["com.google.android.dialer"],
 *       "pages": [{ "items": ["org.example"] }] }       // optional (launcher polish 4.1): pages after home

 *   ]
 * }
 * ```
 *
 * Unlike Launcher3's SQLite workspace (cells with x/y on numbered screens), positions are list
 * order: home fills a grid in reading order, so an agent edits a list rather than coordinates.
 */
data class LayoutFile(
    val active: String,
    val layouts: List<Layout>,
    val hidden: List<AppRef> = emptyList(),
    val drawer: Drawer = Drawer(),
) {
    val current: Layout get() = layouts.first { it.name == active }

    fun toJson(): String = unescapeSlashes(
        JSONObject()
            .put("version", VERSION)
            .put("active", active)
            .apply { if (hidden.isNotEmpty()) put("hidden", JSONArray(hidden.map { it.toString() })) }
            .apply { if (drawer != Drawer()) put("drawer", drawer.toJson()) }
            .put("layouts", JSONArray(layouts.map { it.toJson() }))
            .toString(2),
    )

    /** Whether [app] is hidden: a `hidden` ref matches it. Main-profile apps only; callers check the profile. */
    fun isHidden(app: AppId): Boolean = hidden.any { it.matches(app.packageName, app.className) }

    companion object {
        const val VERSION = 1
        const val MAX_HOME = 24
        const val MAX_DOCK = 5
        /** Pages after home (launcher polish 4.1). */
        const val MAX_PAGES = 4

        /**
         * Android's org.json writes every `/` as `\/`, which is valid JSON but makes a
         * `package/activity` ref hard to read and grep in a file people and agents edit. This
         * undoes it where the backslash isn't itself escaped.
         */
        fun unescapeSlashes(json: String): String = ESCAPED_SLASH.replace(json) { it.groupValues[1] + "/" }

        private val ESCAPED_SLASH = Regex("""(?<!\\)((?:\\\\)*)\\/""")

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
            root.onlyKeys("", "version", "active", "hidden", "drawer", "layouts")
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
            hidden.groupBy { it }.filterValues { it.size > 1 }.keys.firstOrNull()?.let {
                throw LayoutError("hidden: \"$it\" is listed twice")
            }
            val drawer = if (root.has("drawer")) parseDrawer(root.opt("drawer")!!) else Drawer()
            return LayoutFile(active, layouts, hidden, drawer)
        }

        private fun parseDrawer(v: Any): Drawer {
            if (v !is JSONObject) throw LayoutError("drawer: must be an object")
            v.onlyKeys("drawer", "view", "sections")
            val view = if (v.has("view")) {
                val id = v.string("drawer", "view")
                DrawerView.entries.firstOrNull { it.id == id }
                    ?: throw LayoutError("drawer.view: must be one of ${DrawerView.entries.joinToString { it.id }}, got \"$id\"")
            } else null
            val sections = if (v.has("sections")) v.array("drawer", "sections").mapIndexed { i, s -> parseSection("drawer.sections[$i]", s) } else emptyList()
            sections.groupBy { it.name.lowercase() }.filterValues { it.size > 1 }.values.firstOrNull()?.let {
                throw LayoutError("drawer.sections: the name \"${it[0].name}\" is used twice")
            }
            sections.mapNotNull { it.category }.groupBy { it }.filterValues { it.size > 1 }.keys.firstOrNull()?.let {
                throw LayoutError("drawer.sections: the category \"${it.id}\" is taken twice")
            }
            // Two refs may not name one activity, in one section or two.
            val refs = sections.flatMap { s -> s.apps.map { s to it } }
            refs.forEachIndexed { i, (s, a) ->
                refs.drop(i + 1).firstOrNull { (_, b) -> a.overlaps(b) }?.let { (t, b) ->
                    throw LayoutError("drawer.sections: \"$b\" in \"${t.name}\" names an app \"$a\" in \"${s.name}\" already names")
                }
            }
            return Drawer(view, sections)
        }

        private fun parseSection(at: String, v: Any): DrawerSection {
            if (v !is JSONObject) throw LayoutError("$at: must be an object")
            v.onlyKeys(at, "name", "category", "apps", "hidden", "pinned", "reason")
            val category = if (v.has("category")) {
                val id = v.string(at, "category")
                Category.of(id) ?: throw LayoutError("$at.category: must be one of ${Category.entries.joinToString { it.id }}, got \"$id\"")
            } else null
            val apps = if (v.has("apps")) v.array(at, "apps").mapIndexed { i, a -> parseRef("$at.apps[$i]", a) } else emptyList()
            return DrawerSection(
                v.string(at, "name"), category, apps,
                hidden = v.bool(at, "hidden"), pinned = v.bool(at, "pinned"),
                reason = if (v.has("reason")) v.string(at, "reason") else null,
            )
        }

        private fun JSONObject.bool(at: String, key: String): Boolean =
            if (has(key)) opt(key) as? Boolean ?: throw LayoutError("${path(at, key)}: must be true or false") else false

        private fun parseLayout(at: String, v: Any): Layout {
            if (v !is JSONObject) throw LayoutError("$at: must be an object")
            v.onlyKeys(at, "name", "expires", "home", "dock", "pages")
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
            val pages = if (v.has("pages")) v.array(at, "pages").mapIndexed { i, p -> parsePage("$at.pages[$i]", p) } else emptyList()
            if (pages.size > MAX_PAGES) throw LayoutError("$at.pages: at most $MAX_PAGES pages after home, got ${pages.size}")
            return Layout(name, home, dock, expires, pages)
        }

        private fun parsePage(at: String, v: Any): Page {
            if (v !is JSONObject) throw LayoutError("$at: must be an object")
            v.onlyKeys(at, "items")
            val items = if (v.has("items")) v.array(at, "items").mapIndexed { i, item -> parseItem("$at.items[$i]", item) } else emptyList()
            if (items.size > MAX_HOME) throw LayoutError("$at.items: at most $MAX_HOME items, got ${items.size}")
            return Page(items)
        }

        private fun parseItem(at: String, v: Any): LayoutItem = when (v) {
            is String -> parseRef(at, v)
            is JSONObject -> {
                if (!v.has("folder")) throw LayoutError("$at: an object here must be a folder (\"folder\": name)")
                v.onlyKeys(at, "folder", "apps", "reason", "pinned", "cover")
                val apps = v.array(at, "apps").mapIndexed { i, a -> parseRef("$at.apps[$i]", a) }
                if (apps.isEmpty()) throw LayoutError("$at.apps: a folder needs at least one app")
                val reason = if (v.has("reason")) v.string(at, "reason") else null
                val pinned = if (v.has("pinned")) {
                    v.opt("pinned") as? Boolean ?: throw LayoutError("$at.pinned: must be true or false")
                } else false
                val cover = if (v.has("cover")) parseRef("$at.cover", v.opt("cover")!!).also { c ->
                    if (c !in apps) throw LayoutError("$at.cover: \"$c\" is not one of the folder's apps")
                } else null
                Folder(v.string(at, "folder"), apps, reason, pinned, cover)
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

/**
 * One named layout. [expires] is kept for temporary layouts (Increment 10); nothing acts on it yet.
 * [home] is home's first page and [pages] the pages after it (launcher polish 4.1).
 */
data class Layout(
    val name: String,
    val home: List<LayoutItem>,
    val dock: List<AppRef>,
    val expires: Instant? = null,
    val pages: List<Page> = emptyList(),
) {
    /** Every page's items: page 0 is [home], page n is `pages[n-1]`. */
    val allPages: List<List<LayoutItem>> get() = listOf(home) + pages.map { it.items }

    /** This layout with page [n]'s items replaced. */
    fun withPage(n: Int, items: List<LayoutItem>): Layout =
        if (n == 0) copy(home = items) else copy(pages = pages.mapIndexed { i, p -> if (i == n - 1) Page(items) else p })

    /** Each page's items changed by [change]. */
    fun mapPages(change: (List<LayoutItem>) -> List<LayoutItem>): Layout =
        copy(home = change(home), pages = pages.map { Page(change(it.items)) })

    /** Without the extra pages that hold nothing; home's first page stays, even empty. */
    fun stripEmptyPages(): Layout = copy(pages = pages.filter { it.items.isNotEmpty() })

    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        if (expires != null) put("expires", expires.toString())
        put("home", JSONArray(home.map { it.toJsonValue() }))
        put("dock", JSONArray(dock.map { it.toString() }))
        if (pages.isNotEmpty()) put("pages", JSONArray(pages.map { p -> JSONObject().put("items", JSONArray(p.items.map { it.toJsonValue() })) }))
    }
}

/** A page of home after the first (launcher polish 4.1): its items, written as `home` is. */
data class Page(val items: List<LayoutItem>)

sealed interface LayoutItem {
    fun toJsonValue(): Any
}

/**
 * A folder. [reason] and [pinned] are for agent-sorted folders (Increment 9): a pinned one is
 * Sumeet's. [cover], one of [apps], is drawn as the folder's tile (launcher polish 4.1).
 */
data class Folder(
    val name: String,
    val apps: List<AppRef>,
    val reason: String? = null,
    val pinned: Boolean = false,
    val cover: AppRef? = null,
) : LayoutItem {
    override fun toJsonValue(): Any = JSONObject().apply {
        put("folder", name)
        put("apps", JSONArray(apps.map { it.toString() }))
        if (reason != null) put("reason", reason)
        if (pinned) put("pinned", true)
        if (cover != null) put("cover", cover.toString())
    }
}

/**
 * An app: a package name, which means that package's first launchable activity, or
 * `package/activity` for one activity (`package/.Short` as in `ComponentName.unflattenFromString`).
 */
data class AppRef(val packageName: String, val className: String? = null) : LayoutItem {
    fun matches(packageName: String, className: String): Boolean =
        packageName == this.packageName && (this.className == null || this.className == className)

    /** Whether some activity is named by both refs: equal, or a package and one of its activities. */
    fun overlaps(other: AppRef): Boolean =
        packageName == other.packageName && (className == null || other.className == null || className == other.className)

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

/** How the drawer opens: on its sections, A–Z, or by launch count. */
enum class DrawerView(val id: String) { SECTIONS("sections"), AZ("az"), USED("used") }

/**
 * The drawer's part of the file (launcher polish 2.1). [view] is the last view chosen, null until
 * one is. [sections] are the sections the file holds, in order; see [allSections] and [sort].
 */
data class Drawer(val view: DrawerView? = null, val sections: List<DrawerSection> = emptyList()) {
    fun toJson(): JSONObject = JSONObject().apply {
        if (view != null) put("view", view.id)
        if (sections.isNotEmpty()) put("sections", JSONArray(sections.map { it.toJson() }))
    }
}

/**
 * A drawer section: its [name], the [category] whose apps it takes, the [apps] put in it by hand
 * (a package name means every activity), and whether it is [hidden] from the Sections view.
 * [pinned] marks one edited by hand; [reason] is a line an agent wrote (T3 Launcher Increment 9).
 */
data class DrawerSection(
    val name: String,
    val category: Category? = null,
    val apps: List<AppRef> = emptyList(),
    val hidden: Boolean = false,
    val pinned: Boolean = false,
    val reason: String? = null,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        if (category != null) put("category", category.id)
        if (apps.isNotEmpty()) put("apps", JSONArray(apps.map { it.toString() }))
        if (hidden) put("hidden", true)
        if (pinned) put("pinned", true)
        if (reason != null) put("reason", reason)
    }
}

/** One launchable activity of a main-profile app, as the edits below match it against refs. */
data class AppId(val packageName: String, val className: String)

/** The layout file on disk. No Android here, so the JVM tests can reach it. */
class LayoutStore(val file: File) {
    /** A parsed file and the modified time it was read or written at. */
    data class Read(val layout: LayoutFile, val stamp: Long)

    /**
     * The text of the last file that parsed (launcher polish 3.1), so a file that is broken when
     * the process starts still has a layout to show beside its error. A cache, not part of the schema.
     */
    val good = File(file.parentFile, "layout.good.json")

    /** Where [restore] sets a broken file aside, for whoever wrote it to read. */
    val broken = File(file.parentFile, "layout.json.broken")

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
        val text = file.readText()
        val layout = LayoutFile.parse(text)
        keepGood(text)
        return Read(layout, stamp)
    }

    /** The last file that parsed, or null when there is none or it doesn't parse either. */
    fun readGood(): LayoutFile? = try {
        if (good.exists()) LayoutFile.parse(good.readText()) else null
    } catch (e: LayoutError) {
        null
    } catch (e: IOException) {
        null
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
     * Puts a broken file right: renames it to [broken] (replacing an older one), then writes
     * [fallback] in its place. Returns the new modified time.
     */
    fun restore(fallback: LayoutFile): Long {
        if (file.exists() && !file.renameTo(broken)) throw IOException("can't set ${file.path} aside")
        return write(fallback)
    }

    /**
     * Serialises [layout], checks it parses back to itself, writes a temporary file and renames it
     * over the file. Returns the new modified time.
     */
    fun write(layout: LayoutFile): Long {
        val text = layout.toJson() + "\n"
        val back = LayoutFile.parse(text)
        if (back != layout) throw LayoutError("the edited layout doesn't read back the same")
        writeAtomically(file, text)
        keepGood(text)
        return file.lastModified()
    }

    /**
     * Copies [text] to [good] unless it already holds it, so most reads write nothing. The copy is
     * a cache: failing to write it (a full disk) must not fail the read or edit it follows.
     */
    private fun keepGood(text: String) {
        try {
            if (good.exists() && good.readText() == text) return
            writeAtomically(good, text)
        } catch (e: IOException) {
            // Kept as it was; the next read or write tries again.
        }
    }
}

/**
 * Writes [text] to a temporary file beside [file] and renames it over [file]. Synced before the
 * rename, so a crash or power loss leaves the old file or the new one, never a partly written one
 * (f2fs, Samsung's /data, can otherwise rename unwritten data).
 */
fun writeAtomically(file: File, text: String) {
    val tmp = File(file.path + ".tmp")
    FileOutputStream(tmp).use { out ->
        out.write(text.toByteArray())
        out.fd.sync()
    }
    if (!tmp.renameTo(file)) throw IOException("can't replace ${file.path}")
}
