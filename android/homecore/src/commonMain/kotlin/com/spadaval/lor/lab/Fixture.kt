package com.spadaval.lor.lab

import com.spadaval.lor.data.Sensor
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/*
 * A named state: everything a launcher or app screen draws from, as plain data, with nothing read
 * from LoR, T3, the phone or a file. The lab draws a version from one, and so does the debug app's
 * `lab_state` entry, so a state is one name away on both.
 *
 * What has a wire format keeps it, verbatim: the layout as `layout.json` holds it, and LoR's
 * `/device/home`, `/device/tasks` and `/device/people` replies as the server sends them. The app's
 * parsers and the common reader that replaces them (lor-xdoi) read the same text, so no model is
 * written twice. What has none (the app list, T3, the timer, sensors, settings, search's sources,
 * and where the screen is) is a small class here, named after the app's own types; the item that
 * moves a surface into this module may reshape its part.
 *
 * A fixture is composed from parts (`Parts`), so a new state is a `copy` of another with a part or
 * two changed (`Fixtures`). Every field has a default, so a fixture's JSON may leave most of it out.
 */

/** A named state, and the parts it is made of. */
@Serializable
data class Fixture(
    val name: String,
    val about: String = "",
    /** The time the state is at, which the screens read instead of the phone's clock. */
    val clock: Clock = Clock(),
    /** `layout.json` as the file holds it (schema version 2); null when there is no file yet, as on a first run. */
    val layout: JsonObject? = null,
    val apps: Apps = Apps(),
    /** LoR's three reads, each with its reply. */
    val lor: Lor = Lor(),
    val t3: T3 = T3(),
    val timer: Timer = Timer(),
    val phone: Phone = Phone(),
    val settings: Settings = Settings(),
    /** What search, the drawer and quick dial read besides the app list. */
    val search: SearchSources = SearchSources(),
    /** Where the screen is: the cell, what is open, the scroll, the typed text, the held item. */
    val where: Where = Where(),
)

/** An instant, with the zone it is read in: the payload's `now` and `timezone`. */
@Serializable
data class Clock(val now: String = "2026-11-17T20:00:00.000Z", val zone: String = "America/New_York")

/** How far a read has got: [OK] has its reply; [STALE] has an older one and failed since; [FAILED] has none. */
@Serializable
enum class Load { LOADING, OK, STALE, FAILED }

// ---- Apps ----

/**
 * An installed app (the app's `HomeApp`, without Android's `LauncherActivityInfo`). [icon] names a
 * bundled icon; null draws a placeholder from the label.
 */
@Serializable
data class App(
    val label: String,
    val pkg: String,
    val cls: String = "$pkg.Main",
    val work: Boolean = false,
    val icon: String? = null,
    /** When it was first installed, ISO; quick dial's New reads it. */
    val installed: String = "2026-01-01T00:00:00Z",
) {
    /** How `layout.json` names it: `pkg/cls`, with `work:` in front for the work profile. */
    val ref: String get() = (if (work) "work:" else "") + "$pkg/$cls"
}

/** The app list and how far its read has got (the app's `AppsLoad`: loading, loaded, empty). */
@Serializable
data class Apps(val load: Load = Load.OK, val list: List<App> = emptyList())

// ---- LoR ----

/** One of LoR's reads (`LorReads.Kept`): its state, the reply as the server sent it, the error, and when it was read. */
@Serializable
data class Read(
    val load: Load = Load.OK,
    val reply: JsonObject? = null,
    val error: String? = null,
    /** When [reply] was read, ISO; for [Load.STALE], how old it is. */
    val readAt: String? = null,
)

@Serializable
data class Lor(
    /** `GET /device/home`: the agent page, the clock block and LoR's blocks. */
    val home: Read = Read(Load.FAILED, error = "No server set"),
    /** `GET /device/tasks`: the LoR panel's Tasks and Due soon. */
    val tasks: Read = Read(Load.FAILED, error = "No server set"),
    /** `GET /device/people`: the LoR panel's People. */
    val people: Read = Read(Load.FAILED, error = "No server set"),
)

// ---- T3 ----

/** The connection's state (`T3Connection.Status`); [RETRYING] and [REJECTED] carry [T3.error]. */
@Serializable
enum class T3Status { NOT_PAIRED, STOPPED, CONNECTING, LIVE, RETRYING, REJECTED }

/** A thread's state (`ThreadState`). */
@Serializable
enum class ThreadState { WAITING_APPROVAL, WAITING_ANSWER, RUNNING, FAILED, IDLE }

/** A request a thread is blocked on (`PendingRequest`): `user_input` is a question, anything else an approval. */
@Serializable
data class Request(val id: String, val kind: String = "command") {
    val isQuestion: Boolean get() = kind == "user_input"
}

/** A thread as the shell lists it (`T3Thread`). The agent page's cards come from its [requests] and [state], as `AgentQueue` makes them. */
@Serializable
data class Thread(
    val id: String,
    val title: String,
    val project: String = "life-on-rails",
    val state: ThreadState = ThreadState.IDLE,
    val updatedAt: String = "2026-11-17T19:00:00Z",
    val lastError: String? = null,
    val requests: List<Request> = emptyList(),
    val runStartedAt: String? = null,
)

/** One question of a `user_input` request (`Question`), with its options. */
@Serializable
data class Question(
    val id: String,
    val header: String,
    val question: String,
    val options: List<Option> = emptyList(),
    val multiSelect: Boolean = false,
    val allowCustomAnswer: Boolean = true,
) {
    @Serializable
    data class Option(val label: String, val description: String = "")
}

/** A message of a thread's transcript (`ThreadMessage`). */
@Serializable
data class Message(val fromUser: Boolean, val text: String, val at: String = "2026-11-17T19:00:00Z", val streaming: Boolean = false)

@Serializable
data class T3(
    val status: T3Status = T3Status.NOT_PAIRED,
    val error: String? = null,
    /** When the last snapshot arrived, ISO; what the page shows while [RETRYING][T3Status.RETRYING] is that old. */
    val seenAt: String? = null,
    val threads: List<Thread> = emptyList(),
    /** By request id: the questions a `user_input` request asks. */
    val questions: Map<String, List<Question>> = emptyMap(),
    /** By thread id: its transcript, oldest first. */
    val messages: Map<String, List<Message>> = emptyMap(),
    /** The server paired with (`T3Store.lastServer`); empty before any pairing. */
    val server: String = "",
    /** The server's projects a thread can launch in: id to title. */
    val projects: Map<String, String> = emptyMap(),
)

// ---- The timer ----

/** The Timer tab's view of LoR's timer (`/device/state`); [UNREACHABLE] when LoR can't be read. */
@Serializable
enum class TimerState { IDLE, RUNNING, PAUSED, UNREACHABLE }

/** An intent the Timer tab offers (`/device/state`'s `recent` and `intents`): its id, title and type. */
@Serializable
data class TimerIntent(val id: String, val title: String, val type: String = "task")

/**
 * LoR's timer: running or paused under [intent] (its title, null for none) with its own [title],
 * from [start] (ISO), paused at [pausedAt] (ISO; the clock's time when left out); the intents the
 * tab offers to start under; who LoR says is signed in; and, while [TimerState.UNREACHABLE], why.
 */
@Serializable
data class Timer(
    val state: TimerState = TimerState.IDLE,
    val title: String? = null,
    val start: String? = null,
    val intent: String? = null,
    val pausedAt: String? = null,
    val recent: List<TimerIntent> = emptyList(),
    val intents: List<TimerIntent> = emptyList(),
    val login: String = "owner",
    val error: String = "connect timed out",
)

// ---- The phone: sensors and sync ----

/**
 * A sensor's row on the Sensors tab: its key (`Sensor.key`), whether it is on, and whether Android
 * allows it. [status] and [action] are the row's words and button as the app writes them; left out,
 * an allowed sensor says "Allowed." and one that isn't "Needs permission." with Allow.
 */
@Serializable
data class SensorRow(val key: String, val on: Boolean = true, val allowed: Boolean = true, val status: String? = null, val action: String? = null)

/** The last sync (`Prefs.lastSync*`, `SyncReport`): when, what it collected and sent, what waits, and its errors. */
@Serializable
data class SyncStatus(
    val lastAt: String? = null,
    val collected: Map<String, Int> = emptyMap(),
    val uploaded: Int = 0,
    val pending: Long = 0,
    val errors: List<String> = emptyList(),
)

@Serializable
data class Phone(
    val sensors: List<SensorRow> = SENSORS.map { SensorRow(it) },
    val sync: SyncStatus = SyncStatus(),
    /** By app (`pkg`): how many notifications give it a dot. */
    val dots: Map<String, Int> = emptyMap(),
    /** The home role, usage access, notification access, contacts: Android permissions LoR's home asks for, by name, and whether each is granted. */
    val permissions: Map<String, Boolean> = emptyMap(),
) {
    companion object {
        /** `Sensor`'s keys, in the app's order. */
        val SENSORS = Sensor.entries.map { it.key }
    }
}

// ---- Settings ----

@Serializable
data class Settings(
    /** `Prefs.serverUrl`; empty on a first run. */
    val serverUrl: String = "",
    val deviceName: String = "fold7",
    /** `Prefs.setupDone`: the set-up card was finished or dismissed. */
    val setupDone: Boolean = false,
    /** Icon packs installed, by label; [iconPack] is the one in use, null for the apps' own. */
    val iconPacks: List<String> = emptyList(),
    val iconPack: String? = null,
    /** The Settings tab's line under Save and Test connection: "Saved.", "Checking…", or what the test found; null before either. */
    val result: String? = null,
)

// ---- Search, the drawer and quick dial ----

@Serializable
data class Contact(val name: String, val numbers: List<String> = emptyList())

/** An app shortcut (`ShortcutHit`); [conversation] ones are people's chats, which search and quick dial list as people. */
@Serializable
data class Shortcut(val pkg: String, val label: String, val conversation: Boolean = false)

/** A system settings page search finds (`SearchTargets`' settings). */
@Serializable
data class SettingsPage(val title: String, val summary: String = "")

@Serializable
data class SearchSources(
    val contacts: List<Contact> = emptyList(),
    val shortcuts: List<Shortcut> = emptyList(),
    val settingsPages: List<SettingsPage> = emptyList(),
    /** By app ref ([App.ref]): launches counted for search's ranking (`LaunchStats`). */
    val launches: Map<String, Int> = emptyMap(),
    /** Recent searches, newest first. */
    val searches: List<String> = emptyList(),
    /** Quick dial's ranking for the hour it is (`AppOpens`): app refs, best first. */
    val quickDial: List<String> = emptyList(),
)

// ---- Where the screen is ----

/**
 * What is open over the cell. [target] names what it points at: an app ref, a folder's name, a
 * widget id, a thread id, a tab's name. [APP_TAB] is the LoR app on one of its tabs, by [target]:
 * `timer`, `sensors`, `settings`, or `t3` (Settings' T3 debug view).
 */
@Serializable
enum class Opened { NOTHING, FOLDER, APP_MENU, HOME_MENU, WIDGET_MENU, DRAWER, SEARCH, THREAD, MINIMAP, QUICK_SEND, HOME_SETTINGS, APP_TAB }

@Serializable
data class Where(
    /** The cell on screen, `[column, row]`; null is the layout's start. */
    val cell: List<Int>? = null,
    val open: Opened = Opened.NOTHING,
    val target: String? = null,
    /** How far the open list is scrolled, in dp. */
    val scroll: Int = 0,
    /** What is typed in search. */
    val typed: String = "",
    /** The app ref or widget id held in a drag. */
    val held: String? = null,
)
