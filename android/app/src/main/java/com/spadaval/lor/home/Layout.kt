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
 * The home layout file (`layout.json` in the app's files directory), schema version 2: each layout
 * is a sparse grid of panels. The schema is written down in
 * docs/missions/launcher-panels/layout-v2.md. A version 1 file (T3 Launcher 3.2 and Launcher
 * polish's "Schema additions") is read by converting it to the default grid ([Layout.grid]); the
 * phone writes version 2. Agents edit this file, so parsing is strict and every error names where
 * it is.
 *
 * ```
 * {
 *   "version": 2,
 *   "active": "default",
 *   "hidden": [...], "drawer": {...}, "look": {...},       // optional, as in version 1
 *   "layouts": [
 *     { "name": "default",
 *       "expires": "2026-11-18T00:00:00Z",                 // optional; read, not acted on yet
 *       "start": { "narrow": [1, 0], "wide": [1, 0] },     // wide optional, defaults to narrow
 *       "dock": ["com.google.android.dialer"],             // the one dock every `dock` block draws
 *       "panels": [
 *         { "at": [0, 0], "type": "t3" },                 // "wide": "full" or "half"; absent, the type's
 *         { "at": [1, 0], "type": "stack", "blocks": [     // default (stack half, t3 full, drawer half)
 *             { "type": "clock" },
 *             { "type": "apps", "items": ["com.whatsapp", { "folder": "Dev", "apps": ["com.github.android"] }] },
 *             { "type": "dock" }] },
 *         { "at": [1, 1], "type": "drawer" }] }
 *   ]
 * }
 * ```
 *
 * Within an `apps` block, positions are list order, as in version 1: the block fills its grid in
 * reading order, so an agent edits a list rather than coordinates.
 */
data class LayoutFile(
    val active: String,
    val layouts: List<Layout>,
    val hidden: List<AppRef> = emptyList(),
    val drawer: Drawer = Drawer(),
    val look: HomeLook = HomeLook(),
) {
    val current: Layout get() = layouts.first { it.name == active }

    fun toJson(): String = unescapeSlashes(
        JSONObject()
            .put("version", VERSION)
            .put("active", active)
            .apply { if (hidden.isNotEmpty()) put("hidden", JSONArray(hidden.map { it.toString() })) }
            .apply { if (drawer != Drawer()) put("drawer", drawer.toJson()) }
            .apply { look.toJson().takeIf { it.length() > 0 }?.let { put("look", it) } }
            .put("layouts", JSONArray(layouts.map { it.toJson() }))
            .toString(2),
    )

    /** Whether [app] is hidden: a `hidden` ref matches it. Main-profile apps only; callers check the profile. */
    fun isHidden(app: AppId): Boolean = hidden.any { it.matches(app.packageName, app.className) }

    companion object {
        const val VERSION = 2
        /** Items in an `apps` block (version 1: on a page). */
        const val MAX_HOME = 24
        const val MAX_DOCK = 5
        /** Stack panels with an `apps` block in one row: home and 4 more, as version 1's pages. */
        const val MAX_APPS_PANELS = 5
        /** Version 1's pages after home (launcher polish 4.1). */
        private const val V1_MAX_PAGES = 4

        /**
         * Android's org.json writes every `/` as `\/`, which is valid JSON but makes a
         * `package/activity` ref hard to read and grep in a file people and agents edit. This
         * undoes it where the backslash isn't itself escaped.
         */
        fun unescapeSlashes(json: String): String = ESCAPED_SLASH.replace(json) { it.groupValues[1] + "/" }

        private val ESCAPED_SLASH = Regex("""(?<!\\)((?:\\\\)*)\\/""")

        /** The largest file home reads; a real layout is a few KB. */
        const val MAX_BYTES = 256 * 1024L

        /** Parses and validates [text], or throws [LayoutError] naming the first problem. A version 1 file is converted. */
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
            root.onlyKeys("", "version", "active", "hidden", "drawer", "look", "layouts")
            val version = root.opt("version")
            val parseLayout: (String, Any) -> Layout = when (version) {
                VERSION -> ::parseLayout
                1 -> ::parseLayoutV1
                else -> throw LayoutError("version: must be $VERSION (or 1, which is converted), got ${version ?: "nothing"}")
            }
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
            val look = if (root.has("look")) HomeLook.parse(root.opt("look")!!) else HomeLook()
            return LayoutFile(active, layouts, hidden, drawer, look)
        }

        private fun parseDrawer(v: Any): Drawer {
            if (v !is JSONObject) throw LayoutError("drawer: must be an object")
            v.onlyKeys("drawer", "view", "sections", "keyboard")
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
            return Drawer(view, sections, keyboard = v.bool("drawer", "keyboard"))
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

        private fun parseExpires(at: String, v: JSONObject): Instant? = if (v.has("expires")) {
            val s = v.string(at, "expires")
            try {
                OffsetDateTime.parse(s).toInstant()
            } catch (e: DateTimeParseException) {
                throw LayoutError("$at.expires: \"$s\" is not a date-time like 2026-11-18T00:00:00Z")
            }
        } else null

        private fun parseDock(at: String, v: JSONObject): List<AppRef> {
            val dock = if (v.has("dock")) v.array(at, "dock").mapIndexed { i, item -> parseRef("$at.dock[$i]", item) } else emptyList()
            if (dock.size > MAX_DOCK) throw LayoutError("$at.dock: at most $MAX_DOCK apps, got ${dock.size}")
            return dock
        }

        private fun parseItems(at: String, v: JSONObject, key: String): List<LayoutItem> {
            val items = if (v.has(key)) v.array(at, key).mapIndexed { i, item -> parseItem("$at.$key[$i]", item) } else emptyList()
            if (items.size > MAX_HOME) throw LayoutError("$at.$key: at most $MAX_HOME items, got ${items.size}")
            return items
        }

        /** A version 1 layout (`home`, `pages`, `dock`), converted to the default grid. Errors name version 1's places. */
        private fun parseLayoutV1(at: String, v: Any): Layout {
            if (v !is JSONObject) throw LayoutError("$at: must be an object")
            v.onlyKeys(at, "name", "expires", "home", "dock", "pages")
            val name = v.string(at, "name")
            val expires = parseExpires(at, v)
            val home = parseItems(at, v, "home")
            val dock = parseDock(at, v)
            val pages = if (v.has("pages")) v.array(at, "pages").mapIndexed { i, p -> parsePageV1("$at.pages[$i]", p) } else emptyList()
            if (pages.size > V1_MAX_PAGES) throw LayoutError("$at.pages: at most $V1_MAX_PAGES pages after home, got ${pages.size}")
            return Layout.grid(name, home, dock, pages, expires)
        }

        private fun parsePageV1(at: String, v: Any): List<LayoutItem> {
            if (v !is JSONObject) throw LayoutError("$at: must be an object")
            v.onlyKeys(at, "items")
            return parseItems(at, v, "items")
        }

        private fun parseLayout(at: String, v: Any): Layout {
            if (v !is JSONObject) throw LayoutError("$at: must be an object")
            v.onlyKeys(at, "name", "expires", "start", "dock", "panels")
            val name = v.string(at, "name")
            val expires = parseExpires(at, v)
            val dock = parseDock(at, v)
            val panels = v.array(at, "panels").mapIndexed { i, p -> parsePanel("$at.panels[$i]", p) }
            panels.forEachIndexed { i, p ->
                val first = panels.indexOfFirst { it.at == p.at }
                if (first != i) throw LayoutError("$at.panels[$i].at: ${p.at} is taken by panels[$first]")
            }
            panels.filter { it.hasApps }.groupBy { it.at.row }.entries.firstOrNull { it.value.size > MAX_APPS_PANELS }?.let { (row, ps) ->
                throw LayoutError("$at.panels: row $row holds ${ps.size} stacks with an apps block, at most $MAX_APPS_PANELS")
            }
            if (!v.has("start")) throw LayoutError("$at.start: must be an object")
            val start = parseStart("$at.start", v.opt("start")!!, panels)
            return Layout(name, panels, dock, start, expires)
        }

        private fun parseStart(at: String, v: Any, panels: List<Panel>): Start {
            if (v !is JSONObject) throw LayoutError("$at: must be an object")
            v.onlyKeys(at, "narrow", "wide")
            fun cell(key: String): Cell {
                val c = parseCell("$at.$key", v.opt(key))
                if (panels.none { it.at == c }) throw LayoutError("$at.$key: no panel is at $c")
                return c
            }
            return Start(cell("narrow"), if (v.has("wide")) cell("wide") else null)
        }

        private fun parseCell(at: String, v: Any?): Cell {
            val a = v as? JSONArray
            val n = (0 until (a?.length() ?: 0)).map { a!!.get(it) }
            if (n.size != 2 || n.any { it !is Int || it < 0 }) throw LayoutError("$at: must be [column, row], two whole numbers from 0")
            return Cell(n[0] as Int, n[1] as Int)
        }

        private fun parsePanel(at: String, v: Any): Panel {
            if (v !is JSONObject) throw LayoutError("$at: must be an object")
            val type = v.string(at, "type")
            val cell = parseCell("$at.at", v.opt("at"))
            val wide = if (v.has("wide")) parseWide("$at.wide", v.opt("wide")) else null
            return when (type) {
                "stack" -> {
                    v.onlyKeys(at, "at", "type", "wide", "blocks")
                    val blocks = v.array(at, "blocks").mapIndexed { i, b -> parseBlock("$at.blocks[$i]", b) }
                    if (blocks.isEmpty()) throw LayoutError("$at.blocks: a stack needs at least one block")
                    blocks.forEachIndexed { i, b ->
                        if (blocks.indexOfFirst { it.type == b.type } != i) throw LayoutError("$at.blocks[$i]: a stack holds one \"${b.type}\" block")
                    }
                    Panel.Stack(cell, blocks).let { it.copy(wide = wide ?: it.wide) }
                }
                "t3" -> { v.onlyKeys(at, "at", "type", "wide"); Panel.T3(cell).let { it.copy(wide = wide ?: it.wide) } }
                "drawer" -> { v.onlyKeys(at, "at", "type", "wide"); Panel.Drawer(cell).let { it.copy(wide = wide ?: it.wide) } }
                else -> throw LayoutError("$at.type: must be one of ${Panel.TYPES.joinToString()}, got \"$type\"")
            }
        }

        private fun parseWide(at: String, v: Any?): Wide {
            val s = v as? String ?: throw LayoutError("$at: must be \"full\" or \"half\"")
            return Wide.entries.firstOrNull { it.id == s } ?: throw LayoutError("$at: \"$s\" is not \"full\" or \"half\"")
        }

        private fun parseBlock(at: String, v: Any): StackBlock {
            if (v !is JSONObject) throw LayoutError("$at: must be an object")
            return when (val type = v.string(at, "type")) {
                "clock" -> { v.onlyKeys(at, "type"); StackBlock.Clock }
                "apps" -> { v.onlyKeys(at, "type", "items"); StackBlock.Apps(parseItems(at, v, "items")) }
                "dock" -> { v.onlyKeys(at, "type"); StackBlock.Dock }
                else -> throw LayoutError("$at.type: must be one of ${StackBlock.TYPES.joinToString()}, got \"$type\"")
            }
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
 * One named layout: a sparse grid of [panels], the one [dock] every `dock` block draws, and the
 * [start] cell for each screen class. [expires] is kept for temporary layouts; nothing acts on it yet.
 * Home draws the grid itself ([GridHost.kt]); [pages] are what the edits call home's pages.
 */
data class Layout(
    val name: String,
    val panels: List<Panel>,
    val dock: List<AppRef>,
    val start: Start,
    val expires: Instant? = null,
) {
    fun panelAt(cell: Cell): Panel? = panels.firstOrNull { it.at == cell }

    /** The items of the `apps` block at [at], or null when there is none. */
    fun items(at: BlockAt): List<LayoutItem>? = ((panelAt(at.cell) as? Panel.Stack)?.blocks?.getOrNull(at.block) as? StackBlock.Apps)?.items

    /** Every `apps` block in the grid, by row, then column. */
    val appsBlocks: List<BlockAt>
        get() = panels.filterIsInstance<Panel.Stack>().sortedWith(compareBy({ it.at.row }, { it.at.column }))
            .flatMap { p -> p.blocks.mapIndexedNotNull { b, block -> if (block is StackBlock.Apps) BlockAt(p.at, b) else null } }

    /** Home's pages, for Add to home: the `apps` blocks in the narrow start's row, by column. */
    val pages: List<BlockAt> get() = appsBlocks.filter { it.cell.row == start.narrow.row }

    /** Each page's items, as [pages] orders them. */
    val allPages: List<List<LayoutItem>> get() = pages.map { items(it)!! }

    /** Every item in an `apps` block, anywhere in the grid. */
    val allItems: List<LayoutItem> get() = appsBlocks.flatMap { items(it)!! }

    /** This layout with the `apps` block at [at] holding [items]. */
    fun withItems(at: BlockAt, items: List<LayoutItem>): Layout = copy(
        panels = panels.map { p ->
            if (p.at != at.cell || p !is Panel.Stack) p
            else p.copy(blocks = p.blocks.mapIndexed { i, b -> if (i == at.block) StackBlock.Apps(items) else b })
        },
    )

    /** Every `apps` block's items changed by [change]. */
    fun mapApps(change: (List<LayoutItem>) -> List<LayoutItem>): Layout = copy(
        panels = panels.map { p -> if (p is Panel.Stack) p.copy(blocks = p.blocks.map { if (it is StackBlock.Apps) StackBlock.Apps(change(it.items)) else it }) else p },
    )

    /** Whether the start row has room for another stack with an `apps` block. */
    val roomForPanel: Boolean get() = panels.count { it.hasApps && it.at.row == start.narrow.row } < LayoutFile.MAX_APPS_PANELS

    /**
     * A new stack of `apps` (holding [items]) and `dock` after the last panel in the start row, as
     * an extra version 1 page converts; with where its `apps` block is.
     */
    fun withNewPanel(items: List<LayoutItem>): Pair<Layout, BlockAt> {
        val row = start.narrow.row
        val cell = Cell((panels.filter { it.at.row == row }.maxOfOrNull { it.at.column } ?: -1) + 1, row)
        return copy(panels = panels + Panel.Stack(cell, listOf(StackBlock.Apps(items), StackBlock.Dock))) to BlockAt(cell, 0)
    }

    /**
     * Without the panels an extra version 1 page converts to (a stack of only `apps` and `dock`)
     * whose `apps` block is empty; a start cell's panel stays, even empty (launcher polish 4.1's rule).
     */
    fun stripEmptyPanels(): Layout = copy(
        panels = panels.filterNot { p ->
            p is Panel.Stack && p.at != start.narrow && p.at != start.wide &&
                p.blocks.all { it is StackBlock.Dock || (it is StackBlock.Apps && it.items.isEmpty()) } && p.blocks.any { it is StackBlock.Apps }
        },
    )

    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        if (expires != null) put("expires", expires.toString())
        put("start", start.toJson())
        put("dock", JSONArray(dock.map { it.toString() }))
        put("panels", JSONArray(panels.map { it.toJson() }))
    }

    companion object {
        /**
         * The default grid, which a version 1 layout converts to: the `t3` panel left of home; home
         * as a stack of `clock`, `apps` ([home]) and `dock`; each of [pages] as a stack of `apps` and
         * `dock` to its right; one `drawer` panel in home's column on row 1 (decision lor-y7qi);
         * `start` on home.
         */
        fun grid(
            name: String,
            home: List<LayoutItem>,
            dock: List<AppRef>,
            pages: List<List<LayoutItem>> = emptyList(),
            expires: Instant? = null,
        ): Layout = Layout(
            name,
            listOf(Panel.T3(Cell(0, 0)), Panel.Stack(HOME, listOf(StackBlock.Clock, StackBlock.Apps(home), StackBlock.Dock))) +
                pages.mapIndexed { i, items -> Panel.Stack(Cell(HOME.column + 1 + i, 0), listOf(StackBlock.Apps(items), StackBlock.Dock)) } +
                Panel.Drawer(Cell(HOME.column, 1)),
            dock,
            Start(HOME),
            expires,
        )

        /** Home's cell in the default grid. */
        val HOME = Cell(1, 0)
    }
}

/** A cell of the grid, written `[column, row]`; both from 0. */
data class Cell(val column: Int, val row: Int) {
    fun toJson(): JSONArray = JSONArray().put(column).put(row)
    override fun toString() = "[$column, $row]"
}

/** Where home starts on each screen class; [wide] (the inner screen) is [narrow] when absent. */
data class Start(val narrow: Cell, val wide: Cell? = null) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("narrow", narrow.toJson())
        if (wide != null) put("wide", wide.toJson())
    }
}

/** An `apps` block: the stack panel at [cell], its block number [block]. */
data class BlockAt(val cell: Cell, val block: Int)

/**
 * How a panel sits on the inner screen (decision lor-b1dx): [FULL] is one inner screen; two
 * [HALF] panels of a row share one ([InnerScreens]).
 */
enum class Wide(val id: String) { FULL("full"), HALF("half") }

/**
 * A panel of the grid, at its cell. [wide] is the file's, or the type's default when the file has
 * none ([defaultWide]); the phone writes it only when it differs.
 */
sealed interface Panel {
    val at: Cell
    val wide: Wide
    fun toJson(): JSONObject

    /** A column of [blocks]. */
    data class Stack(override val at: Cell, val blocks: List<StackBlock>, override val wide: Wide = Wide.HALF) : Panel {
        override fun toJson(): JSONObject = head("stack").put("blocks", JSONArray(blocks.map { it.toJson() }))
    }

    /** The agent page. */
    data class T3(override val at: Cell, override val wide: Wide = Wide.FULL) : Panel {
        override fun toJson(): JSONObject = head("t3")
    }

    /** The app drawer as a panel; not the top-level `drawer` settings, which it draws with. */
    data class Drawer(override val at: Cell, override val wide: Wide = Wide.HALF) : Panel {
        override fun toJson(): JSONObject = head("drawer")
    }

    /** The type's `wide` when the file says none. */
    val defaultWide: Wide
        get() = when (this) {
            is Stack -> Wide.HALF
            is T3 -> Wide.FULL
            is Drawer -> Wide.HALF
        }

    /** Whether this is a stack with an `apps` block. */
    val hasApps: Boolean get() = this is Stack && blocks.any { it is StackBlock.Apps }

    companion object {
        val TYPES = listOf("stack", "t3", "drawer")
    }
}

/** A panel's `at`, `type`, and `wide` when it isn't the type's default. */
private fun Panel.head(type: String): JSONObject = JSONObject().put("at", at.toJson()).put("type", type)
    .apply { if (wide != defaultWide) put("wide", wide.id) }

/** A block of a stack panel. */
sealed interface StackBlock {
    val type: String
    fun toJson(): JSONObject = JSONObject().put("type", type)

    /** Today's header, drawn in the style `look.header` names. */
    data object Clock : StackBlock {
        override val type = "clock"
    }

    /** Apps and folders in reading order, written as version 1's `home`, at most 24. */
    data class Apps(val items: List<LayoutItem>) : StackBlock {
        override val type = "apps"
        override fun toJson(): JSONObject = super.toJson().put("items", JSONArray(items.map { it.toJsonValue() }))
    }

    /** The layout's one `dock`. */
    data object Dock : StackBlock {
        override val type = "dock"
    }

    companion object {
        val TYPES = listOf("clock", "apps", "dock")
    }
}

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
data class Drawer(
    val view: DrawerView? = null,
    val sections: List<DrawerSection> = emptyList(),
    /** Open the drawer with the search field focused and the keyboard up (launcher polish 5.5); written only when true. */
    val keyboard: Boolean = false,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        if (view != null) put("view", view.id)
        if (sections.isNotEmpty()) put("sections", JSONArray(sections.map { it.toJson() }))
        if (keyboard) put("keyboard", true)
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
    /**
     * A parsed file and the modified time it was read or written at. After an edit that wrote,
     * [undo] holds what [undo] needs to put the file back (launcher polish 4.2).
     */
    data class Read(val layout: LayoutFile, val stamp: Long, val undo: Undo? = null)

    /** The file's text before an edit, and the text the edit wrote. */
    data class Undo(val before: String, val wrote: String)

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
        return readText().first
    }

    /** The file parsed, and its text exactly as it is on disk. */
    private fun readText(): Pair<Read, String> {
        val stamp = file.lastModified()
        if (file.length() > LayoutFile.MAX_BYTES) throw LayoutError("the file is over ${LayoutFile.MAX_BYTES / 1024} KB")
        val text = file.readText()
        val layout = LayoutFile.parse(text)
        keepGood(text)
        return Read(layout, stamp) to text
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
     * thing; otherwise the file is left as it was. Fails with the [LayoutError] or I/O error. When
     * [change] changes nothing, nothing is written and the result has no [Read.undo].
     */
    fun edit(change: (LayoutFile) -> LayoutFile): Result<Read> = runCatching {
        if (!file.exists()) throw LayoutError("there is no layout file yet")
        val (read, before) = readText()
        val changed = change(read.layout)
        if (changed == read.layout) return@runCatching read
        val stamp = write(changed)
        Read(changed, stamp, Undo(before, changed.toJson() + "\n"))
    }

    /**
     * Puts the file back as it was before the edit [undo] came from, byte for byte: the copy of
     * its text, not an inverse edit, so a hand-formatted file comes back as it was written. Refuses
     * when the file no longer holds what that edit wrote (an agent or adb wrote since).
     */
    fun undo(undo: Undo): Read {
        if (!file.exists() || file.readText() != undo.wrote) throw LayoutError("layout.json changed since; nothing was undone")
        val layout = LayoutFile.parse(undo.before)
        writeAtomically(file, undo.before)
        keepGood(undo.before)
        return Read(layout, file.lastModified())
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
