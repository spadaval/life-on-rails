package com.spadaval.lor.home

/*
 * Phone edits to the layout file (launcher polish 1.1, and 4.1 for pages, places and dragging),
 * on schema version 2's grid: where version 1 had pages, an edit addresses an `apps` block of a
 * stack panel ([BlockAt]).
 * Each is a pure function from the parsed file to the edited one, throwing a [LayoutError] whose
 * message says why it refuses (a limit, a duplicate, nothing to do). No Android here, so the JVM
 * tests reach all of it.
 *
 * Edits act on the active layout, except hide, which acts on every layout. A folder an edit
 * touches becomes `pinned` (it is Sumeet's now); a folder an edit empties is removed, and so is
 * an extra page's panel (a stack of only `apps`) the edit emptied or made; one that was
 * already empty stays, and the start cell's panel stays even when empty ([Layout.stripEmptyPanels]).
 * "On home" means in any `apps` block of the grid.
 *
 * Which app a ref names is worked out as home draws it (R3 of Increment 1's review): a package
 * name draws as the first activity of the package in its profile, in label order, so it names that
 * activity only. A ref names an app of its own profile only (`work:`, lor-72c3), so an app and its
 * work twin are two apps. The edits that ask take the installed apps of both profiles in label
 * order; an app the list doesn't hold is taken as installed after all of them.
 *
 * Places in the file are [Spot]s, the file's indices (not the drawn order: home skips a ref that
 * isn't installed but keeps its place). An edit by place also takes what home drew there, and
 * refuses when the file holds something else there now (an agent or adb wrote while home was on
 * screen), so it never moves or removes the wrong thing.
 */

/** A place in the active layout: an item of an `apps` block, an app in a folder. */
sealed interface Spot {
    /** The item at [index] of the `apps` block [at]. */
    data class Apps(val at: BlockAt, val index: Int) : Spot
    /** The app at [app] in the folder at `Apps(at, index)`. */
    data class InFolder(val at: BlockAt, val index: Int, val app: Int) : Spot {
        val folder: Apps get() = Apps(at, index)
    }
}

/** Where a moved item comes from: a place in the file, with what home drew there, or the drawer. */
sealed interface From {
    data class At(val spot: Spot, val item: LayoutItem) : From
    data class Drawer(val app: AppId, val ref: AppRef) : From
}

/** Where a moved item goes. */
sealed interface Target {
    /** In front of the item at [spot] (an `apps` block or a folder). */
    data class Before(val spot: Spot) : Target
    data class AtEnd(val place: Place) : Target
    /** Onto the app or folder at [spot]: the two make a folder, or the folder takes it. */
    data class Onto(val spot: Spot.Apps) : Target
    data object Remove : Target
}

/** A list an item can be appended to. */
sealed interface Place {
    /** The `apps` block [at]. */
    data class InApps(val at: BlockAt) : Place
    data class InFolder(val folder: Spot.Apps) : Place
    /** A new stack panel after the last in the start row, made by the drop (launcher polish 4.5). */
    data object NewPanel : Place
}

/** Where [LayoutFile.current] holds an app: on home itself (any `apps` block), in a folder. */
data class Places(val home: Boolean, val folder: String?)

/** A drop's result: the edited file, and where the moved item is now (null when it was removed). */
data class Dropped(val file: LayoutFile, val at: Spot?)

/** The activity [this] draws as: the first of [installed] it matches, in their (label) order. */
fun AppRef.resolve(installed: List<AppId>): AppId? = installed.firstOrNull { matches(it) }

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
    val items = layout.allItems
    return Places(
        home = items.any { it is AppRef && it.names(app, installed) },
        folder = items.filterIsInstance<Folder>().firstOrNull { f -> f.apps.any { it.names(app, installed) } }?.name,
    )
}

/** What [spot] holds in the active layout, or null when nothing is there. */
fun LayoutFile.itemAt(spot: Spot): LayoutItem? {
    val layout = current
    return when (spot) {
        is Spot.Apps -> layout.items(spot.at)?.getOrNull(spot.index)
        is Spot.InFolder -> (itemAt(spot.folder) as? Folder)?.apps?.getOrNull(spot.app)
    }
}

private fun LayoutFile.check(from: From.At) {
    if (itemAt(from.spot) != from.item) throw LayoutError(HOME_CHANGED)
}

const val HOME_CHANGED = "Home changed; try again"

/**
 * Where [app] is in a folder on home when it isn't on home itself: the first folder holding it, as
 * [placesOf] names it. Add to home takes it out of there (lor-83uq).
 */
fun LayoutFile.folderSpotOf(app: AppId, installed: List<AppId> = emptyList()): Spot.InFolder? {
    val layout = current
    if (layout.allItems.any { it is AppRef && it.names(app, installed) }) return null
    for (at in layout.appsBlocks) {
        layout.items(at)!!.forEachIndexed { i, item ->
            val inside = (item as? Folder)?.apps?.indexOfFirst { it.names(app, installed) } ?: -1
            if (inside >= 0) return Spot.InFolder(at, i, inside)
        }
    }
    return null
}

/**
 * What a drag of [drawer]'s app from the drawer writes, [work] being the file once it ended at [at]
 * (null: removed) and [original] the file the drag began on. A step over home takes an app in a
 * folder out of it, so when the drag ends off home (on Remove) the result is made from [original]
 * instead, and the folder keeps the app (lor-83uq's review, R1).
 */
fun drawerDragResult(original: LayoutFile, work: LayoutFile, drawer: From.Drawer, at: Spot?, installed: List<AppId> = emptyList()): Dropped =
    if (at == null && original.folderSpotOf(drawer.app, installed) != null) Dropped(original, null) else Dropped(work, at)

/**
 * Adds [ref] (written for [app]) to the end of the first page ([Layout.pages]) with room; when
 * every page is full, to a new panel while the start row has room ([Layout.roomForPanel]). An app
 * in a folder on home moves out of it, its ref as the folder wrote it, and a folder that leaves
 * empty goes (lor-83uq).
 */
fun LayoutFile.addToHome(app: AppId, ref: AppRef, installed: List<AppId> = emptyList()): LayoutFile {
    if (placesOf(app, installed).home) throw LayoutError("it's already on home")
    val inFolder = folderSpotOf(app, installed)
    val base = inFolder?.let { drop(From.At(it, itemAt(it)!!), Target.Remove, installed, strip = false).file } ?: this
    val adds = inFolder?.let { itemAt(it) as AppRef } ?: ref
    val layout = base.current
    val page = layout.pages.firstOrNull { layout.items(it)!!.size < LayoutFile.MAX_HOME }
    val added = when {
        page != null -> layout.withItems(page, layout.items(page)!! + adds)
        layout.roomForPanel -> layout.withNewPanel(listOf(adds)).first
        else -> throw LayoutError("home is full (${LayoutFile.MAX_APPS_PANELS} pages of ${LayoutFile.MAX_HOME})")
    }
    return withCurrent(added.stripEmptyPanels(current))
}

/** Removes the refs in every `apps` block (not in folders) that name [app]. */
fun LayoutFile.removeFromHome(app: AppId, installed: List<AppId> = emptyList()): LayoutFile {
    if (!placesOf(app, installed).home) throw LayoutError("it isn't on home")
    return withCurrent(current.mapApps { items -> items.filterNot { it is AppRef && it.names(app, installed) } }.stripEmptyPanels(current))
}

/** Removes [app] from the first folder (by place, not name) that holds it. */
fun LayoutFile.removeFromFolder(app: AppId, installed: List<AppId> = emptyList()): LayoutFile {
    val layout = current
    layout.appsBlocks.forEach { block ->
        layout.items(block)!!.forEachIndexed { i, item ->
            if (item is Folder) {
                val a = item.apps.indexOfFirst { it.names(app, installed) }
                if (a >= 0) return removeAt(From.At(Spot.InFolder(block, i, a), item.apps[a]))
            }
        }
    }
    throw LayoutError("it isn't in a folder")
}

/** Removes the item [from] names (an app, or a folder with its apps) from where it is. */
fun LayoutFile.removeAt(from: From.At): LayoutFile = drop(from, Target.Remove).file

/**
 * Hides every activity of [packageName] in one profile (the work profile when [work]): adds it to
 * `hidden` and removes, in every layout, each ref to that package in that profile from every
 * `apps` block and folders.
 */
fun LayoutFile.hide(packageName: String, work: Boolean = false): LayoutFile {
    if (hidden.any { it.packageName == packageName && it.work == work && it.className == null }) throw LayoutError("it's already hidden")
    val gone = { ref: AppRef -> ref.packageName == packageName && ref.work == work }
    val layouts = layouts.map { layout ->
        layout.mapApps { items ->
            items.mapNotNull { item ->
                when (item) {
                    is AppRef -> item.takeUnless(gone)
                    is Folder -> if (item.apps.any(gone)) item.without(gone) else item
                }
            }
        }.stripEmptyPanels(layout)
    }
    return copy(layouts = layouts, hidden = hidden + AppRef(packageName, work = work))
}

/** Takes [packageName] in one profile out of `hidden`, nothing else: what hiding removed stays removed. */
fun LayoutFile.unhide(packageName: String, work: Boolean = false): LayoutFile {
    val named = { ref: AppRef -> ref.packageName == packageName && ref.work == work }
    if (hidden.none(named)) throw LayoutError("it isn't hidden")
    return copy(hidden = hidden.filterNot(named))
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

/**
 * Adds [apps], each with the ref Add writes for it, to the end of the folder [at] holds, in order
 * (lor-gnk0: the open folder's +). An app is on home once, so one already elsewhere on home (in an
 * `apps` block or another folder) moves into this folder; a folder that leaves empty goes, and so
 * does an extra page's panel. Refuses an app this folder already holds.
 * The result's place is the folder's, which moves up by the items that left ahead of it.
 */
fun LayoutFile.addToFolder(at: From.At, apps: List<Pair<AppId, AppRef>>, installed: List<AppId> = emptyList()): Dropped {
    check(at)
    val spot = at.spot as? Spot.Apps ?: throw LayoutError("that isn't a folder")
    val folder = at.item as? Folder ?: throw LayoutError("that isn't a folder")
    val adding = apps.distinctBy { it.first }
    if (adding.isEmpty()) throw LayoutError("no apps were picked")
    if (adding.any { (app, _) -> folder.apps.any { it.names(app, installed) } }) throw LayoutError("it's already in ${folder.name}")
    val gone = { ref: AppRef -> adding.any { (app, _) -> ref.names(app, installed) } }
    var layout = current
    var index = spot.index
    layout.appsBlocks.forEach { block ->
        val items = layout.items(block)!!.mapIndexedNotNull { i, item ->
            if (block == spot.at && i == spot.index) return@mapIndexedNotNull folder.copy(apps = folder.apps + adding.map { it.second }, pinned = true)
            val kept = when (item) {
                is AppRef -> item.takeUnless(gone)
                is Folder -> if (item.apps.any(gone)) item.without(gone) else item
            }
            if (kept == null && block == spot.at && i < spot.index) index--
            kept
        }
        layout = layout.withItems(block, items)
    }
    // Places are cells, so a panel that goes moves no other item's place.
    return Dropped(withCurrent(layout.stripEmptyPanels(current)), spot.copy(index = index))
}

private fun LayoutFile.changeFolder(at: From.At, change: (Folder) -> Folder): LayoutFile {
    check(at)
    val spot = at.spot as? Spot.Apps ?: throw LayoutError("that isn't a folder")
    val folder = at.item as? Folder ?: throw LayoutError("that isn't a folder")
    val layout = current
    val items = layout.items(spot.at)!!.toMutableList()
    items[spot.index] = change(folder).copy(pinned = true)
    return withCurrent(layout.withItems(spot.at, items))
}

/**
 * Moves an item: out of [from], then into [to]. With [strip] false (while a drag holds the item),
 * an extra page's panel the move empties stays, so the page under the finger doesn't vanish; the
 * drop strips it. [nameFor] names a folder two apps make.
 *
 * - Onto an app in an `apps` block makes a folder of the two there; onto a folder appends to it.
 * - A folder goes only to `apps` blocks: not into a folder, or onto anything.
 * - An app is on home once (any page, folders included), by what refs draw as: moving from the
 *   drawer onto home refuses one already there. From the drawer, one in a folder on home (and not
 *   on home itself) is moved out of that folder instead (lor-83uq). This is a rule of the edits;
 *   the parser doesn't refuse duplicates.
 * - An `apps` block holds 24, and the start row at most 5 stacks with an `apps` block.
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
    // From the drawer onto home, an app in a folder there moves out of it (lor-83uq).
    if (from is From.Drawer && to.toHome) folderSpotOf(from.app, installed)?.let { return drop(From.At(it, itemAt(it)!!), to, installed, strip, nameFor) }
    val spot = (from as? From.At)?.spot
    if (item is Folder) when (to) {
        is Target.Before -> if (to.spot !is Spot.Apps) throw LayoutError("folders don't nest")
        is Target.AtEnd -> if (to.place is Place.InFolder) throw LayoutError("folders don't nest")
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
            is Spot.Apps -> {
                layout = layout.withItems(spot.at, layout.items(spot.at)!!.filterIndexed { i, _ -> i != spot.index })
                target = target.shifted(spot)
            }
            is Spot.InFolder -> {
                val items = layout.items(spot.at)!!.toMutableList()
                val f = items[spot.index] as Folder
                val left = f.apps.filterIndexed { i, _ -> i != spot.app }
                if (left.isEmpty()) {
                    items.removeAt(spot.index)
                    target = target.shifted(spot.folder)
                } else {
                    items[spot.index] = f.copy(apps = left, pinned = true, cover = f.cover.takeUnless { it == f.apps[spot.app] })
                    target = target.shiftedInFolder(spot)
                }
                layout = layout.withItems(spot.at, items)
            }
        }
    }

    // The rule that depends on what is left.
    if (ref != null && target.toHome && spot == null) {
        val onHome = layout.allItems.flatMap { if (it is Folder) it.apps else listOf(it as AppRef) }
        if (onHome.any { sameApp(it, ref, installed) }) throw LayoutError("it's already on home")
    }

    // Into the target. A folder moved by hand is Sumeet's now (pinned), as any hand edit to a folder.
    val placed = if (item is Folder) item.copy(pinned = true) else item
    var at: Spot? = null
    when (val t = target) {
        Target.Remove -> {}
        is Target.Before -> when (val s = t.spot) {
            is Spot.Apps -> { layout = layout.insertInApps(s.at, s.index, placed); at = s }
            is Spot.InFolder -> { layout = layout.insertInFolder(s.folder, s.app, ref!!); at = s }
        }
        is Target.AtEnd -> when (val p = t.place) {
            is Place.InApps -> {
                // A target from a block that is gone (the file changed under the drag) refuses rather than crashing home.
                val size = layout.items(p.at)?.size ?: throw LayoutError(HOME_CHANGED)
                at = Spot.Apps(p.at, size)
                layout = layout.insertInApps(p.at, size, placed)
            }
            is Place.InFolder -> {
                val f = layout.items(p.folder.at)?.getOrNull(p.folder.index) as? Folder ?: throw LayoutError(HOME_CHANGED)
                at = Spot.InFolder(p.folder.at, p.folder.index, f.apps.size)
                layout = layout.insertInFolder(p.folder, f.apps.size, ref!!)
            }
            Place.NewPanel -> {
                if (!layout.roomForPanel) throw LayoutError("home has ${LayoutFile.MAX_APPS_PANELS} pages already")
                val (made, block) = layout.withNewPanel(listOf(placed))
                at = Spot.Apps(block, 0)
                layout = made
            }
        }
        is Target.Onto -> {
            val items = layout.items(t.spot.at)?.toMutableList() ?: throw LayoutError(HOME_CHANGED)
            val under = items.getOrNull(t.spot.index) ?: throw LayoutError(HOME_CHANGED)
            items[t.spot.index] = when (under) {
                is Folder -> under.copy(apps = under.apps + ref!!, pinned = true)
                is AppRef -> Folder(nameFor(under, ref!!), listOf(under, ref), pinned = true)
            }
            layout = layout.withItems(t.spot.at, items)
            at = Spot.InFolder(t.spot.at, t.spot.index, (items[t.spot.index] as Folder).apps.lastIndex)
        }
    }
    // Places are cells, so a panel that goes moves no other item's place.
    if (strip) layout = layout.stripEmptyPanels(current)
    return Dropped(withCurrent(layout), at)
}

/** Whether this target is on home: anything but Remove. */
private val Target.toHome: Boolean get() = this != Target.Remove

/** Whether dropping the item at [spot] on [to] leaves it where it is. */
private fun LayoutFile.inPlace(spot: Spot, to: Target): Boolean = when (to) {
    is Target.Before -> when {
        to.spot == spot -> true
        spot is Spot.Apps && to.spot is Spot.Apps -> to.spot.at == spot.at && to.spot.index == spot.index + 1
        spot is Spot.InFolder && to.spot is Spot.InFolder -> to.spot.folder == spot.folder && to.spot.app == spot.app + 1
        else -> false
    }
    is Target.AtEnd -> when (val p = to.place) {
        is Place.InApps -> spot is Spot.Apps && spot.at == p.at && spot.index == current.items(p.at)?.lastIndex
        is Place.InFolder -> spot is Spot.InFolder && spot.folder == p.folder &&
            spot.app == (itemAt(p.folder) as? Folder)?.apps?.lastIndex
        Place.NewPanel -> false
    }
    is Target.Onto -> to.spot == spot || (spot is Spot.InFolder && spot.folder == to.spot)
    Target.Remove -> false
}

/** [this] after the item at [gone] (in an `apps` block) left it. */
private fun Target.shifted(gone: Spot.Apps): Target {
    fun Spot.Apps.after() = if (at == gone.at && index > gone.index) copy(index = index - 1) else this
    return when (this) {
        is Target.Before -> when (val s = spot) {
            is Spot.Apps -> Target.Before(s.after())
            is Spot.InFolder -> {
                val f = s.folder.after()
                Target.Before(Spot.InFolder(f.at, f.index, s.app))
            }
        }
        is Target.AtEnd -> when (val p = place) {
            is Place.InFolder -> Target.AtEnd(Place.InFolder(p.folder.after()))
            else -> this
        }
        is Target.Onto -> Target.Onto(spot.after())
        Target.Remove -> this
    }
}

private fun Target.shiftedInFolder(gone: Spot.InFolder): Target =
    if (this is Target.Before && spot is Spot.InFolder && spot.folder == gone.folder && spot.app > gone.app) {
        Target.Before(spot.copy(app = spot.app - 1))
    } else this

private fun Layout.insertInApps(at: BlockAt, index: Int, item: LayoutItem): Layout {
    val items = items(at) ?: throw LayoutError(HOME_CHANGED)
    if (items.size >= LayoutFile.MAX_HOME) throw LayoutError("this page is full (${LayoutFile.MAX_HOME} items)")
    return withItems(at, items.toMutableList().apply { add(index.coerceIn(0, size), item) })
}

private fun Layout.insertInFolder(folder: Spot.Apps, index: Int, ref: AppRef): Layout {
    val items = items(folder.at)?.toMutableList() ?: throw LayoutError(HOME_CHANGED)
    val f = items.getOrNull(folder.index) as? Folder ?: throw LayoutError(HOME_CHANGED)
    items[folder.index] = f.copy(apps = f.apps.toMutableList().apply { add(index.coerceIn(0, size), ref) }, pinned = true)
    return withItems(folder.at, items)
}

internal fun LayoutFile.withCurrent(layout: Layout) = copy(layouts = layouts.map { if (it.name == active) layout else it })

/** This folder without the apps [gone] picks, pinned and without a cover that went; null when that leaves it empty. */
private fun Folder.without(gone: (AppRef) -> Boolean): Folder? =
    apps.filterNot(gone).takeIf { it.isNotEmpty() }?.let { copy(apps = it, pinned = true, cover = cover?.takeUnless(gone)) }
