package com.spadaval.lor.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spadaval.lor.net.DeviceHome
import com.spadaval.lor.ui.Line
import com.spadaval.lor.ui.Mute
import com.spadaval.lor.ui.Type
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/*
 * What only a platform has, for home's common code (lor-xdoi): one interface, implemented by the
 * app on the phone (HomeActivity's `AndroidHome`) and by the lab in the browser
 * (`PlaceholderPlatform`, which wasmJsMain fills from a fixture). What homecore's own targets can
 * supply without the app, such as the back handlers and the log, is in Platform.kt instead.
 *
 * The actions a press takes (launching, editing the file, the system's screens) are
 * [HomeActions]'s, which this extends, so home's screens hold one object for both.
 */

/** What home draws with that only the platform has, and what its presses do there. */
interface HomePlatform : HomeActions {
    // ---- Icons ----

    /** [app]'s icon at [size]: the app's own, or its pack's, on the phone; a placeholder in the lab. */
    @Composable
    fun AppIcon(app: HomeApp, size: Dp, modifier: Modifier)

    // ---- Panels and blocks other items draw (lor-rgx4) ----

    /** The `t3` panel: the agent page. */
    @Composable
    fun AgentPanel()

    /** The `lor` panel. */
    @Composable
    fun LorPanel()

    /** The set-up card on a first-run home's start stack (until set-up is done); nothing once it is. */
    @Composable
    fun SetupCard(modifier: Modifier)

    /** One of LoR's blocks in a stack: `today`, `needs_you`, `running` or `t3_queue`. */
    @Composable
    fun LorBlock(block: StackBlock, grid: GridPlace)

    // ---- Widgets ----

    /** The widget [id] of [provider] as bound now, or null when its provider is gone or turned off. */
    fun widget(id: Int, provider: String): WidgetFacts?

    /** The label of [provider]'s app, a disabled one too; its package name when it isn't installed. */
    fun appLabel(provider: String): String

    /** The widgets the picker offers, by app, each preview fitted to [previewWidthPx] by [previewHeightPx]. Slow: off the main thread. */
    suspend fun widgetProviders(previewWidthPx: Int, previewHeightPx: Int, density: Float): List<PickerApp>

    /**
     * The widget's own view, filling [modifier], told [widthDp] by its block's height; a long press
     * on it calls [onLongPress] when not null. [press] is where that press was.
     */
    @Composable
    fun WidgetView(block: StackBlock.Widget, facts: WidgetFacts, widthDp: Float, onLongPress: (() -> Unit)?, press: WidgetPress, modifier: Modifier)

    // ---- The header and quick dial: the clock, the alarm, LoR's reply ----

    /** Now, read again each minute while home is started. */
    @Composable
    fun rememberMinute(): Instant

    /** When the next alarm rings, while home is started; null with none. */
    @Composable
    fun rememberNextAlarm(): Instant?

    /** The zone the phone is in. */
    val zone: TimeZone

    /** The header's date for [now], as "Tuesday 17 November" in the phone's language. */
    fun dateWords(now: Instant): String

    /** The locale reads Fahrenheit (its measurement system is the US's, decision lor-g4ta). */
    val fahrenheit: Boolean

    /** What LoR's `/device/home` said last, for the header and quick dial's card. */
    val lor: State<LorRead>

    /** Keeps LoR's reader going until cancelled; held while the header or quick dial's card shows. */
    suspend fun holdLor()

    /** Quick dial's counts of when apps are opened. */
    val opens: StateFlow<AppOpens>

    /** Reads the phone's app usage into [opens], as the drawer opens. */
    fun readUsage()

    // ---- Notification dots, the wallpaper, the window ----

    /** The [HomeApp.dotKey]s of apps with a notification dot. */
    val dots: StateFlow<Set<String>>

    /** The wallpaper's ink: dark text over a light wallpaper. */
    val wallpaperInk: StateFlow<Ink>

    /** A LoR write's line for home's Undo bar (lor-rz50), or null. */
    val lorBar: UndoMessage?

    /** Whether the window can blur what is behind it now. */
    @Composable
    fun rememberBlur(): Boolean

    /** Blurs the wallpaper behind the window by [shown], 0 to 1, while [on]. */
    @Composable
    fun BlurBehind(on: Boolean, shown: () -> Float)

    /** The system bars' icons dark (over a light wallpaper) or light. */
    @Composable
    fun SystemBars(darkIcons: Boolean)

    /** Puts [text] on the clipboard, [label] naming it. */
    fun copy(label: String, text: String)

    /** Milliseconds since 1970, as the phone's clock says. */
    fun now(): Long = Clock.System.now().toEpochMilliseconds()
}

/** What LoR's `/device/home` said last: the reply, whether it is current, whether the last read failed, and when it was read. */
data class LorRead(val home: DeviceHome? = null, val current: Boolean = false, val failed: Boolean = false, val at: Instant? = null)

/** A bound widget's facts from its provider: its label, and its limits in px, as Android gives them. */
open class WidgetFacts(
    val label: String,
    val minHeight: Int,
    val minResizeHeight: Int,
    val maxResizeHeight: Int,
    val maxResizeWidth: Int,
    /** Its configure screen can be opened again (the menu's Settings). */
    val reconfigurable: Boolean,
)

/** Where the last long press on a widget was, in the window; set by the widget's view. */
class WidgetPress {
    var at: () -> Bounds? = { null }
}

/**
 * A widget provider the picker offers. The app's version holds Android's
 * `AppWidgetProviderInfo`, which binding it needs.
 */
open class WidgetProvider(
    /** `package/class`. */
    val provider: String,
    val packageName: String,
)

/** A widget the picker offers: its provider, label, preview (else its app's icon) and minimum size in dp. */
class PickerWidget(val info: WidgetProvider, val label: String, val image: ImageBitmap?, val minWidth: Int, val minHeight: Int)

/** An app's widgets in the picker. */
class PickerApp(val label: String, val icon: ImageBitmap?, val widgets: List<PickerWidget>)

/** The platform home's screens draw with; the lab's placeholder until a screen provides one. */
val LocalHomePlatform = staticCompositionLocalOf<HomePlatform> { PlaceholderPlatform() }

/** [app]'s icon, from the [HomePlatform] in place. */
@Composable
fun AppIcon(app: HomeApp, size: Dp, modifier: Modifier = Modifier) = LocalHomePlatform.current.AppIcon(app, size, modifier)

/**
 * The platform where the phone's own parts can't be had: the lab (lor-xdoi). Icons are discs with
 * the app's initial, widgets labelled boxes of their height (STATES.md's decision), the agent page
 * and the LoR panel a note naming them until lor-rgx4 moves them, LoR's reply what [lorRead] holds,
 * and the presses log what they would do. Open so the lab can fill what a fixture names.
 */
open class PlaceholderPlatform(
    override val zone: TimeZone = TimeZone.UTC,
    private val clock: () -> Instant = { Clock.System.now() },
    lorRead: LorRead = LorRead(),
    override val dots: StateFlow<Set<String>> = MutableStateFlow(emptySet()),
    override val wallpaperInk: StateFlow<Ink> = MutableStateFlow(Ink.LIGHT),
    override val opens: StateFlow<AppOpens> = MutableStateFlow(AppOpens()),
    private val nextAlarm: Instant? = null,
) : HomePlatform {
    override val lor: State<LorRead> = mutableStateOf(lorRead)
    override val fahrenheit: Boolean = false
    override val lorBar: UndoMessage? = null

    override fun now(): Long = clock().toEpochMilliseconds()

    @Composable
    override fun AppIcon(app: HomeApp, size: Dp, modifier: Modifier) {
        Box(modifier.size(size).background(Line, CircleShape).border(1.dp, Mute, CircleShape), contentAlignment = Alignment.Center) {
            Text(app.label.take(1).uppercase(), style = Type.title)
        }
    }

    @Composable
    override fun AgentPanel() = Placeholder("The agent page (lor-rgx4)")

    @Composable
    override fun LorPanel() = Placeholder("The LoR panel (lor-rgx4)")

    @Composable
    override fun SetupCard(modifier: Modifier) {}

    @Composable
    override fun LorBlock(block: StackBlock, grid: GridPlace) = Box(Modifier.padding(12.dp)) { Note("LoR's ${block.type} block (lor-rgx4)") }

    override fun widget(id: Int, provider: String): WidgetFacts? =
        WidgetFacts(provider.substringAfterLast('.').ifEmpty { provider }, 40, 40, 1_000, 10_000, reconfigurable = false)

    override fun appLabel(provider: String): String = provider.substringBefore('/')

    override suspend fun widgetProviders(previewWidthPx: Int, previewHeightPx: Int, density: Float): List<PickerApp> = emptyList()

    @Composable
    override fun WidgetView(block: StackBlock.Widget, facts: WidgetFacts, widthDp: Float, onLongPress: (() -> Unit)?, press: WidgetPress, modifier: Modifier) {
        Box(modifier.border(1.dp, Mute), contentAlignment = Alignment.Center) { Note("${facts.label}, ${block.height} dp") }
    }

    @Composable
    override fun rememberMinute(): Instant = clock()

    @Composable
    override fun rememberNextAlarm(): Instant? = nextAlarm

    override fun dateWords(now: Instant): String = englishDate(now, zone)

    override suspend fun holdLor() {}

    override fun readUsage() {}

    @Composable
    override fun rememberBlur(): Boolean = false

    @Composable
    override fun BlurBehind(on: Boolean, shown: () -> Float) {}

    @Composable
    override fun SystemBars(darkIcons: Boolean) {}

    override fun copy(label: String, text: String) = say("copied $label")

    // ---- HomeActions: the lab has nowhere to go, so each says what it would do ----

    /** What a press would have done on the phone. */
    open fun say(what: String) {
        Log.i("LorLab", what)
    }

    override fun open(app: HomeApp, at: At, icon: IconSpot?) = say("open ${app.label} from ${at.from}")
    override suspend fun shortcuts(app: HomeApp): List<MenuShortcut> = emptyList()
    override fun openShortcut(app: HomeApp, shortcut: AppShortcut, at: At, icon: IconSpot?) = say("open shortcut ${shortcut.id}")
    override suspend fun shortcutIcon(shortcut: AppShortcut): ImageBitmap? = null
    override fun appInfo(app: HomeApp) = say("app info for ${app.label}")
    override fun uninstall(app: HomeApp) = say("uninstall ${app.label}")
    override fun settings() = say("home settings")
    override fun wallpaper() = say("wallpaper picker")
    override fun calendar() = say("calendar")
    override fun alarms() = say("alarms")
    override fun edit(done: String?, after: () -> Unit, change: (LayoutFile) -> LayoutFile) = say("edit: ${done ?: "Home changed."}")
    override fun refused(reason: String) = say("refused: $reason")
    override fun undo() = say("undo")
    override fun openSetting(page: SettingsPage, query: String) = say("settings page ${page.title}")
    override fun openContact(contact: SearchContact, way: ContactWay, query: String) = say("contact ${contact.name}: $way")
    override fun clearSearches() = say("clear searches")
    override fun elsewhere(way: Elsewhere, query: String) = say("elsewhere: ${way.kind}")
    override fun restore() = say("restore the layout")
    override fun quickSend() = say("quick send")
    override fun addWidget(at: Cell, provider: WidgetProvider) = say("add widget ${provider.provider} at $at")
    override fun widgetSettings(id: Int) = say("widget settings $id")
}

@Composable
private fun Placeholder(what: String) = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Note(what) }

@Composable
private fun Note(text: String) = Text(text, style = Type.bodySmall, textAlign = TextAlign.Center)

/** "Tuesday 17 November": the header's date in English, for the lab. */
fun englishDate(now: Instant, zone: TimeZone): String {
    val d = now.toLocalDateTime(zone)
    val day = d.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
    val month = d.month.name.lowercase().replaceFirstChar { it.uppercase() }
    return "$day ${d.day} $month"
}
