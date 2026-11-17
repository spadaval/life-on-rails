package com.spadaval.lor.home

/**
 * How home looks on this device (launcher polish 6.4): `look` at the top of `layout.json`, every
 * field optional (Schema additions in docs/missions/launcher-polish/plan.md). Defaults are today's
 * look; the app's `toJson` writes only fields that differ, so a default written by hand is dropped on the
 * next phone edit, meaning the same.
 */
data class HomeLook(
    val grid: Grid = Grid(),
    val labels: Boolean = true,
    val labelSize: Int = 12,
    val iconSize: Int = 52,
    /** An icon pack's package name (launcher polish 6.5); null draws the apps' own icons. */
    val iconPack: String? = null,
    /** Apps with a monochrome layer draw it in LoR's colours (6.5). */
    val themedIcons: Boolean = false,
    val iconShape: IconShape = IconShape.SYSTEM,
    /** What home's header shows (launcher polish 6.7). */
    val header: Header = Header.DATE,
) {
    enum class Header(val id: String, val title: String) { DATE("date", "Date"), CLOCK("clock", "Clock"), NONE("none", "None") }

    /** The mask for adaptive icons; pack icons keep their own shape. */
    enum class IconShape(val id: String, val title: String) { SYSTEM("system", "System"), SQUARE("square", "Square"), CIRCLE("circle", "Circle") }

    /** Columns of home's grid and the drawer's, for one class of screen. */
    data class Columns(val home: Int, val drawer: Int)

    /** Columns under 600 dp wide ([narrow]: the phone, the Fold's cover screen) and from 600 dp ([wide]). */
    data class Grid(val narrow: Columns = NARROW, val wide: Columns = WIDE)

    /** The columns for a screen [widthDp] wide. */
    fun columnsFor(widthDp: Float): Columns = if (widthDp >= WIDE_DP) grid.wide else grid.narrow

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
    }
}
