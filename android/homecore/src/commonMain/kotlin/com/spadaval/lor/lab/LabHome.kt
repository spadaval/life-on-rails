package com.spadaval.lor.lab

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.spadaval.lor.agent.AgentPage
import com.spadaval.lor.agent.InGrid
import com.spadaval.lor.home.GridPlace
import com.spadaval.lor.home.SetupActions
import com.spadaval.lor.home.cellInsetsPadding
import com.spadaval.lor.home.listTakes
import com.spadaval.lor.home.scrollsInGrid
import com.spadaval.lor.lor.LorBlockKind
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import com.spadaval.lor.home.AppOpens
import com.spadaval.lor.home.AppShortcut
import com.spadaval.lor.home.AppsLoad
import com.spadaval.lor.home.BlockAt
import com.spadaval.lor.home.Cell
import com.spadaval.lor.home.Folder
import com.spadaval.lor.home.Found
import com.spadaval.lor.home.HomeApp
import com.spadaval.lor.home.HomeScreen
import com.spadaval.lor.home.HomeUi
import com.spadaval.lor.home.Ink
import com.spadaval.lor.home.Kind
import com.spadaval.lor.home.LaunchStats
import com.spadaval.lor.home.LayoutError
import com.spadaval.lor.home.LayoutFile
import com.spadaval.lor.home.LayoutState
import com.spadaval.lor.home.LorRead
import com.spadaval.lor.home.Match
import com.spadaval.lor.home.OpenFolder
import com.spadaval.lor.home.Panel
import com.spadaval.lor.home.PlaceholderPlatform
import com.spadaval.lor.home.SearchContact
import com.spadaval.lor.home.SearchTargets
import com.spadaval.lor.home.ShortcutHit
import com.spadaval.lor.home.Spot
import com.spadaval.lor.home.StackBlock
import com.spadaval.lor.home.opensKey
import com.spadaval.lor.net.DeviceHome
import com.spadaval.lor.ui.LorTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.TimeZone
import kotlin.time.Instant
import com.spadaval.lor.home.SearchSources as HomeSources
import com.spadaval.lor.home.SettingsPage as HomeSettingsPage

/*
 * Home in the lab (version `current`, lor-xdoi): the phone's own HomeScreen from homecore, drawn
 * from a fixture. The fixture stands in for everything the app reads from Android and LoR: the app
 * list, `layout.json`, the launch counts, search's sources, LoR's `/device/home` reply (never read
 * from LoR), the dots, the clock; [LabPlatform] draws the agent page, the LoR panel, LoR's blocks
 * and the set-up card with lor-rgx4's functions from the same fixture, and answers the rest with
 * placeholders (icons, widgets). Where the screen is (`where`) is applied once home has drawn its
 * first frame.
 */

/** The work profile's user, as `UserHandle.toString()` writes it on a phone. */
private const val WORK = "UserHandle{10}"
private const val MAIN = "UserHandle{0}"

/** A fixture's app as home's model holds it. */
fun App.toHome(): HomeApp = HomeApp(
    label, pkg, cls, work,
    key = "$pkg/$cls#${if (work) WORK else MAIN}",
    profile = if (work) WORK else MAIN,
    firstInstalled = Instant.parse(installed).toEpochMilliseconds(),
)

/** The fixture's `layout.json` as home's store would read it: a file that doesn't parse shows its last good one, which the lab has none of. */
fun Fixture.layoutState(): LayoutState {
    val text = layout?.toString() ?: return LayoutState.Ready(LayoutFile.firstRun(apps.list.map { it.pkg }.toSet(), emptyList()))
    return try {
        LayoutState.Ready(LayoutFile.parse(text))
    } catch (e: LayoutError) {
        // The phone shows the last good file under the banner; the lab's broken fixture keeps the demo layout as that.
        val good = runCatching { LayoutFile.parse(Parts.demoLayout.toString()) }.getOrNull()
        LayoutState.Broken(e.message ?: "the file can't be read", good)
    }
}

/** What the app's store builds for search from the phone, from the fixture's lists. */
fun Fixture.searchSources(apps: List<HomeApp>): HomeSources {
    val shortcuts = search.shortcuts.mapIndexed { i, s ->
        val owner = apps.firstOrNull { it.packageName == s.pkg }
        val hit = ShortcutHit(
            AppShortcut("lab-$i", s.pkg, owner?.id?.className, owner?.profile ?: MAIN, s.label, null, isCached = s.conversation, rank = i, lastChangedTimestamp = Instant.parse(clock.now).toEpochMilliseconds() - i * 60_000L),
            s.conversation,
        )
        Found<Any>(Kind.SHORTCUTS, "s:${s.pkg}/lab-$i#${owner?.profile ?: MAIN}", listOf(Match.prepare(s.label)), hit)
    }
    val settings = search.settingsPages.mapNotNull { p -> HomeSettingsPage.ALL.firstOrNull { it.title == p.title } }
        .map { HomeSettingsPage.found<Any>(it, it) }
    val contacts = search.contacts.mapIndexed { i, c ->
        val contact = SearchContact(i.toLong(), "lab-$i", c.name, c.numbers)
        Found<Any>(Kind.CONTACTS, "c:$i", listOf(Match.prepare(c.name)), contact, c.numbers.map { n -> n.filter(Char::isDigit) })
    }
    return HomeSources(SearchTargets("Chrome", "Play Store"), shortcuts, settings, contacts)
}

/** The launch counts search ranks by, keyed as home keys apps. */
fun Fixture.launchStats(apps: List<HomeApp>): LaunchStats {
    val now = Instant.parse(clock.now).toEpochMilliseconds()
    val byRef = apps.associateBy { (if (it.id.work) "work:" else "") + "${it.packageName}/${it.id.className}" }
    val entries = search.launches.mapNotNull { (ref, n) -> byRef[ref]?.let { it.key to LaunchStats.Entry(n, n.toDouble(), now - 3_600_000L) } }.toMap()
    return LaunchStats(entries, search.searches)
}

/** Quick dial's counts, made so its ranking for the fixture's hour is [SearchSources.quickDial]'s order. */
fun Fixture.appOpens(apps: List<HomeApp>, zone: TimeZone): AppOpens {
    val now = Instant.parse(clock.now).toEpochMilliseconds()
    val slot = com.spadaval.lor.home.HourSlot.of(Instant.parse(clock.now), zone).index
    val byRef = apps.associateBy { (if (it.id.work) "work:" else "") + "${it.packageName}/${it.id.className}" }
    val ranked = search.quickDial.mapNotNull { byRef[it] }
    val counts = ranked.mapIndexed { i, app -> opensKey(app.id) to DoubleArray(com.spadaval.lor.home.HourSlot.COUNT) { s -> if (s == slot) 40.0 - i * 4 else 0.0 } }.toMap()
    return AppOpens(counts, now, ranked.associate { opensKey(it.id) to now - 86_400_000L })
}

/** LoR's `/device/home` read as the app's reader would hold it. */
fun Fixture.lorRead(): LorRead {
    val r = lor.home
    val home = r.reply?.let { runCatching { DeviceHome.parse(it.toString()) }.getOrNull() }
    val at = r.readAt?.let(Instant::parse)
    return LorRead(home, current = r.load == Load.OK, failed = r.load == Load.FAILED || r.load == Load.STALE, at = at)
}

/**
 * The platform the lab's home draws on: the fixture's clock, LoR reply, dots and quick dial, and
 * placeholders for the rest. An edit changes the lab's own copy of the layout, with Undo, as the
 * phone's store changes the file; a press that would leave home (open an app, a settings page)
 * says in home's bar what the phone would have done.
 */
class LabPlatform(fixture: Fixture, apps: List<HomeApp>, wallpaper: Wallpaper, private val ui: HomeUi, start: LayoutState) : PlaceholderPlatform(
    zone = TimeZone.of(fixture.clock.zone),
    clock = { Instant.parse(fixture.clock.now) },
    lorRead = fixture.lorRead(),
    dots = MutableStateFlow(apps.filter { fixture.phone.dots.getOrElse(it.packageName) { 0 } > 0 }.map { "${it.packageName}#${it.profile}" }.toSet()),
    // The pale wallpaper is light, so home's text takes dark ink over it, as the phone reads its hints.
    wallpaperInk = MutableStateFlow(if (wallpaper == Wallpaper.Pale) Ink.DARK else Ink.LIGHT),
    opens = MutableStateFlow(fixture.appOpens(apps, TimeZone.of(fixture.clock.zone))),
) {
    /** The layout home draws: the fixture's, then as edits leave it. */
    var layout by mutableStateOf(start)
        private set

    private val undos = ArrayDeque<LayoutFile>()

    override fun say(what: String) {
        super.say(what)
        ui.undo.say("Lab: $what", failed = false)
    }

    override fun edit(done: String?, after: () -> Unit, change: (LayoutFile) -> LayoutFile) {
        val now = (layout as? LayoutState.Ready)?.file ?: return say("the broken file takes no edits")
        try {
            val next = change(now)
            // As the store does: what is written is what parses back.
            val written = LayoutFile.parse(next.toJson())
            if (written != now) {
                undos.addLast(now)
                layout = LayoutState.Ready(written)
                ui.undo.show(done ?: "Home changed.")
            }
        } catch (e: LayoutError) {
            ui.undo.say("Can't: ${e.message ?: "the layout couldn't be changed"}.", failed = false)
        }
        after()
    }

    override fun undo() {
        val before = undos.removeLastOrNull() ?: return
        layout = LayoutState.Ready(before)
    }

    override fun refused(reason: String) = ui.undo.say("Can't: $reason.", failed = false)

    // ---- The panels and blocks lor-rgx4 moved: drawn from the fixture as LabPanels makes them ----

    private var panelUi by mutableStateOf(LabPanels.ui(fixture))
    private val panelOn = LabPanels.actions(fixture, { panelUi }, { panelUi = it })
    private val setupDone = fixture.settings.setupDone
    private val setup = LabPanels.setup(fixture)

    /** As the app's grid hands it to the panels: the cell's insets, and its scrolls arbitrated with the grid's. */
    private val inGrid = InGrid(
        insets = Modifier.cellInsetsPadding(),
        scrolls = { scroll: ScrollState, reversed: Boolean ->
            if (reversed) Modifier.scrollsInGrid { way -> listTakes(way, scroll.canScrollForward, scroll.canScrollBackward, reversed = true) }
            else Modifier.scrollsInGrid(scroll)
        },
    )

    @Composable
    override fun AgentPanel() = AgentPage(panelUi, panelOn, inGrid)

    @Composable
    override fun LorPanel() = com.spadaval.lor.lor.LorPanel(panelUi, panelOn, inGrid)

    @Composable
    override fun LorBlock(block: StackBlock, grid: GridPlace) {
        fun goTo(t3: Boolean): (() -> Unit)? = grid.panels.firstOrNull { if (t3) it is Panel.T3 else it is Panel.Lor }?.let { p -> { grid.goTo(p.at, animate = true) } }
        val (kind, more) = when (block) {
            StackBlock.Today -> LorBlockKind.TODAY to null
            StackBlock.NeedsYou -> LorBlockKind.NEEDS_YOU to goTo(t3 = true)
            StackBlock.Running -> LorBlockKind.RUNNING to goTo(t3 = true)
            StackBlock.T3Queue -> LorBlockKind.T3_QUEUE to goTo(t3 = false)
            else -> return
        }
        com.spadaval.lor.lor.LorBlock(kind, panelUi, panelOn, more)
    }

    @Composable
    override fun SetupCard(modifier: Modifier) {
        if (!setupDone) com.spadaval.lor.home.SetupCard(setup, SetupActions(pair = LabPanels.pairCalls), modifier)
    }
}

/** Home, drawn from [scene]'s fixture. */
@Composable
fun LabHome(scene: LabScene) {
    val f = scene.fixture
    val apps = remember(f) { f.apps.list.map { it.toHome() }.sortedBy { it.label.lowercase() } }
    val load = when {
        f.apps.load == Load.LOADING -> AppsLoad.LOADING
        apps.isEmpty() -> AppsLoad.EMPTY
        else -> AppsLoad.LOADED
    }
    val stats = remember(f, apps) { f.launchStats(apps) }
    val search = remember(f, apps) { f.searchSources(apps) }
    val ui = remember(f) { HomeUi() }
    val platform = remember(f, apps, scene.wallpaper) { LabPlatform(f, apps, scene.wallpaper, ui, f.layoutState()) }
    val layout = platform.layout
    LaunchedEffect(ui) {
        // After the first frame, as on the phone, where the first layout puts home on its start.
        withFrameNanos {}
        withFrameNanos {}
        ui.applyWhere(f.where, (layout as? LayoutState.Ready)?.file ?: (layout as? LayoutState.Broken)?.lastGood)
    }
    LorTheme {
        HomeScreen(apps, load, layout, 0, stats, search, ui, platform)
    }
}

/** The [HomeApp.key] of the app a fixture's ref names (`pkg/cls`, `work:` in front for the work profile). */
fun keyOfRef(ref: String): String {
    val work = ref.startsWith("work:")
    return "${ref.removePrefix("work:")}#${if (work) WORK else MAIN}"
}

/** Puts home where [where] says: on a cell, with the drawer, search, a folder or the minimap open. */
fun HomeUi.applyWhere(where: Where, file: LayoutFile?) {
    where.cell?.let { (c, r) -> grid.goTo(Cell(c, r), animate = false) }
    when (where.open) {
        Opened.DRAWER -> {
            openDrawer(animate = false)
            file?.drawer?.tabs?.indexOfFirst { it.name == where.target }?.takeIf { it >= 0 }?.let { tab = it }
            scrollRequest = where.scroll
        }
        Opened.SEARCH -> {
            openDrawer(animate = false)
            searchOpen = true
            query = where.typed
        }
        Opened.FOLDER -> {
            val name = where.target ?: return
            val layout = file?.current ?: return
            for (at in layout.appsBlocks) {
                val items = layout.items(at) ?: continue
                val i = items.indexOfFirst { it is Folder && it.name == name }
                if (i >= 0) {
                    folder = OpenFolder(Spot.Apps(at, i), name, (items[i] as Folder).apps)
                    return
                }
            }
        }
        Opened.MINIMAP -> openMinimap()
        Opened.APP_MENU -> {
            // On the drawer's search when something is typed, else on home's cell.
            if (where.typed.isNotEmpty()) {
                openDrawer(animate = false)
                searchOpen = true
                query = where.typed
            }
            menuRequest = where.target?.let(::keyOfRef)
        }
        else -> {}
    }
}
