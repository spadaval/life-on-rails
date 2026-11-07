package com.spadaval.lor.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class DrawerTest {
    private data class App(val pkg: String, val cls: String, val category: Category) {
        val id get() = AppId(pkg, cls)
    }

    private val slack = App("com.slack", "com.slack.Main", Category.WORK)
    private val chrome = App("com.android.chrome", "com.android.chrome.Main", Category.TOOLS)
    private val yt = App("com.google.android.youtube", "com.google.android.youtube.Main", Category.MEDIA)
    private val game = App("com.example.game", "com.example.game.Main", Category.GAMES)
    private val twoA = App("com.two", "com.two.A", Category.OTHER)
    private val twoB = App("com.two", "com.two.B", Category.OTHER)
    private val apps = listOf(chrome, game, slack, twoA, twoB, yt)

    private fun Drawer.sorted() = sort(apps, { it.id }, { it.category }).associate { it.name to it.apps }

    @Test
    fun categorisesByTableThenDeclaredThenSystem() {
        assertEquals(Category.COMMUNICATION, categoryOf("com.whatsapp", -1, false))
        // The table wins over what the app declares.
        assertEquals(Category.WORK, categoryOf("com.google.android.calendar", 3, true))
        assertEquals(Category.GAMES, categoryOf("com.example.x", 0, false))
        assertEquals(Category.MEDIA, categoryOf("com.example.x", 1, false))
        assertEquals(Category.COMMUNICATION, categoryOf("com.example.x", 4, false))
        assertEquals(Category.TRAVEL, categoryOf("com.example.x", 6, false))
        assertEquals(Category.WORK, categoryOf("com.example.x", 7, false))
        assertEquals(Category.SYSTEM, categoryOf("com.example.x", 8, false))
        assertEquals(Category.SYSTEM, categoryOf("com.example.x", -1, true))
        assertEquals(Category.OTHER, categoryOf("com.example.x", -1, false))
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
    fun ranksRecentFrequentAndMostUsed() {
        var s = LaunchStats()
        // c once long ago, then a three times, then b twice, the last launch.
        s = s.launched("c", 1)
        repeat(3) { s = s.launched("a", 10L + it) }
        repeat(2) { s = s.launched("b", 20L + it) }
        assertEquals(listOf("b", "a", "c"), s.recent())
        assertEquals(listOf("a", "b", "c"), s.mostUsed())
        // b's two recent launches outweigh a's three earlier ones? Not yet: a's weight decayed only twice.
        assertEquals(listOf("a", "b", "c"), s.frequent())
        repeat(3) { s = s.launched("b", 30L + it) }
        assertEquals(listOf("b", "a", "c"), s.frequent())
        assertEquals(listOf("b", "a", "c"), s.mostUsed())
    }

    @Test
    fun forgottenWeightsDropToZeroButCountsStay() {
        var s = LaunchStats().launched("old", 1)
        repeat(200) { s = s.launched("new", 2L + it) }
        assertEquals(0.0, s.entries["old"]!!.weight, 0.0)
        assertFalse("old" in s.frequent())
        assertEquals(listOf("new", "old"), s.mostUsed())
    }

    @Test
    fun statsRoundTripAndAnUnreadableFileStartsAgain() {
        val s = LaunchStats().launched("a/b#UserHandle{0}", 5).launched("c", 6)
        assertEquals(s.entries, LaunchStats.parse(s.toJson()).entries)
        assertEquals(emptyMap<String, LaunchStats.Entry>(), LaunchStats.parse("not json").entries)
    }

    @Test
    fun sortsByCategoryWithASectionPerCategory() {
        val sorted = Drawer().sorted()
        assertEquals(Category.entries.map { it.title }, sorted.keys.toList())
        assertEquals(listOf(slack), sorted["Work"])
        assertEquals(listOf(chrome), sorted["Tools"])
        assertEquals(listOf(twoA, twoB), sorted["Other"])
        assertEquals("every app in exactly one section", apps.toSet(), sorted.values.flatten().toSet())
        assertEquals(apps.size, sorted.values.flatten().size)
    }

    @Test
    fun aListedAppBeatsItsCategoryAndAPackageTakesEveryActivity() {
        val d = Drawer(sections = listOf(
            DrawerSection("Daily", apps = listOf(AppRef("com.android.chrome"), AppRef("com.two"), AppRef("com.not.installed"))),
            DrawerSection("Watch", Category.MEDIA, reason = "Videos"),
        ))
        val sorted = d.sort(apps, { it.id }, { it.category })
        assertEquals(listOf("Daily", "Watch", "Work", "Communication", "Money", "Travel", "Games", "Tools", "System", "Other"), sorted.map { it.name })
        assertEquals(listOf(chrome, twoA, twoB), sorted[0].apps)
        assertEquals(listOf(yt), sorted[1].apps)
        assertEquals("Videos", sorted[1].reason)
        assertTrue(sorted[0].inFile && sorted[1].inFile && !sorted[2].inFile)
        assertEquals(emptyList<App>(), sorted.first { it.name == "Tools" }.apps)
        assertEquals(emptyList<App>(), sorted.first { it.name == "Other" }.apps)
    }

    @Test
    fun anActivityRefTakesOnlyThatActivity() {
        val d = Drawer(sections = listOf(DrawerSection("Pick", apps = listOf(AppRef("com.two", "com.two.B")))))
        val sorted = d.sorted()
        assertEquals(listOf(twoB), sorted["Pick"])
        assertEquals(listOf(twoA), sorted["Other"])
    }

    @Test
    fun hiddenSectionsAreKeptAndMarked() {
        val sorted = Drawer(sections = listOf(DrawerSection("Games", Category.GAMES, hidden = true))).sort(apps, { it.id }, { it.category })
        val games = sorted.first { it.name == "Games" }
        assertTrue(games.hidden)
        assertEquals(listOf(game), games.apps)
    }

    @Test
    fun anAutomaticSectionWhoseNameIsTakenGetsASuffix() {
        val d = Drawer(sections = listOf(DrawerSection("media", apps = listOf(AppRef("com.slack")))))
        val names = d.allSections().map { it.name }
        assertTrue("Media (2)" in names)
        assertEquals(names.size, names.map { it.lowercase() }.toSet().size)
        assertEquals(listOf(yt), d.sorted()["Media (2)"])
    }

    private fun parse(drawer: String) = LayoutFile.parse(v2Empty(""""drawer": $drawer,"""))

    private fun error(drawer: String): String = try {
        parse(drawer)
        fail("parsed: $drawer")
        ""
    } catch (e: LayoutError) {
        e.message!!
    }

    @Test
    fun drawerParsesAndRoundTrips() {
        val f = parse(
            """{"view": "az", "sections": [
                 {"name": "Daily", "apps": ["com.android.chrome", "com.two/.B"], "pinned": true},
                 {"name": "Watch", "category": "media", "hidden": true, "reason": "Videos"},
                 {"name": "Empty", "apps": []}]}""",
        )
        assertEquals(DrawerView.AZ, f.drawer.view)
        assertEquals(
            listOf(
                DrawerSection("Daily", apps = listOf(AppRef("com.android.chrome"), AppRef("com.two", "com.two.B")), pinned = true),
                DrawerSection("Watch", Category.MEDIA, hidden = true, reason = "Videos"),
                DrawerSection("Empty"),
            ),
            f.drawer.sections,
        )
        assertEquals(f, LayoutFile.parse(f.toJson()))
        assertFalse("empty apps are written absent", f.toJson().contains("\"apps\": []"))
        assertEquals(Drawer(), parse("{}").drawer)
        assertFalse(parse("{}").toJson().contains("drawer"))
    }

    @Test
    fun drawerErrorsSayWhere() {
        assertEquals("drawer: must be an object", error("[]"))
        assertTrue(error("""{"views": "az"}""").startsWith("drawer: unknown field \"views\""))
        assertEquals("drawer.view: must be one of sections, az, used, got \"grid\"", error("""{"view": "grid"}"""))
        assertEquals("drawer.sections[0].name: must be a string", error("""{"sections": [{"category": "work"}]}"""))
        assertEquals(
            "drawer.sections[0].category: must be one of work, communication, money, travel, media, games, tools, system, other, got \"fun\"",
            error("""{"sections": [{"name": "A", "category": "fun"}]}"""),
        )
        assertEquals("drawer.sections: the name \"A\" is used twice", error("""{"sections": [{"name": "A"}, {"name": "a"}]}"""))
        assertEquals("drawer.sections: the category \"work\" is taken twice",
            error("""{"sections": [{"name": "A", "category": "work"}, {"name": "B", "category": "work"}]}"""))
        assertEquals("drawer.sections: \"com.x/com.x.M\" in \"B\" names an app \"com.x\" in \"A\" already names",
            error("""{"sections": [{"name": "A", "apps": ["com.x"]}, {"name": "B", "apps": ["com.x/.M"]}]}"""))
        assertEquals("drawer.sections: \"com.x\" in \"A\" names an app \"com.x\" in \"A\" already names",
            error("""{"sections": [{"name": "A", "apps": ["com.x", "com.x"]}]}"""))
        // Two activities of one package are two apps.
        parse("""{"sections": [{"name": "A", "apps": ["com.x/.M"]}, {"name": "B", "apps": ["com.x/.N"]}]}""")
        assertEquals("drawer.sections[0].hidden: must be true or false", error("""{"sections": [{"name": "A", "hidden": 1}]}"""))
        assertEquals("drawer.sections[0].apps[0]: \"X\" is not a package name or package/activity", error("""{"sections": [{"name": "A", "apps": ["X"]}]}"""))
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
