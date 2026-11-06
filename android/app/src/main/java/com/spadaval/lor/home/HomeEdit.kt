package com.spadaval.lor.home

/*
 * Phone edits to the layout file (launcher polish 1.1, and 4.1 for pages, places and dragging).
 * Each is a pure function from the parsed file to the edited one, throwing a [LayoutError] whose
 * message says why it refuses (a limit, a duplicate, nothing to do). No Android here, so the JVM
 * tests reach all of it.
 *
 * Edits act on the active layout, except hide, which acts on every layout. A folder an edit
 * touches becomes `pinned` (it is Sumeet's now); a folder an edit empties is removed, and so is
 * an extra page (page 0, `home`, stays even when empty).
 *
 * Which app a ref names is worked out as home draws it (R3 of Increment 1's review): a package
 * name draws as the first main-profile activity of the package in label order, so it names that
 * activity only. The edits that ask take the installed main-profile apps in label order; an app
 * the list doesn't hold is taken as installed after all of them.
 *
 * Places in the file are [Spot]s, the file's indices (not the drawn order: home skips a ref that
 * isn't installed but keeps its place). An edit by place also takes what home drew there, and
 * refuses when the file holds something else there now (an agent or adb wrote while home was on
 * screen), so it never moves or removes the wrong thing.
 */

/** A place in the active layout. Page 0 is `home`; page n is `pages[n-1]`. */
sealed interface Spot {
    data class Page(val page: Int, val index: Int) : Spot
    data class Dock(val index: Int) : Spot
    /** The app at [app] in the folder at `Page(page, index)`. */
    data class InFolder(val page: Int, val index: Int, val app: Int) : Spot {
        val folder: Page get() = Page(page, index)
    }
}

/** Where a moved item comes from: a place in the file, with what home drew there, or the drawer. */
sealed interface From {
    data class At(val spot: Spot, val item: LayoutItem) : From
    data class Drawer(val app: AppId, val ref: AppRef) : From
}

/** Where a moved item goes. */
sealed interface Target {
    /** In front of the item at [spot] (a page, the dock or a folder). */
    data class Before(val spot: Spot) : Target
    data class AtEnd(val place: Place) : Target
    /** Onto the app or folder at `Page(page, index)`: the two make a folder, or the folder takes it. */
    data class Onto(val spot: Spot.Page) : Target
    data object Remove : Target
}

/** A list an item can be appended to. */
sealed interface Place {
    data class OnPage(val page: Int) : Place
    data object Dock : Place
    data class InFolder(val folder: Spot.Page) : Place
    /** A page after the last, made by the drop. */
    data object NewPage : Place
}

/** Where [LayoutFile.current] holds an app: on home itself (any page), in the dock, in a folder. */
data class Places(val home: Boolean, val dock: Boolean, val folder: String?)

/** A drop's result: the edited file, and where the moved item is now (null when it was removed). */
data class Dropped(val file: LayoutFile, val at: Spot?)

/** The activity [this] draws as: the first of [installed] it matches, in their (label) order. */
fun AppRef.resolve(installed: List<AppId>): AppId? = installed.firstOrNull { matches(it.packageName, it.className) }

/** Whether [this] names [app] as home draws it. */
fun AppRef.names(app: AppId, installed: List<AppId> = emptyList()): Boolean =
    resolve(if (app in installed) installed else installed + app) == app

/** Whether two refs draw as the same app; a ref nothing installed matches is compared as written. */
private fun sameApp(a: AppRef, b: AppRef, installed: List<AppId>): Boolean {
    val ra = a.resolve(installed)
    val rb = b.resolve(installed)
    return if (ra != null && rb != null) ra == rb else a == b
}

/** A new folder's name: the two apps' category when they share one, else "Folder". */
fun folderName(a: Category?, b: Category?): String = if (a != null && a == b) a.title else "Folder"

fun LayoutFile.placesOf(app: AppId, installed: List<AppId> = emptyList()): Places {
    val layout = current
    val items = layout.allPages.flatten()
    return Places(
        home = items.any { it is AppRef && it.names(app, installed) },
        dock = layout.dock.any { it.names(app, installed) },
        folder = items.filterIsInstance<Folder>().firstOrNull { f -> f.apps.any { it.names(app, installed) } }?.name,
    )
}

/** What [spot] holds in the active layout, or null when nothing is there. */
fun LayoutFile.itemAt(spot: Spot): LayoutItem? {
    val layout = current
    return when (spot) {
        is Spot.Page -> layout.allPages.getOrNull(spot.page)?.getOrNull(spot.index)
        is Spot.Dock -> layout.dock.getOrNull(spot.index)
        is Spot.InFolder -> (itemAt(spot.folder) as? Folder)?.apps?.getOrNull(spot.app)
    }
}

private fun LayoutFile.check(from: From.At) {
    if (itemAt(from.spot) != from.item) throw LayoutError(HOME_CHANGED)
}

const val HOME_CHANGED = "Home changed; try again"

/**
 * Adds [ref] (written for [app]) to the end of the first page with room; when every page is
 * full, to a new page while there are fewer than [LayoutFile.MAX_PAGES] after home.
 */
fun LayoutFile.addToHome(app: AppId, ref: AppRef, installed: List<AppId> = emptyList()): LayoutFile {
    val layout = current
    val places = placesOf(app, installed)
    if (places.home) throw LayoutError("it's already on home")
    places.folder?.let { throw LayoutError("it's already on home, in $it") }
    val page = layout.allPages.indexOfFirst { it.size < LayoutFile.MAX_HOME }
    return when {
        page >= 0 -> withCurrent(layout.withPage(page, layout.allPages[page] + ref))
        layout.pages.size < LayoutFile.MAX_PAGES -> withCurrent(layout.copy(pages = layout.pages + Page(listOf(ref))))
        else -> throw LayoutError("home is full (${LayoutFile.MAX_PAGES + 1} pages of ${LayoutFile.MAX_HOME})")
    }
}

/** Adds [ref] (written for [app]) to the end of the dock. */
fun LayoutFile.addToDock(app: AppId, ref: AppRef, installed: List<AppId> = emptyList()): LayoutFile {
    val layout = current
    if (placesOf(app, installed).dock) throw LayoutError("it's already in the dock")
    if (layout.dock.size >= LayoutFile.MAX_DOCK) throw LayoutError("the dock is full (${LayoutFile.MAX_DOCK} apps)")
    return withCurrent(layout.copy(dock = layout.dock + ref))
}

/** Removes the refs on home's pages (not in folders) that name [app]. */
fun LayoutFile.removeFromHome(app: AppId, installed: List<AppId> = emptyList()): LayoutFile {
    if (!placesOf(app, installed).home) throw LayoutError("it isn't on home")
    return withCurrent(current.mapPages { items -> items.filterNot { it is AppRef && it.names(app, installed) } }.stripEmptyPages())
}

fun LayoutFile.removeFromDock(app: AppId, installed: List<AppId> = emptyList()): LayoutFile {
    val layout = current
    if (!placesOf(app, installed).dock) throw LayoutError("it isn't in the dock")
    return withCurrent(layout.copy(dock = layout.dock.filterNot { it.names(app, installed) }))
}

/** Removes [app] from the first folder (by place, not name) that holds it. */
fun LayoutFile.removeFromFolder(app: AppId, installed: List<AppId> = emptyList()): LayoutFile {
    val layout = current
    layout.allPages.forEachIndexed { p, items ->
        items.forEachIndexed { i, item ->
            if (item is Folder) {
                val a = item.apps.indexOfFirst { it.names(app, installed) }
                if (a >= 0) return removeAt(From.At(Spot.InFolder(p, i, a), item.apps[a]))
            }
        }
    }
    throw LayoutError("it isn't in a folder")
}

/** Removes the item [from] names (an app, or a folder with its apps) from where it is. */
fun LayoutFile.removeAt(from: From.At): LayoutFile = drop(from, Target.Remove).file

/**
 * Hides every activity of [packageName]: adds it to `hidden` and removes, in every layout, each
 * ref to that package from every page, the dock and folders.
 */
fun LayoutFile.hide(packageName: String): LayoutFile {
    if (hidden.any { it.packageName == packageName && it.className == null }) throw LayoutError("it's already hidden")
    val gone = { ref: AppRef -> ref.packageName == packageName }
    val layouts = layouts.map { layout ->
        layout.mapPages { items ->
            items.mapNotNull { item ->
                when (item) {
                    is AppRef -> item.takeUnless(gone)
                    is Folder -> if (item.apps.any(gone)) item.without(gone) else item
                }
            }
        }.copy(dock = layout.dock.filterNot(gone)).stripEmptyPages()
    }
    return copy(layouts = layouts, hidden = hidden + AppRef(packageName))
}

/** Takes [packageName] out of `hidden`, nothing else: what hiding removed stays removed. */
fun LayoutFile.unhide(packageName: String): LayoutFile {
    if (hidden.none { it.packageName == packageName }) throw LayoutError("it isn't hidden")
    return copy(hidden = hidden.filterNot { it.packageName == packageName })
}

/** Renames the folder [at] holds. */
fun LayoutFile.renameFolder(at: From.At, name: String): LayoutFile {
    val n = name.trim()
    if (n.isEmpty()) throw LayoutError("a folder needs a name")
    return changeFolder(at) { it.copy(name = n) }
}

/** Makes [cover], one of the folder's apps, its tile; null for the four-app preview. */
fun LayoutFile.setCover(at: From.At, cover: AppRef?): LayoutFile = changeFolder(at) { f ->
    if (cover != null && cover !in f.apps) throw LayoutError("\"$cover\" isn't in ${f.name}")
    f.copy(cover = cover)
}

private fun LayoutFile.changeFolder(at: From.At, change: (Folder) -> Folder): LayoutFile {
    check(at)
    val spot = at.spot as? Spot.Page ?: throw LayoutError("that isn't a folder")
    val folder = at.item as? Folder ?: throw LayoutError("that isn't a folder")
    val layout = current
    val items = layout.allPages[spot.page].toMutableList()
    items[spot.index] = change(folder).copy(pinned = true)
    return withCurrent(layout.withPage(spot.page, items))
}

/**
 * Moves an item: out of [from], then into [to]. With [strip] false (while a drag holds the item),
 * an extra page the move empties stays, so the page under the finger doesn't vanish; the drop
 * strips it. [nameFor] names a folder two apps make.
 *
 * - Onto an app on a page makes a folder of the two there; onto a folder appends to it.
 * - A folder goes only to pages: not into the dock, a folder, or onto anything.
 * - An app is on home once (any page, folders included) and in the dock once, by what refs draw
 *   as: moving from the drawer or the dock onto home refuses one already there, and so does
 *   moving into the dock. This is a rule of the edits; the parser doesn't refuse duplicates.
 * - A page holds 24, the dock 5, and there are at most 4 pages after home.
 * - Remove takes the item out (a folder with its apps); from the drawer it changes nothing.
 * - Dropping an item where it is returns the file unchanged.
 */
fun LayoutFile.drop(
    from: From,
    to: Target,
    installed: List<AppId> = emptyList(),
    strip: Boolean = true,
    nameFor: (AppRef, AppRef) -> String = { _, _ -> "Folder" },
): Dropped {
    val item: LayoutItem = when (from) {
        is From.At -> { check(from); from.item }
        is From.Drawer -> from.ref
    }
    val spot = (from as? From.At)?.spot
    if (item is Folder) when (to) {
        is Target.Before -> if (to.spot !is Spot.Page) throw LayoutError(if (to.spot is Spot.Dock) "folders don't go in the dock" else "folders don't nest")
        is Target.AtEnd -> when (to.place) {
            Place.Dock -> throw LayoutError("folders don't go in the dock")
            is Place.InFolder -> throw LayoutError("folders don't nest")
            else -> {}
        }
        is Target.Onto -> throw LayoutError("folders don't nest")
        Target.Remove -> {}
    }
    if (spot != null && inPlace(spot, to)) return Dropped(this, spot)
    if (to is Target.Remove && spot == null) return Dropped(this, null)
    val ref = item as? AppRef

    // Out of where it was.
    var layout = current
    var target = to
    if (spot != null) {
        when (spot) {
            is Spot.Page -> {
                layout = layout.withPage(spot.page, layout.allPages[spot.page].filterIndexed { i, _ -> i != spot.index })
                target = target.shifted(Spot.Page(spot.page, spot.index))
            }
            is Spot.Dock -> {
                layout = layout.copy(dock = layout.dock.filterIndexed { i, _ -> i != spot.index })
                target = target.shiftedInDock(spot.index)
            }
            is Spot.InFolder -> {
                val f = layout.allPages[spot.page][spot.index] as Folder
                val left = f.apps.filterIndexed { i, _ -> i != spot.app }
                val items = layout.allPages[spot.page].toMutableList()
                if (left.isEmpty()) {
                    items.removeAt(spot.index)
                    target = target.shifted(spot.folder)
                } else {
                    items[spot.index] = f.copy(apps = left, pinned = true, cover = f.cover.takeUnless { it == f.apps[spot.app] })
                    target = target.shiftedInFolder(spot)
                }
                layout = layout.withPage(spot.page, items)
            }
        }
    }

    // The rules that depend on what is left.
    val fromHome = spot is Spot.Page || spot is Spot.InFolder
    val toHome = when (target) {
        is Target.Before -> (target as Target.Before).spot !is Spot.Dock
        is Target.AtEnd -> (target as Target.AtEnd).place != Place.Dock
        is Target.Onto -> true
        Target.Remove -> false
    }
    if (ref != null && toHome && !fromHome) {
        val onHome = layout.allPages.flatten().flatMap { if (it is Folder) it.apps else listOf(it as AppRef) }
        if (onHome.any { sameApp(it, ref, installed) }) throw LayoutError("it's already on home")
    }
    val toDock = (target as? Target.Before)?.spot is Spot.Dock || (target as? Target.AtEnd)?.place == Place.Dock
    if (ref != null && toDock && spot !is Spot.Dock) {
        if (layout.dock.any { sameApp(it, ref, installed) }) throw LayoutError("it's already in the dock")
        if (layout.dock.size >= LayoutFile.MAX_DOCK) throw LayoutError("the dock is full (${LayoutFile.MAX_DOCK} apps)")
    }

    // Into the target. A folder moved by hand is Sumeet's now (pinned), as any hand edit to a folder.
    val placed = if (item is Folder) item.copy(pinned = true) else item
    var at: Spot? = null
    when (val t = target) {
        Target.Remove -> {}
        is Target.Before -> when (val s = t.spot) {
            is Spot.Page -> { layout = layout.insertOnPage(s.page, s.index, placed); at = s }
            is Spot.Dock -> { layout = layout.copy(dock = layout.dock.toMutableList().apply { add(s.index.coerceIn(0, size), ref!!) }); at = s }
            is Spot.InFolder -> { layout = layout.insertInFolder(s.folder, s.app, ref!!); at = s }
        }
        is Target.AtEnd -> when (val p = t.place) {
            is Place.OnPage -> {
                // A target from a page that is gone (the file changed under the drag) refuses rather than crashing home.
                val size = layout.allPages.getOrNull(p.page)?.size ?: throw LayoutError(HOME_CHANGED)
                at = Spot.Page(p.page, size)
                layout = layout.insertOnPage(p.page, size, placed)
            }
            Place.Dock -> { at = Spot.Dock(layout.dock.size); layout = layout.copy(dock = layout.dock + ref!!) }
            is Place.InFolder -> {
                val f = layout.allPages.getOrNull(p.folder.page)?.getOrNull(p.folder.index) as? Folder ?: throw LayoutError(HOME_CHANGED)
                at = Spot.InFolder(p.folder.page, p.folder.index, f.apps.size)
                layout = layout.insertInFolder(p.folder, f.apps.size, ref!!)
            }
            Place.NewPage -> {
                if (layout.pages.size >= LayoutFile.MAX_PAGES) throw LayoutError("home has ${LayoutFile.MAX_PAGES + 1} pages already")
                at = Spot.Page(layout.pages.size + 1, 0)
                layout = layout.copy(pages = layout.pages + Page(listOf(placed)))
            }
        }
        is Target.Onto -> {
            val items = layout.allPages.getOrNull(t.spot.page)?.toMutableList() ?: throw LayoutError(HOME_CHANGED)
            val under = items.getOrNull(t.spot.index) ?: throw LayoutError(HOME_CHANGED)
            items[t.spot.index] = when (under) {
                is Folder -> under.copy(apps = under.apps + ref!!, pinned = true)
                is AppRef -> Folder(nameFor(under, ref!!), listOf(under, ref), pinned = true)
            }
            layout = layout.withPage(t.spot.page, items)
            at = Spot.InFolder(t.spot.page, t.spot.index, (items[t.spot.index] as Folder).apps.lastIndex)
        }
    }
    if (strip) {
        val before = layout.pages.size
        layout = layout.stripEmptyPages()
        // A page that went may have been before the one the item landed on.
        if (at != null && layout.pages.size != before) at = null
    }
    return Dropped(withCurrent(layout), at)
}

/** Whether dropping the item at [spot] on [to] leaves it where it is. */
private fun LayoutFile.inPlace(spot: Spot, to: Target): Boolean = when (to) {
    is Target.Before -> when {
        to.spot == spot -> true
        spot is Spot.Page && to.spot is Spot.Page -> to.spot.page == spot.page && to.spot.index == spot.index + 1
        spot is Spot.Dock && to.spot is Spot.Dock -> to.spot.index == spot.index + 1
        spot is Spot.InFolder && to.spot is Spot.InFolder -> to.spot.folder == spot.folder && to.spot.app == spot.app + 1
        else -> false
    }
    is Target.AtEnd -> when (val p = to.place) {
        is Place.OnPage -> spot is Spot.Page && spot.page == p.page && spot.index == current.allPages.getOrNull(p.page)?.lastIndex
        Place.Dock -> spot is Spot.Dock && spot.index == current.dock.lastIndex
        is Place.InFolder -> spot is Spot.InFolder && spot.folder == p.folder &&
            spot.app == (itemAt(p.folder) as? Folder)?.apps?.lastIndex
        Place.NewPage -> false
    }
    is Target.Onto -> to.spot == spot || (spot is Spot.InFolder && spot.folder == to.spot)
    Target.Remove -> false
}

/** [this] after the item at [gone] (on a page) left it. */
private fun Target.shifted(gone: Spot.Page): Target {
    fun Spot.Page.after() = if (page == gone.page && index > gone.index) copy(index = index - 1) else this
    return when (this) {
        is Target.Before -> when (val s = spot) {
            is Spot.Page -> Target.Before(s.after())
            is Spot.InFolder -> {
                val f = s.folder.after()
                Target.Before(Spot.InFolder(f.page, f.index, s.app))
            }
            is Spot.Dock -> this
        }
        is Target.AtEnd -> when (val p = place) {
            is Place.InFolder -> Target.AtEnd(Place.InFolder(p.folder.after()))
            else -> this
        }
        is Target.Onto -> Target.Onto(spot.after())
        Target.Remove -> this
    }
}

private fun Target.shiftedInDock(gone: Int): Target =
    if (this is Target.Before && spot is Spot.Dock && spot.index > gone) Target.Before(Spot.Dock(spot.index - 1)) else this

private fun Target.shiftedInFolder(gone: Spot.InFolder): Target =
    if (this is Target.Before && spot is Spot.InFolder && spot.folder == gone.folder && spot.app > gone.app) {
        Target.Before(spot.copy(app = spot.app - 1))
    } else this

private fun Layout.insertOnPage(page: Int, index: Int, item: LayoutItem): Layout {
    val items = allPages.getOrNull(page) ?: throw LayoutError(HOME_CHANGED)
    if (items.size >= LayoutFile.MAX_HOME) throw LayoutError("this page is full (${LayoutFile.MAX_HOME} items)")
    return withPage(page, items.toMutableList().apply { add(index.coerceIn(0, size), item) })
}

private fun Layout.insertInFolder(folder: Spot.Page, index: Int, ref: AppRef): Layout {
    val items = allPages.getOrNull(folder.page)?.toMutableList() ?: throw LayoutError(HOME_CHANGED)
    val f = items.getOrNull(folder.index) as? Folder ?: throw LayoutError(HOME_CHANGED)
    items[folder.index] = f.copy(apps = f.apps.toMutableList().apply { add(index.coerceIn(0, size), ref) }, pinned = true)
    return withPage(folder.page, items)
}

internal fun LayoutFile.withCurrent(layout: Layout) = copy(layouts = layouts.map { if (it.name == active) layout else it })

/** This folder without the apps [gone] picks, pinned and without a cover that went; null when that leaves it empty. */
private fun Folder.without(gone: (AppRef) -> Boolean): Folder? =
    apps.filterNot(gone).takeIf { it.isNotEmpty() }?.let { copy(apps = it, pinned = true, cover = cover?.takeUnless(gone)) }
