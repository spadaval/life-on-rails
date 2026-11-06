package com.spadaval.lor.home

import org.json.JSONObject

/**
 * How home looks on this device (launcher polish 6.4): `look` at the top of `layout.json`, every
 * field optional (Schema additions in docs/missions/launcher-polish/plan.md). Defaults are today's
 * look; [toJson] writes only fields that differ, so a default written by hand is dropped on the
 * next phone edit, meaning the same.
 */
data class HomeLook(
    val grid: Grid = Grid(),
    val labels: Boolean = true,
    val labelSize: Int = 12,
    val iconSize: Int = 52,
) {
    /** Columns of home's grid and the drawer's, for one class of screen. */
    data class Columns(val home: Int, val drawer: Int)

    /** Columns under 600 dp wide ([narrow]: the phone, the Fold's cover screen) and from 600 dp ([wide]). */
    data class Grid(val narrow: Columns = NARROW, val wide: Columns = WIDE)

    /** The columns for a screen [widthDp] wide. */
    fun columnsFor(widthDp: Float): Columns = if (widthDp >= WIDE_DP) grid.wide else grid.narrow

    fun toJson(): JSONObject = JSONObject().apply {
        val g = JSONObject()
        fun cols(name: String, c: Columns, d: Columns) {
            val o = JSONObject()
            if (c.home != d.home) o.put("home", c.home)
            if (c.drawer != d.drawer) o.put("drawer", c.drawer)
            if (o.length() > 0) g.put(name, o)
        }
        cols("narrow", grid.narrow, NARROW)
        cols("wide", grid.wide, WIDE)
        if (g.length() > 0) put("grid", g)
        if (!labels) put("labels", false)
        if (labelSize != DEFAULT.labelSize) put("labelSize", labelSize)
        if (iconSize != DEFAULT.iconSize) put("iconSize", iconSize)
    }

    companion object {
        const val WIDE_DP = 600f
        val NARROW = Columns(home = 4, drawer = 4)
        val WIDE = Columns(home = 5, drawer = 6)
        val DEFAULT = HomeLook()
        val NARROW_HOME = 4..6
        val NARROW_DRAWER = 3..6
        val WIDE_HOME = 4..8
        val WIDE_DRAWER = 4..10
        val LABEL_SIZES = 12..14
        val ICON_SIZES = 44..56

        fun parse(v: Any): HomeLook {
            if (v !is JSONObject) throw LayoutError("look: must be an object")
            v.only("look", "grid", "labels", "labelSize", "iconSize")
            var grid = Grid()
            if (v.has("grid")) {
                val g = v.opt("grid") as? JSONObject ?: throw LayoutError("look.grid: must be an object")
                g.only("look.grid", "narrow", "wide")
                fun cols(name: String, d: Columns, home: IntRange, drawer: IntRange): Columns {
                    if (!g.has(name)) return d
                    val at = "look.grid.$name"
                    val c = g.opt(name) as? JSONObject ?: throw LayoutError("$at: must be an object")
                    c.only(at, "home", "drawer")
                    return Columns(c.int(at, "home", home) ?: d.home, c.int(at, "drawer", drawer) ?: d.drawer)
                }
                grid = Grid(cols("narrow", NARROW, NARROW_HOME, NARROW_DRAWER), cols("wide", WIDE, WIDE_HOME, WIDE_DRAWER))
            }
            val labels = if (v.has("labels")) v.opt("labels") as? Boolean ?: throw LayoutError("look.labels: must be true or false") else true
            return HomeLook(
                grid = grid,
                labels = labels,
                labelSize = v.int("look", "labelSize", LABEL_SIZES) ?: DEFAULT.labelSize,
                iconSize = v.int("look", "iconSize", ICON_SIZES) ?: DEFAULT.iconSize,
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
    }
}
