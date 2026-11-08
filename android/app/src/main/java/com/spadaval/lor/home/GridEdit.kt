package com.spadaval.lor.home

/*
 * The minimap's edits to the grid (lor-v692, for lor-f3vn's minimap) and the layout lock's switch.
 * Each is a pure function from the parsed file to the edited one on the active layout, throwing a
 * [LayoutError] whose message says why it refuses, as HomeEdit.kt's do. No Android here, so the JVM
 * tests reach all of it. The rules are written down in layout-v2.md's "What the phone's edits mean".
 *
 * An edit that names a panel or a block also takes what the minimap drew there, and refuses with
 * [HOME_CHANGED] when the file holds something else there now (an agent or adb wrote while the
 * minimap was open), so it never moves or removes the wrong thing.
 *
 * No edit here checks the lock: [LayoutStore.edit] refuses any edit to the grid while it is set.
 * Every result is a file the parser takes (GridEditTest.everyEditParsesBackOrRefuses).
 */

/** A screen class, for [setStart]: the phone and the cover screen, or the inner screen. */
enum class StartScreen { NARROW, WIDE }

/**
 * The cells the minimap draws and a new or moved panel may take: columns 0 to one past the last
 * panel's, and rows 0 to one past the last panel's, of the layout's panels.
 */
data class Reach(val columns: IntRange, val rows: IntRange) {
    operator fun contains(cell: Cell): Boolean = cell.column in columns && cell.row in rows

    /** Every cell in reach, by row, then column. */
    val cells: List<Cell> get() = rows.flatMap { r -> columns.map { c -> Cell(c, r) } }
}

val Layout.reach: Reach
    get() = Reach(0..(panels.maxOf { it.at.column } + 1), 0..(panels.maxOf { it.at.row } + 1))

/** Locks home (decision lor-73li) or unlocks it; refuses when it is already so. */
fun LayoutFile.setLocked(on: Boolean): LayoutFile {
    if (locked == on) throw LayoutError(if (on) "home is already locked" else "home isn't locked")
    return copy(locked = on)
}

/**
 * Adds a panel of [type] at [cell], an empty cell in reach. A `stack` is an empty `apps` block and
 * `dock`, as a new page is. A layout holds one `t3` and one `drawer` panel, and a row at most
 * [LayoutFile.MAX_APPS_PANELS] stacks with an `apps` block.
 */
fun LayoutFile.addPanel(cell: Cell, type: String): LayoutFile {
    val layout = current
    if (cell !in layout.reach) throw LayoutError("a new panel goes next to the grid")
    if (layout.panelAt(cell) != null) throw LayoutError(HOME_CHANGED)
    val panel = when (type) {
        "stack" -> Panel.Stack(cell, listOf(StackBlock.Apps(emptyList()), StackBlock.Dock))
        "t3" -> Panel.T3(cell)
        "drawer" -> Panel.Drawer(cell)
        else -> throw LayoutError("there's no panel type \"$type\"")
    }
    ONCE[type]?.let { name -> if (layout.panels.any { it.typeName == type }) throw LayoutError("there's already $name") }
    return withCurrent(layout.copy(panels = layout.panels + panel).checkRows())
}

/**
 * Moves [panel], drawn at [from], to [to], a cell in reach: into it when it is empty, else the two
 * panels swap. A `start` that named a moved panel's cell names its new cell. Moving it where it is
 * changes nothing.
 */
fun LayoutFile.movePanel(from: Cell, panel: Panel, to: Cell): LayoutFile {
    val layout = current
    checkPanel(from, panel)
    if (to !in layout.reach) throw LayoutError("a panel goes next to the grid")
    if (to == from) return this
    val other = layout.panelAt(to)
    val moved = layout.panels.map {
        when (it.at) {
            from -> it.movedTo(to)
            to -> it.movedTo(from)
            else -> it
        }
    }
    fun follow(c: Cell): Cell = when {
        c == from -> to
        c == to && other != null -> from
        else -> c
    }
    val start = layout.start.let { s -> Start(follow(s.narrow), s.wide?.let(::follow)) }
    return withCurrent(layout.copy(panels = moved, start = start).checkRows())
}

/** Removes [panel], drawn at [at], with whatever it holds. A panel a `start` names stays. */
fun LayoutFile.removePanel(at: Cell, panel: Panel): LayoutFile {
    val layout = current
    checkPanel(at, panel)
    if (at == layout.start.narrow || at == layout.start.wide) throw LayoutError("home starts there; choose another start first")
    return withCurrent(layout.copy(panels = layout.panels.filterNot { it.at == at }))
}

/** Sets how [panel], drawn at [at], sits on the inner screen; refuses when it already does. */
fun LayoutFile.setWide(at: Cell, panel: Panel, wide: Wide): LayoutFile {
    val layout = current
    checkPanel(at, panel)
    if (panel.wide == wide) throw LayoutError("it's already ${wide.id}")
    val changed = when (panel) {
        is Panel.Stack -> panel.copy(wide = wide)
        is Panel.T3 -> panel.copy(wide = wide)
        is Panel.Drawer -> panel.copy(wide = wide)
    }
    return withCurrent(layout.copy(panels = layout.panels.map { if (it.at == at) changed else it }))
}

/**
 * Makes the panel at [at] where home starts on [screen]. `start.wide` is written only when it
 * differs from `start.narrow` (absent, it is the narrow start), so the wide start is cleared when
 * it would equal the narrow one. Refuses when home already starts there on that screen.
 */
fun LayoutFile.setStart(at: Cell, screen: StartScreen): LayoutFile {
    val layout = current
    if (layout.panelAt(at) == null) throw LayoutError(HOME_CHANGED)
    val s = layout.start
    val start = when (screen) {
        StartScreen.NARROW -> {
            if (s.narrow == at) throw LayoutError("home already starts there")
            Start(at, s.wide.takeUnless { it == at })
        }
        StartScreen.WIDE -> {
            if ((s.wide ?: s.narrow) == at) throw LayoutError("the inner screen already starts there")
            Start(s.narrow, at.takeUnless { it == s.narrow })
        }
    }
    return withCurrent(layout.copy(start = start))
}

/**
 * Adds a block of [type], one that takes no field ([StackBlock.bare]), to the stack at [at],
 * before its block [index] (its block count to add it last). A stack holds each type once
 * ([Panel.Stack.takes]). A `widget` is added on home, through Android's bind flow ([addWidget]).
 */
fun LayoutFile.addBlock(at: Cell, index: Int, type: String): LayoutFile {
    val layout = current
    val stack = layout.panelAt(at) as? Panel.Stack ?: throw LayoutError(HOME_CHANGED)
    if (index !in 0..stack.blocks.size) throw LayoutError(HOME_CHANGED)
    val block = StackBlock.bare(type) ?: throw LayoutError(
        if (type == StackBlock.Widget.TYPE) "a widget is added from the home menu" else "there's no block type \"$type\"",
    )
    if (!stack.takes(type)) throw LayoutError(oneOf(type))
    val blocks = stack.blocks.toMutableList().apply { add(index, block) }
    return withCurrent(layout.withPanel(stack.copy(blocks = blocks)).checkRows())
}

/**
 * Moves [block], drawn at [from], to the stack at [to], to be its block [index]. Within one stack,
 * [index] is where it ends up (moving it where it is changes nothing); into another, it goes before
 * that stack's block [index] (its block count for last). An `apps` block keeps its items. A stack's
 * last block can't leave it.
 */
fun LayoutFile.moveBlock(from: BlockAt, block: StackBlock, to: Cell, index: Int): LayoutFile {
    val layout = current
    val source = checkBlock(from, block)
    if (to == from.cell) {
        if (index !in source.blocks.indices) throw LayoutError(HOME_CHANGED)
        if (index == from.block) return this
        val blocks = source.blocks.toMutableList().apply { add(index, removeAt(from.block)) }
        return withCurrent(layout.withPanel(source.copy(blocks = blocks)))
    }
    val target = layout.panelAt(to) as? Panel.Stack ?: throw LayoutError(HOME_CHANGED)
    if (index !in 0..target.blocks.size) throw LayoutError(HOME_CHANGED)
    if (source.blocks.size == 1) throw LayoutError(LAST_BLOCK)
    if (!target.takes(block.type)) throw LayoutError(oneOf(block.type))
    val left = source.copy(blocks = source.blocks.filterIndexed { i, _ -> i != from.block })
    val took = target.copy(blocks = target.blocks.toMutableList().apply { add(index, block) })
    return withCurrent(layout.withPanel(left).withPanel(took).checkRows())
}

/** Removes [block], drawn at [at]; an `apps` block's apps leave home with it. A stack's last block stays. */
fun LayoutFile.removeBlock(at: BlockAt, block: StackBlock): LayoutFile {
    val layout = current
    val stack = checkBlock(at, block)
    if (stack.blocks.size == 1) throw LayoutError(LAST_BLOCK)
    return withCurrent(layout.withPanel(stack.copy(blocks = stack.blocks.filterIndexed { i, _ -> i != at.block })))
}

/** The panel types a layout holds at most one of, with how a refusal names one. */
private val ONCE = mapOf("t3" to "an agent page", "drawer" to "a drawer")

internal const val LAST_BLOCK = "a stack needs a block; remove the panel instead"

private fun oneOf(type: String) = "a stack holds one \"$type\" block"

private val Panel.typeName: String
    get() = when (this) {
        is Panel.Stack -> "stack"
        is Panel.T3 -> "t3"
        is Panel.Drawer -> "drawer"
    }

private fun LayoutFile.checkPanel(at: Cell, panel: Panel) {
    if (current.panelAt(at) != panel) throw LayoutError(HOME_CHANGED)
}

/** The stack holding [block] at [at], when the file holds it there. */
private fun LayoutFile.checkBlock(at: BlockAt, block: StackBlock): Panel.Stack {
    val stack = current.panelAt(at.cell) as? Panel.Stack ?: throw LayoutError(HOME_CHANGED)
    if (stack.blocks.getOrNull(at.block) != block) throw LayoutError(HOME_CHANGED)
    return stack
}

/** This layout with [panel] in place of the one at its cell. */
internal fun Layout.withPanel(panel: Panel): Layout = copy(panels = panels.map { if (it.at == panel.at) panel else it })

/** This layout, or a refusal when a row holds more stacks with an `apps` block than the parser takes. */
private fun Layout.checkRows(): Layout {
    panels.filter { it.hasApps }.groupBy { it.at.row }.entries.firstOrNull { it.value.size > LayoutFile.MAX_APPS_PANELS }?.let { (row, _) ->
        throw LayoutError("row $row holds ${LayoutFile.MAX_APPS_PANELS} stacks with apps already")
    }
    return this
}
