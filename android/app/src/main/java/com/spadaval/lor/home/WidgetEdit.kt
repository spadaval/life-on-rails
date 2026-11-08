package com.spadaval.lor.home

/*
 * The `widget` block's edits (decision lor-9wlx) and the sweep of ids no layout names, for the
 * widget host (lor-r0f9). Each edit is a pure function from the parsed file to the edited one on
 * the active layout, throwing a [LayoutError] whose message says why it refuses, as HomeEdit.kt's
 * do. No Android here, so the JVM tests reach all of it.
 *
 * Edits address a widget by its appWidgetId, not its place, so other edits don't move it. Like the
 * minimap's block edits, none of these removes an extra page an edit empties ([Layout.stripEmptyPanels]).
 */

/**
 * Adds a widget block of [id], [provider] (`package/class`, `package/.Short` expanded) and [height]
 * (dp) to the stack at [at]: before its `apps` block, else before its `dock`, else last. Refuses an
 * [id] the active layout already holds, and a provider or height the file can't hold.
 */
fun LayoutFile.addWidget(at: Cell, id: Int, provider: String, height: Int): LayoutFile {
    val layout = current
    val stack = layout.panelAt(at) as? Panel.Stack ?: throw LayoutError(HOME_CHANGED)
    if (id < 1) throw LayoutError("a widget's id is a whole number from 1, got $id")
    if (layout.widgets.any { it.id == id }) throw LayoutError("widget $id is already on home")
    val full = StackBlock.Widget.provider(provider) ?: throw LayoutError("\"$provider\" is not package/class")
    checkHeight(height)
    val index = stack.blocks.indexOfFirst { it is StackBlock.Apps }.takeIf { it >= 0 }
        ?: stack.blocks.indexOfFirst { it is StackBlock.Dock }.takeIf { it >= 0 }
        ?: stack.blocks.size
    val blocks = stack.blocks.toMutableList().apply { add(index, StackBlock.Widget(id, full, height)) }
    return withCurrent(layout.withPanel(stack.copy(blocks = blocks)))
}

/** Removes the widget [id]. A stack's last block stays, as the minimap's Remove refuses it. */
fun LayoutFile.removeWidget(id: Int): LayoutFile {
    val (layout, stack, index) = widget(id)
    if (stack.blocks.size == 1) throw LayoutError(LAST_BLOCK)
    return withCurrent(layout.withPanel(stack.copy(blocks = stack.blocks.filterIndexed { i, _ -> i != index })))
}

/**
 * Sets the widget [id]'s height to [height] dp. Setting the height it has changes nothing, so a
 * resize let go where it began writes nothing. The provider's limits are the caller's to apply;
 * this refuses only a height the file can't hold.
 */
fun LayoutFile.setWidgetHeight(id: Int, height: Int): LayoutFile {
    val (layout, stack, index) = widget(id)
    checkHeight(height)
    val w = stack.blocks[index] as StackBlock.Widget
    if (w.height == height) return this
    return withCurrent(layout.withPanel(stack.copy(blocks = stack.blocks.mapIndexed { i, b -> if (i == index) w.copy(height = height) else b })))
}

/**
 * The ids in [hostIds] (the widget host's, `AppWidgetHost.getAppWidgetIds`) that no layout of any
 * of [files] names, to delete from the host (Launcher3's `DatabaseHelper.removeGhostWidgets`,
 * Apache-2.0). The caller passes `layout.json` and `layout.good.json` as parsed, and nothing when
 * `layout.json` doesn't parse; with no file, nothing is deleted.
 */
fun widgetIdsToDelete(hostIds: Collection<Int>, files: List<LayoutFile>): List<Int> {
    if (files.isEmpty()) return emptyList()
    val named = files.flatMap { f -> f.layouts.flatMap { l -> l.widgets.map { it.id } } }.toSet()
    return hostIds.filter { it !in named }
}

/** The active layout, the stack holding the widget [id], and its index there. */
private fun LayoutFile.widget(id: Int): Triple<Layout, Panel.Stack, Int> {
    val layout = current
    for (p in layout.panels) {
        if (p !is Panel.Stack) continue
        val i = p.blocks.indexOfFirst { it is StackBlock.Widget && it.id == id }
        if (i >= 0) return Triple(layout, p, i)
    }
    throw LayoutError("widget $id isn't on home")
}

private fun checkHeight(height: Int) {
    val h = StackBlock.Widget.HEIGHTS
    if (height !in h) throw LayoutError("a widget's height is ${h.first} to ${h.last} dp, got $height")
}
