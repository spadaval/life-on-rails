package com.spadaval.lor.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class DrawerTest {
    private data class App(val pkg: String, val cls: String, val work: Boolean = false) {
        val id get() = AppId(pkg, cls, work)
    }

    // In label order, as the drawer gets them.
    private val chrome = App("com.android.chrome", "com.android.chrome.Main")
    private val slack = App("com.slack", "com.slack.Main")
    private val slackWork = App("com.slack", "com.slack.Main", work = true)
    private val twoA = App("com.two", "com.two.A")
    private val twoB = App("com.two", "com.two.B")
    private val yt = App("com.google.android.youtube", "com.google.android.youtube.Main")
    private val apps = listOf(chrome, slack, slackWork, twoA, twoB, yt)

    private val quick = """{"name": "Quick dial", "icon": "bolt", "kind": "quick"}"""
    private val all = """{"name": "All", "icon": "all", "kind": "all"}"""

    private fun parse(drawer: String) = LayoutFile.parse(v2Empty(""""drawer": $drawer,"""))

    private fun error(drawer: String): String = try {
        parse(drawer)
        fail("parsed: $drawer")
        ""
    } catch (e: LayoutError) {
        e.message!!
    }

    /** A drawer of quick dial, [middle] (tab objects) and All. */
    private fun tabs(vararg middle: String) = """{"tabs": [${(listOf(quick) + middle + all).joinToString()}]}"""

    @Test
    fun aListTabShowsItsAppsInItsOrderAndAPackageTakesEveryActivity() {
        val tab = DrawerTab("Daily", "star", apps = listOf(AppRef("com.google.android.youtube"), AppRef("com.two"), AppRef("com.not.installed"), AppRef("com.slack", work = true)))
        assertEquals(listOf(yt, twoA, twoB, slackWork), tab.shown(apps) { it.id })
        // An activity ref takes only that activity.
        assertEquals(listOf(twoB), DrawerTab("Pick", "star", apps = listOf(AppRef("com.two", "com.two.B"))).shown(apps) { it.id })
        assertEquals(emptyList<App>(), DrawerTab("Empty", "star").shown(apps) { it.id })
    }

    @Test
    fun anAppIsInTheListTabThatNamesItAndOnlyThere() {
        val f = parse(tabs("""{"name": "Work", "icon": "brief", "apps": ["work:com.slack", "com.two/.A"]}""", """{"name": "Watch", "icon": "play", "apps": ["com.google.android.youtube"]}"""))
        assertEquals("Work", f.drawer.tabOf(slackWork.id)?.name)
        assertEquals("Work", f.drawer.tabOf(twoA.id)?.name)
        assertEquals("Watch", f.drawer.tabOf(yt.id)?.name)
        // The main twin and an app no list names are in no list tab (All still shows them).
        assertNull(f.drawer.tabOf(slack.id))
        assertNull(f.drawer.tabOf(twoB.id))
        assertNull(f.drawer.tabOf(chrome.id))
    }

    @Test
    fun withoutTabsTheDrawerIsQuickDialThenAll() {
        val d = parse("{}").drawer
        assertEquals(listOf(TabKind.QUICK, TabKind.ALL), d.tabs.map { it.kind })
        assertEquals(Drawer(), d)
        assertFalse(parse("{}").toJson().contains("drawer"))
        // Written out only once they differ.
        assertEquals(Drawer(), parse(tabs()).drawer)
    }

    @Test
    fun tabsParseAndRoundTrip() {
        val f = parse(tabs("""{"name": "Work", "icon": "brief", "apps": ["com.slack", "work:com.slack", "com.two/.B"]}""", """{"name": "Empty", "icon": "nope", "apps": []}"""))
        assertEquals(
            listOf(
                Drawer.DEFAULT_TABS[0],
                DrawerTab("Work", "brief", apps = listOf(AppRef("com.slack"), AppRef("com.slack", work = true), AppRef("com.two", "com.two.B"))),
                // An icon the phone doesn't draw is kept; the button draws the name's first letter.
                DrawerTab("Empty", "nope"),
                Drawer.DEFAULT_TABS[1],
            ),
            f.drawer.tabs,
        )
        assertEquals(f, LayoutFile.parse(f.toJson()))
        assertTrue("an empty list tab keeps its apps", f.toJson().contains("\"apps\": []"))
        assertNull(TabIcon.of("nope"))
        assertEquals("E", tabLetter("Empty"))
        assertEquals("2", tabLetter(" 2nd"))
    }

    @Test
    fun anOlderDrawersSectionsAndViewAreReadAndDropped() {
        val f = parse("""{"view": "az", "keyboard": true, "sections": [{"name": "Work", "category": "work", "apps": ["com.slack"]}]}""")
        assertEquals(Drawer(keyboard = true), f.drawer)
        assertFalse(f.toJson().contains("sections"))
        assertFalse(f.toJson().contains("\"view\""))
    }

    @Test
    fun tabErrorsSayWhere() {
        assertEquals("drawer: must be an object", error("[]"))
        assertTrue(error("""{"tab": []}""").startsWith("drawer: unknown field \"tab\""))
        assertEquals("drawer.tabs: must be a list", error("""{"tabs": {}}"""))
        assertEquals("drawer.tabs: needs exactly one \"quick\" tab, got 0", error("""{"tabs": []}"""))
        assertEquals("drawer.tabs: needs exactly one \"quick\" tab, got 0", error("""{"tabs": [$all]}"""))
        assertEquals("drawer.tabs: needs exactly one \"all\" tab, got 0", error("""{"tabs": [$quick]}"""))
        assertEquals("drawer.tabs: needs exactly one \"quick\" tab, got 2",
            error("""{"tabs": [$quick, {"name": "Q2", "icon": "bolt", "kind": "quick"}, $all]}"""))
        assertEquals("drawer.tabs[1]: the \"quick\" tab must be first",
            error("""{"tabs": [{"name": "A", "icon": "star", "apps": []}, $quick, $all]}"""))
        assertEquals("drawer.tabs[1]: the \"all\" tab must be last",
            error("""{"tabs": [$quick, $all, {"name": "A", "icon": "star", "apps": []}]}"""))
        val nine = (1..7).map { """{"name": "T$it", "icon": "star", "apps": []}""" }.toTypedArray()
        assertEquals("drawer.tabs: at most 8 tabs, got 9", error(tabs(*nine)))
        parse(tabs(*nine.drop(1).toTypedArray()))
        assertEquals("drawer.tabs: the name \"all\" is used twice", error(tabs("""{"name": "all", "icon": "star", "apps": []}""")))
        assertEquals("drawer.tabs[1].name: must be a string", error(tabs("""{"icon": "star", "apps": []}""")))
        assertEquals("drawer.tabs[1].icon: must not be empty", error(tabs("""{"name": "A", "icon": "", "apps": []}""")))
        assertEquals("drawer.tabs[1]: needs \"apps\" (a list tab) or \"kind\" (\"quick\" or \"all\"), not both",
            error(tabs("""{"name": "A", "icon": "star"}""")))
        assertEquals("drawer.tabs[1]: needs \"apps\" (a list tab) or \"kind\" (\"quick\" or \"all\"), not both",
            error(tabs("""{"name": "A", "icon": "star", "kind": "all", "apps": []}""")))
        assertEquals("drawer.tabs[0].kind: must be \"quick\" or \"all\", got \"list\"",
            error("""{"tabs": [{"name": "Q", "icon": "bolt", "kind": "list"}, $all]}"""))
        assertTrue(error(tabs("""{"name": "A", "icon": "star", "apps": [], "reason": "x"}""")).startsWith("drawer.tabs[1]: unknown field \"reason\""))
        assertEquals("drawer.tabs[1].apps[0]: \"X\" is not a package name or package/activity", error(tabs("""{"name": "A", "icon": "star", "apps": ["X"]}""")))
        assertEquals("drawer.tabs: \"com.x/com.x.M\" in \"B\" names an app \"com.x\" in \"A\" already names",
            error(tabs("""{"name": "A", "icon": "star", "apps": ["com.x"]}""", """{"name": "B", "icon": "star", "apps": ["com.x/.M"]}""")))
        // Two activities of one package, or an app and its work twin, are two apps.
        parse(tabs("""{"name": "A", "icon": "star", "apps": ["com.x/.M", "work:com.x"]}""", """{"name": "B", "icon": "star", "apps": ["com.x/.N"]}"""))
    }

    @Test
    fun weightsFollowKvaesitso() {
        var s = LaunchStats()
        s = s.launched("a", 1)
        assertEquals(0.03, s.entries["a"]!!.weight, 1e-9)
        s = s.launched("b", 2)
        assertEquals(0.03 * 0.97, s.entries["a"]!!.weight, 1e-9)
        assertEquals(0.03, s.entries["b"]!!.weight, 1e-9)
        s = s.launched("a", 3)
        assertEquals(0.03 * 0.97 + 0.03 * (1 - 0.03 * 0.97), s.entries["a"]!!.weight, 1e-9)
        assertEquals(2, s.entries["a"]!!.count)
        assertEquals(3L, s.entries["a"]!!.last)
    }

    @Test
    fun forgottenWeightsDropToZeroButCountsStay() {
        var s = LaunchStats().launched("old", 1)
        repeat(200) { s = s.launched("new", 2L + it) }
        assertEquals(0.0, s.entries["old"]!!.weight, 0.0)
        assertEquals(1, s.entries["old"]!!.count)
    }

    @Test
    fun statsRoundTripAndAnUnreadableFileStartsAgain() {
        val s = LaunchStats().launched("a/b#UserHandle{0}", 5).launched("c", 6)
        assertEquals(s.entries, LaunchStats.parse(s.toJson()).entries)
        assertEquals(emptyMap<String, LaunchStats.Entry>(), LaunchStats.parse("not json").entries)
    }

    @Test
    fun recentSearchesKeepFiveNewestFirstOncePerText() {
        var s = LaunchStats()
        for (q in listOf("cal", "maps", " Cal ", "", "a", "b", "c", "d")) s = s.searched(q)
        assertEquals(listOf("d", "c", "b", "a", "Cal"), s.searches)
        // A launch keeps them, and they survive the file.
        s = s.launched("x", 1)
        assertEquals(listOf("d", "c", "b", "a", "Cal"), s.searches)
        val back = LaunchStats.parse(s.toJson())
        assertEquals(s.searches, back.searches)
        assertEquals(s.entries, back.entries)
        // Cleared, the file holds none.
        assertFalse(s.clearSearches().toJson().contains("searches"))
        assertEquals(emptyList<String>(), LaunchStats.parse(s.clearSearches().toJson()).searches)
    }

    @Test
    fun aStatsFileWithoutSearchesParses() {
        val s = LaunchStats.parse("""{"version": 1, "apps": {"a": {"count": 2, "weight": 0.5, "last": 7}}}""")
        assertEquals(LaunchStats.Entry(2, 0.5, 7), s.entries["a"])
        assertEquals(emptyList<String>(), s.searches)
    }

    @Test
    fun drawerKeyboardParsesAndIsWrittenOnlyWhenTrue() {
        assertTrue(parse("""{"keyboard": true}""").drawer.keyboard)
        assertFalse(parse("""{"keyboard": false}""").drawer.keyboard)
        assertFalse(parse("""{}""").drawer.keyboard)
        assertEquals("drawer.keyboard: must be true or false", error("""{"keyboard": "yes"}"""))
        val on = parse("""{"keyboard": true}""")
        assertEquals(on, LayoutFile.parse(on.toJson()))
        assertTrue(on.toJson().contains("\"keyboard\": true") || on.toJson().contains("\"keyboard\":true"))
        assertFalse(parse("""{"keyboard": false}""").toJson().contains("keyboard"))
    }
}
