package com.spadaval.lor.home

import org.json.JSONObject

/*
 * `look` in `layout.json` (launcher polish 6.4): [HomeLook] is in the shared module; reading and
 * writing it with org.json stays here, with the rest of the file's parsing.
 */

/** The look as `layout.json` holds it: only fields that differ from the defaults, so a default written by hand is dropped on the next phone edit. */
fun HomeLook.toJson(): JSONObject = JSONObject().apply {
    val g = JSONObject()
    fun cols(name: String, c: HomeLook.Columns, d: HomeLook.Columns) {
        val o = JSONObject()
        if (c.home != d.home) o.put("home", c.home)
        if (c.drawer != d.drawer) o.put("drawer", c.drawer)
        if (o.length() > 0) g.put(name, o)
    }
    cols("narrow", grid.narrow, HomeLook.NARROW)
    cols("wide", grid.wide, HomeLook.WIDE)
    if (g.length() > 0) put("grid", g)
    if (!labels) put("labels", false)
    if (labelSize != HomeLook.DEFAULT.labelSize) put("labelSize", labelSize)
    if (iconSize != HomeLook.DEFAULT.iconSize) put("iconSize", iconSize)
    iconPack?.let { put("iconPack", it) }
    if (themedIcons) put("themedIcons", true)
    if (iconShape != HomeLook.IconShape.SYSTEM) put("iconShape", iconShape.id)
    if (header != HomeLook.Header.DATE) put("header", header.id)
}

private val PACKAGE = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")

/** Reads the file's `look`, or throws [LayoutError] naming the first problem. */
fun HomeLook.Companion.parse(v: Any): HomeLook {
    if (v !is JSONObject) throw LayoutError("look: must be an object")
    v.only("look", "grid", "labels", "labelSize", "iconSize", "iconPack", "themedIcons", "iconShape", "header")
    var grid = HomeLook.Grid()
    if (v.has("grid")) {
        val g = v.opt("grid") as? JSONObject ?: throw LayoutError("look.grid: must be an object")
        g.only("look.grid", "narrow", "wide")
        fun cols(name: String, d: HomeLook.Columns, home: IntRange, drawer: IntRange): HomeLook.Columns {
            if (!g.has(name)) return d
            val at = "look.grid.$name"
            val c = g.opt(name) as? JSONObject ?: throw LayoutError("$at: must be an object")
            c.only(at, "home", "drawer")
            return HomeLook.Columns(c.int(at, "home", home) ?: d.home, c.int(at, "drawer", drawer) ?: d.drawer)
        }
        grid = HomeLook.Grid(cols("narrow", NARROW, NARROW_HOME, NARROW_DRAWER), cols("wide", WIDE, WIDE_HOME, WIDE_DRAWER))
    }
    val labels = if (v.has("labels")) v.opt("labels") as? Boolean ?: throw LayoutError("look.labels: must be true or false") else true
    return HomeLook(
        grid = grid,
        labels = labels,
        labelSize = v.int("look", "labelSize", LABEL_SIZES) ?: DEFAULT.labelSize,
        iconSize = v.int("look", "iconSize", ICON_SIZES) ?: DEFAULT.iconSize,
        iconPack = if (v.has("iconPack")) {
            (v.opt("iconPack") as? String)?.takeIf { PACKAGE.matches(it) }
                ?: throw LayoutError("look.iconPack: \"${v.opt("iconPack")}\" is not a package name")
        } else null,
        themedIcons = if (v.has("themedIcons")) v.opt("themedIcons") as? Boolean ?: throw LayoutError("look.themedIcons: must be true or false") else false,
        iconShape = if (v.has("iconShape")) {
            HomeLook.IconShape.entries.firstOrNull { it.id == v.opt("iconShape") }
                ?: throw LayoutError("look.iconShape: must be one of ${HomeLook.IconShape.entries.joinToString { it.id }}")
        } else HomeLook.IconShape.SYSTEM,
        header = if (v.has("header")) {
            HomeLook.Header.entries.firstOrNull { it.id == v.opt("header") }
                ?: throw LayoutError("look.header: must be one of ${HomeLook.Header.entries.joinToString { it.id }}")
        } else HomeLook.Header.DATE,
    )
}

private fun JSONObject.only(at: String, vararg allowed: String) {
    keys().asSequence().firstOrNull { it !in allowed }?.let {
        throw LayoutError("$at: unknown field \"$it\" (allowed: ${allowed.joinToString()})")
    }
}

/** An optional whole number in [range]; null when absent. */
private fun JSONObject.int(at: String, key: String, range: IntRange): Int? {
    if (!has(key)) return null
    val n = opt(key)
    val i = (n as? Int) ?: throw LayoutError("$at.$key: must be a whole number")
    if (i !in range) throw LayoutError("$at.$key: $i is not between ${range.first} and ${range.last}")
    return i
}
