package com.spadaval.lor.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Pages, covers, edits by place and the drop edit (launcher polish 4.1). */
class HomeEditTest {
    private fun ref(s: String) = AppRef.parse(s)!!
    private fun app(s: String) = ref(s).let { AppId(it.packageName, it.className ?: "${it.packageName}.Main") }

    private fun file(vararg pages: List<LayoutItem>, dock: List<AppRef> = emptyList()) = LayoutFile(
        "a",
        listOf(Layout.grid("a", pages[0], dock, pages.drop(1))),
    )

    private fun refused(edit: () -> Any): String = try {
        edit()
        fail("not refused")
        ""
    } catch (e: LayoutError) {
        e.message!!
    }

    private fun roundTrips(f: LayoutFile) = assertEquals(f, LayoutFile.parse(f.toJson()))
    private fun at(f: LayoutFile, spot: Spot) = From.At(spot, f.itemAt(spot)!!)
    private val LayoutFile.pages get() = current.allPages.map { p -> p.map { (it as? Folder)?.let { f -> "[${f.name}: ${f.apps.joinToString()}]" } ?: it.toString() } }

    @Test
    fun pagesAndCoverParseRefuseAndRoundTrip() {
        // Version 1's pages are stacks of apps and dock to home's right (lor-euec).
        val text = """{"version": 2, "active": "a", "layouts": [{"name": "a", "start": {"narrow": [1, 0]}, "panels": [
            {"at": [1, 0], "type": "stack", "blocks": [{"type": "clock"}, {"type": "apps", "items": ["a.a"]}, {"type": "dock"}]},
            {"at": [2, 0], "type": "stack", "blocks": [{"type": "apps", "items": ["b.b", {"folder": "F", "apps": ["c.c", "d.d/.X"], "cover": "d.d/d.d.X"}]}, {"type": "dock"}]},
            {"at": [3, 0], "type": "stack", "blocks": [{"type": "apps"}, {"type": "dock"}]}]}]}"""
        val f = LayoutFile.parse(text)
        assertEquals(listOf(listOf("a.a"), listOf("b.b", "[F: c.c, d.d/d.d.X]"), emptyList()), f.pages)
        assertEquals(AppRef("d.d", "d.d.X"), (f.current.items(page(1))!![1] as Folder).cover)
        roundTrips(f)
        fun stack(at: Int, items: String) = """{"at": [$at, 0], "type": "stack", "blocks": [{"type": "apps", "items": [$items]}]}"""
        fun bad(panels: String) = refused { LayoutFile.parse("""{"version": 2, "active": "a", "layouts": [{"name": "a", "start": {"narrow": [0, 0]}, "panels": [$panels]}]}""") }
        assertEquals("layouts[0].panels: row 0 holds 6 stacks with an apps block, at most 5", bad((0..5).joinToString { stack(it, "") }))
        val many = (1..25).joinToString { "\"p.p$it\"" }
        assertEquals("layouts[0].panels[1].blocks[0].items: at most 24 items, got 25", bad(stack(0, "") + ", " + stack(1, many)))
        assertEquals("layouts[0].panels[0].blocks[0]: unknown field \"home\" (allowed: type, items)",
            bad("""{"at": [0, 0], "type": "stack", "blocks": [{"type": "apps", "home": []}]}"""))
        assertEquals("layouts[0].panels[0].blocks[0].items[0].cover: \"x.x\" is not one of the folder's apps", bad(stack(0, """{"folder": "F", "apps": ["c.c"], "cover": "x.x"}""")))
        val plain = file(listOf(ref("a.a"), Folder("F", listOf(ref("b.b")))))
        assertFalse("neither field is written when unset", plain.toJson().contains("pages") || plain.toJson().contains("cover"))
    }

    @Test
    fun aPackageRefNamesTheActivityItDrawsAs() {
        // R3: p has Alpha (p/.A) and Beta (p/.B); home holds p, which draws as Alpha.
        val a = AppId("p.p", "p.p.A")
        val b = AppId("p.p", "p.p.B")
        val installed = listOf(a, b)
        val f = file(listOf(ref("x.x"), ref("p.p")))
        assertEquals(Places(home = true, dock = false, folder = null), f.placesOf(a, installed))
        assertEquals(Places(home = false, dock = false, folder = null), f.placesOf(b, installed))
        assertEquals("it isn't on home", refused { f.removeFromHome(b, installed) })
        val added = f.addToHome(b, AppRef("p.p", "p.p.B"), installed)
        assertEquals(listOf(listOf("x.x", "p.p", "p.p/p.p.B")), added.pages)
        // The on-home-once rule of a drop doesn't count p as Beta either.
        val dropped = f.drop(From.Drawer(b, AppRef("p.p", "p.p.B")), Target.AtEnd(Place.InApps(page(0))), installed).file
        assertEquals(added, dropped)
        assertEquals("it's already on home", refused { f.drop(From.Drawer(a, AppRef("p.p", "p.p.A")), Target.AtEnd(Place.InApps(page(0))), installed) })
    }

    @Test
    fun foldersAreEditedByPlace() {
        // R4: two folders with one name.
        val f = file(listOf(Folder("Google", listOf(ref("a.a"), ref("b.b"))), Folder("Google", listOf(ref("c.c"), ref("d.d")))))
        val e = f.removeAt(at(f, Spot.InFolder(page(0), 1, 0)))
        assertEquals(listOf(listOf("[Google: a.a, b.b]", "[Google: d.d]")), e.pages)
        assertFalse((e.current.home[0] as Folder).pinned)
        assertTrue((e.current.home[1] as Folder).pinned)
    }

    @Test
    fun anEditByPlaceRefusesWhenTheFileChangedThere() {
        val f = file(listOf(ref("a.a"), ref("b.b")))
        val drawn = From.At(Spot.Apps(page(0), 0), ref("a.a"))
        val changed = file(listOf(ref("b.b"), ref("a.a")))
        assertEquals(HOME_CHANGED, refused { changed.removeAt(drawn) })
        assertEquals(HOME_CHANGED, refused { changed.drop(drawn, Target.AtEnd(Place.Dock)) })
        assertEquals(listOf(listOf("b.b")), f.removeAt(drawn).pages)
    }

    @Test
    fun theSectionCarryKeepsAPackagesOtherActivities() {
        val a = AppId("p.p", "p.p.A")
        val b = AppId("p.p", "p.p.B")
        val f = LayoutFile("a", listOf(Layout.grid("a", emptyList(), emptyList())), drawer = Drawer(sections = listOf(DrawerSection("Mine", apps = listOf(ref("p.p"))))))
        val moved = f.moveToSection(a, AppRef("p.p", "p.p.A"), Category.TOOLS, "Work", listOf(a, b))
        assertEquals(listOf(AppRef("p.p", "p.p.B")), moved.drawer.sections.first { it.name == "Mine" }.apps)
        assertEquals("Work", moved.drawer.sectionOf(a, Category.TOOLS))
        assertEquals("Mine", moved.drawer.sectionOf(b, Category.TOOLS))
        roundTrips(moved)
    }

    @Test
    fun dropsMoveReorderAndMakeFolders() {
        val f = file(listOf(ref("a.a"), ref("b.b"), ref("c.c")), listOf(ref("e.e")), dock = listOf(ref("d.d")))
        // Reorder on a page, both ways.
        assertEquals(listOf("b.b", "c.c", "a.a"), f.drop(at(f, Spot.Apps(page(0), 0)), Target.AtEnd(Place.InApps(page(0)))).file.pages[0])
        val moved = f.drop(at(f, Spot.Apps(page(0), 2)), Target.Before(Spot.Apps(page(0), 0)))
        assertEquals(listOf("c.c", "a.a", "b.b"), moved.file.pages[0])
        assertEquals(Spot.Apps(page(0), 0), moved.at)
        assertEquals(listOf("b.b", "a.a", "c.c"), f.drop(at(f, Spot.Apps(page(0), 0)), Target.Before(Spot.Apps(page(0), 2))).file.pages[0])
        // To another page, and the dock both ways.
        assertEquals(listOf(listOf("b.b", "c.c"), listOf("a.a", "e.e")), f.drop(at(f, Spot.Apps(page(0), 0)), Target.Before(Spot.Apps(page(1), 0))).file.pages)
        val docked = f.drop(at(f, Spot.Apps(page(0), 1)), Target.Before(Spot.Dock(0))).file
        assertEquals(listOf(ref("b.b"), ref("d.d")), docked.current.dock)
        assertEquals(listOf("a.a", "b.b", "c.c", "d.d"), f.drop(at(f, Spot.Dock(0)), Target.AtEnd(Place.InApps(page(0)))).file.pages[0])
        // Onto an app makes a folder there; onto a folder appends to it.
        val made = f.drop(at(f, Spot.Apps(page(0), 2)), Target.Onto(Spot.Apps(page(0), 0)), nameFor = { _, _ -> "Media" })
        assertEquals(listOf("[Media: a.a, c.c]", "b.b"), made.file.pages[0])
        assertTrue((made.file.current.home[0] as Folder).pinned)
        assertEquals(Spot.InFolder(page(0), 0, 1), made.at)
        val filled = made.file.drop(at(made.file, Spot.Dock(0)), Target.Onto(Spot.Apps(page(0), 0))).file
        assertEquals(listOf("[Media: a.a, c.c, d.d]", "b.b"), filled.pages[0])
        // In and out of a folder, and reorder inside it.
        assertEquals(listOf("[Media: c.c, a.a]", "b.b"), made.file.drop(at(made.file, Spot.InFolder(page(0), 0, 0)), Target.AtEnd(Place.InFolder(Spot.Apps(page(0), 0)))).file.pages[0])
        val out = made.file.drop(at(made.file, Spot.InFolder(page(0), 0, 0)), Target.AtEnd(Place.InApps(page(0)))).file
        assertEquals(listOf("[Media: c.c]", "b.b", "a.a"), out.pages[0])
        assertEquals("the last app out takes the folder", listOf("b.b", "a.a", "c.c"), out.drop(at(out, Spot.InFolder(page(0), 0, 0)), Target.AtEnd(Place.InApps(page(0)))).file.pages[0])
        // A new page; Remove; a drop in place changes nothing.
        assertEquals(listOf(listOf("b.b", "c.c"), listOf("e.e"), listOf("a.a")), f.drop(at(f, Spot.Apps(page(0), 0)), Target.AtEnd(Place.NewPanel)).file.pages)
        assertEquals(listOf("a.a", "c.c"), f.drop(at(f, Spot.Apps(page(0), 1)), Target.Remove).file.pages[0])
        assertSame(f, f.drop(at(f, Spot.Apps(page(0), 1)), Target.Before(Spot.Apps(page(0), 2))).file)
        assertSame(f, f.drop(at(f, Spot.Apps(page(0), 1)), Target.Before(Spot.Apps(page(0), 1))).file)
        assertSame(f, f.drop(From.Drawer(app("z.z"), ref("z.z")), Target.Remove).file)
        roundTrips(made.file)
    }

    @Test
    fun dropsRefuse() {
        val folder = Folder("F", listOf(ref("x.x")))
        val f = file(listOf(ref("a.a"), folder), dock = listOf(ref("d.d")))
        assertEquals("folders don't go in the dock", refused { f.drop(at(f, Spot.Apps(page(0), 1)), Target.AtEnd(Place.Dock)) })
        assertEquals("folders don't nest", refused { f.drop(at(f, Spot.Apps(page(0), 1)), Target.Onto(Spot.Apps(page(0), 0))) })
        assertEquals("folders don't nest", refused { f.drop(at(f, Spot.Apps(page(0), 1)), Target.Before(Spot.InFolder(page(0), 1, 0))) })
        assertEquals("it's already on home", refused { f.drop(From.Drawer(app("x.x"), ref("x.x")), Target.AtEnd(Place.InApps(page(0)))) })
        assertEquals("it's already in the dock", refused { f.drop(From.Drawer(app("d.d"), ref("d.d")), Target.AtEnd(Place.Dock)) })
        val fullDock = file(listOf(ref("a.a")), dock = (1..5).map { ref("d.d$it") })
        assertEquals("the dock is full (5 apps)", refused { fullDock.drop(at(fullDock, Spot.Apps(page(0), 0)), Target.AtEnd(Place.Dock)) })
        val fullPage = file((1..24).map { ref("p.p$it") }, listOf(ref("a.a")))
        assertEquals("this page is full (24 items)", refused { fullPage.drop(at(fullPage, Spot.Apps(page(1), 0)), Target.AtEnd(Place.InApps(page(0)))) })
        val fivePages = file(listOf(ref("a.a"), ref("b.b")), listOf(ref("c.c")), listOf(ref("d.d")), listOf(ref("e.e")), listOf(ref("f.f")))
        assertEquals("home has 5 pages already", refused { fivePages.drop(at(fivePages, Spot.Apps(page(0), 0)), Target.AtEnd(Place.NewPanel)) })
    }

    @Test
    fun emptiedFoldersAndExtraPagesGoButHomeStays() {
        val f = file(listOf(ref("a.a")), listOf(ref("b.b")), listOf(Folder("F", listOf(ref("c.c")))))
        assertEquals(listOf(listOf("a.a", "b.b"), listOf("[F: c.c]")), f.drop(at(f, Spot.Apps(page(1), 0)), Target.AtEnd(Place.InApps(page(0)))).file.pages)
        val held = f.drop(at(f, Spot.Apps(page(1), 0)), Target.AtEnd(Place.InApps(page(0))), strip = false).file
        assertEquals("a drag keeps the page under the finger", listOf(listOf("a.a", "b.b"), emptyList(), listOf("[F: c.c]")), held.pages)
        assertEquals(listOf(listOf("a.a", "c.c"), listOf("b.b")), f.drop(at(f, Spot.InFolder(page(2), 0, 0)), Target.AtEnd(Place.InApps(page(0)))).file.pages)
        assertEquals("home's page stays when emptied", listOf(emptyList(), listOf("b.b"), listOf("[F: c.c]")), f.removeFromHome(app("a.a")).pages)
        assertEquals(listOf(listOf("a.a"), listOf("[F: c.c]")), f.removeFromHome(app("b.b")).pages)
        assertEquals(listOf(listOf("a.a"), listOf("b.b")), f.hide("c.c").pages)
    }

    @Test
    fun coversFollowTheirApp() {
        val f = file(listOf(Folder("F", listOf(ref("a.a"), ref("b.b")))))
        val folder = at(f, Spot.Apps(page(0), 0))
        val covered = f.setCover(folder, ref("b.b"))
        assertEquals(ref("b.b"), (covered.current.home[0] as Folder).cover)
        assertTrue((covered.current.home[0] as Folder).pinned)
        assertEquals("\"z.z\" isn't in F", refused { f.setCover(folder, ref("z.z")) })
        assertNull((covered.removeAt(at(covered, Spot.InFolder(page(0), 0, 1))).current.home[0] as Folder).cover)
        assertNull((covered.hide("b.b").current.home[0] as Folder).cover)
        assertEquals(ref("b.b"), (covered.hide("a.a").current.home[0] as Folder).cover)
        assertEquals("Work", (covered.renameFolder(at(covered, Spot.Apps(page(0), 0)), " Work ").current.home[0] as Folder).name)
        assertEquals("a folder needs a name", refused { f.renameFolder(folder, " ") })
        roundTrips(covered)
    }

    @Test
    fun addToHomeFillsPagesThenMakesOne() {
        var f = file((1..24).map { ref("p.p$it") })
        f = f.addToHome(app("a.a"), ref("a.a"))
        assertEquals(listOf("a.a"), f.pages[1])
        f = file((1..24).map { ref("p.p$it") }, (1..24).map { ref("q.q$it") }, emptyList())
        assertEquals("the first page with room", listOf("b.b"), f.addToHome(app("b.b"), ref("b.b")).pages[2])
        val full = file(*(0..4).map { p -> (1..24).map { ref("p$p.p$it") } }.toTypedArray())
        assertEquals("home is full (5 pages of 24)", refused { full.addToHome(app("a.a"), ref("a.a")) })
    }

    @Test
    fun aNewFolderIsNamedForTheSharedCategory() {
        assertEquals("Media", folderName(Category.MEDIA, Category.MEDIA))
        assertEquals("Folder", folderName(Category.MEDIA, Category.TOOLS))
        assertEquals("Folder", folderName(null, null))
    }

    @Test
    fun aFolderMovedByHandIsPinnedAndAStaleTargetRefuses() {
        // From the increment's review: R6 and R5.
        val f = file(listOf(ref("a.a"), Folder("F", listOf(ref("x.x")))), listOf(ref("b.b")))
        val moved = f.drop(at(f, Spot.Apps(page(0), 1)), Target.Before(Spot.Apps(page(0), 0))).file
        assertTrue((moved.current.home[0] as Folder).pinned)
        assertTrue((f.drop(at(f, Spot.Apps(page(0), 1)), Target.AtEnd(Place.InApps(page(1)))).file.current.items(page(1))!![1] as Folder).pinned)
        assertTrue((f.drop(at(f, Spot.Apps(page(0), 1)), Target.AtEnd(Place.NewPanel)).file.current.items(page(2))!![0] as Folder).pinned)
        assertFalse("in place, nothing changes", (f.drop(at(f, Spot.Apps(page(0), 1)), Target.AtEnd(Place.InApps(page(0)))).file.current.home[1] as Folder).pinned)
        assertEquals(HOME_CHANGED, refused { f.drop(at(f, Spot.Apps(page(0), 0)), Target.AtEnd(Place.InApps(page(3)))) })
        assertEquals(listOf(ref("a.a")), f.drop(at(f, Spot.Apps(page(0), 0)), Target.Before(Spot.Dock(7))).file.current.dock)
    }

    @Test
    fun anUnpinnedFolderStepsTwiceWithTheFromTheFirstStepGave() {
        // From the review of the review's fixes (R1): the first step pins the folder in the copy, and the second checks against it.
        val f = file(listOf(ref("a.a"), ref("b.b"), Folder("F", listOf(ref("x.x")))))
        val first = f.drop(at(f, Spot.Apps(page(0), 2)), Target.Before(Spot.Apps(page(0), 0)), strip = false)
        val from = From.At(first.at!!, first.file.itemAt(first.at!!)!!)
        val second = first.file.drop(from, Target.Before(Spot.Apps(page(0), 2)), strip = false)
        assertEquals(listOf("a.a", "[F: x.x]", "b.b"), second.file.pages[0])
        assertEquals(listOf("a.a", "b.b"), second.file.drop(From.At(second.at!!, second.file.itemAt(second.at!!)!!), Target.Remove).file.pages[0])
        assertEquals(HOME_CHANGED, refused { f.drop(at(f, Spot.Apps(page(0), 0)), Target.AtEnd(Place.InFolder(Spot.Apps(page(5), 0)))) })
    }

    @Test
    fun theOpenFoldersPlusAddsAppsAndMovesThemFromElsewhereOnHome() {
        // lor-gnk0. Page 0: x.x, [F: a.a], [G: b.b, c.c]; page 1: d.d; dock: x.x.
        val f = file(
            listOf(ref("x.x"), Folder("F", listOf(ref("a.a"))), Folder("G", listOf(ref("b.b"), ref("c.c")))),
            listOf(ref("d.d")),
            dock = listOf(ref("x.x")),
        )
        val folder = at(f, Spot.Apps(page(0), 1))
        // A new app goes last; the folder stays where it is and is pinned.
        val one = f.addToFolder(folder, listOf(app("e.e") to ref("e.e")))
        assertEquals(listOf(listOf("x.x", "[F: a.a, e.e]", "[G: b.b, c.c]"), listOf("d.d")), one.file.pages)
        assertEquals(Spot.Apps(page(0), 1), one.at)
        assertTrue((one.file.current.home[1] as Folder).pinned)
        // From elsewhere on home they move, in the order picked: an app ahead of it moves the folder up,
        // an extra page emptied goes, another folder keeps the rest; the dock keeps its own.
        val many = f.addToFolder(folder, listOf(app("d.d") to ref("d.d"), app("x.x") to ref("x.x"), app("b.b") to ref("b.b")))
        assertEquals(listOf(listOf("[F: a.a, d.d, x.x, b.b]", "[G: c.c]")), many.file.pages)
        assertEquals(Spot.Apps(page(0), 0), many.at)
        assertEquals(listOf(ref("x.x")), many.file.current.dock)
        assertEquals(many.file.current.home[0], many.file.itemAt(many.at!!))
        // A folder emptied by it goes.
        val g = f.addToFolder(folder, listOf(app("b.b") to ref("b.b"), app("c.c") to ref("c.c")))
        assertEquals(listOf(listOf("x.x", "[F: a.a, b.b, c.c]"), listOf("d.d")), g.file.pages)
        roundTrips(many.file)
        // Refusals: an app it holds, nothing picked, a folder that changed, not a folder.
        assertEquals("it's already in F", refused { f.addToFolder(folder, listOf(app("a.a") to ref("a.a"))) })
        assertEquals("no apps were picked", refused { f.addToFolder(folder, emptyList()) })
        assertEquals(HOME_CHANGED, refused { one.file.addToFolder(folder, listOf(app("z.z") to ref("z.z"))) })
        assertEquals("that isn't a folder", refused { f.addToFolder(at(f, Spot.Apps(page(0), 0)), listOf(app("z.z") to ref("z.z"))) })
    }

    @Test
    fun renamingInPlaceWritesTheTrimmedName() {
        // lor-gnk0: the open folder's name field writes through renameFolder; blank is refused.
        val f = file(listOf(Folder("F", listOf(ref("a.a"), ref("b.b")))))
        val folder = at(f, Spot.Apps(page(0), 0))
        val renamed = f.renameFolder(folder, "  Travel ")
        assertEquals("Travel", (renamed.current.home[0] as Folder).name)
        assertTrue((renamed.current.home[0] as Folder).pinned)
        assertEquals(listOf(ref("a.a"), ref("b.b")), (renamed.current.home[0] as Folder).apps)
        assertEquals("a folder needs a name", refused { f.renameFolder(folder, "") })
        // Renaming again from the name drawn before the first landed refuses rather than renaming the wrong thing.
        assertEquals(HOME_CHANGED, refused { renamed.renameFolder(folder, "Trips") })
    }
}
