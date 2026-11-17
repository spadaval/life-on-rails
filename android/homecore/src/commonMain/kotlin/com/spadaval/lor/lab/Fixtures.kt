package com.spadaval.lor.lab

import com.spadaval.lor.lab.Parts.with
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/*
 * The catalogue: every named state, each a few lines over the parts. `android/homecore/STATES.md`
 * lists them; a test keeps the two the same. An item that moves a surface into this module adds
 * that surface's states here, under the names lor-q165 and its own Done when give them.
 */
object Fixtures {
    /** The default: the demo set's day at 15:00, the same made-up day the dashboard shows. */
    val demo = Fixture(
        "demo", "The demo day at 15:00: a full home, LoR's replies, T3 live with one approval and a running turn",
        clock = Parts.demoClock,
        layout = Parts.demoLayout,
        apps = Apps(Load.OK, Parts.demoApps),
        lor = Parts.demoLor,
        t3 = Parts.demoT3,
        timer = Parts.demoTimer,
        phone = Parts.demoPhone,
        settings = Parts.demoSettings,
        search = Parts.demoSearch,
    )

    /** The first start: no layout file, nothing paired, no server set, nothing used yet. */
    val empty = Fixture(
        "empty", "The first start: no layout yet, no server, T3 unpaired, nothing counted",
        clock = Parts.demoClock,
        apps = Apps(Load.OK, Parts.demoApps),
    )

    val loading = demo.copy(
        name = "loading", about = "Every read under way: the app list, LoR's three reads, T3 connecting, the timer unknown",
        apps = Apps(Load.LOADING), lor = Parts.loadingLor, t3 = Parts.connectingT3, timer = Timer(TimerState.UNREACHABLE),
    )

    val stale = demo.copy(
        name = "stale", about = "LoR last read two hours ago and timing out since; T3 retrying on what it saw 40 minutes ago",
        lor = Parts.staleLor, t3 = Parts.staleT3, phone = Parts.demoPhone.copy(sync = Parts.demoPhone.sync.copy(lastAt = "2026-11-16T22:10:00Z")),
    )

    val failed = demo.copy(
        name = "failed", about = "LoR unreachable, T3's pairing revoked, the last sync failed, two permissions gone, the layout file broken",
        layout = Parts.brokenLayout, lor = Parts.failedLor, t3 = Parts.rejectedT3, timer = Timer(TimerState.UNREACHABLE), phone = Parts.failedPhone,
    )

    val overfull = demo.copy(
        name = "overfull", about = "Everything at its most: 200 apps, five full pages, twelve cards, nine flags, long names everywhere",
        layout = Parts.fullLayout,
        apps = Apps(Load.OK, Parts.manyApps),
        lor = Parts.demoLor.copy(home = Parts.ok(Parts.homeWithFlags(9))),
        t3 = Parts.cards(12),
        phone = Parts.demoPhone.copy(dots = Parts.manyApps.take(60).associate { it.pkg to 99 }),
        search = Parts.demoSearch.copy(searches = List(20) { "search $it" }),
    )

    /** The Sensors tab on the demo day after `failed`'s sync: two errors, two permissions missing, WhatsApp off. */
    val sensorsFailed = demo.copy(
        name = "sensors-failed", about = "Sensors after a failed sync with two permissions missing",
        phone = Parts.failedPhone,
        where = Where(open = Opened.APP_TAB, target = "sensors"),
    )

    /** Home settings over the demo's file, with two more hidden apps than the demo hides. */
    val homeSettingsTabs = demo.copy(
        name = "home-settings-tabs", about = "Home settings with hidden apps and four drawer tabs",
        layout = Parts.demoLayout.with("hidden", Parts.hidden("Play Store", "Duolingo", "Kindle")),
        where = Where(open = Opened.HOME_SETTINGS),
    )

    // ---- The agent page, the LoR panel, the set-up card (lor-rgx4) ----

    /** The agent page's cell in the demo layout, left of home. */
    private val agentCell = Where(cell = listOf(0, 0))

    val firstRun = empty.copy(
        name = "first-run", about = "A first-run home with the set-up card: not yet the home app, T3 unpaired, LoR not reached at the address",
        settings = Settings(serverUrl = "http://10.0.2.2:3857"),
        // The reason as the card says it after the address: LorClient.describe's words.
        lor = Read(Load.FAILED, error = "LoR didn't answer. Is Tailscale on?").let { Lor(it, it, it) },
        phone = Parts.demoPhone.copy(permissions = mapOf("home" to false)),
    )

    val agentWaiting = demo.copy(
        name = "agent-waiting", about = "The agent page with one approval, one question, a failed turn and two LoR flags",
        lor = Parts.demoLor.copy(home = Parts.ok(Parts.homeWithFlags(2))),
        t3 = Parts.waitingT3,
        where = agentCell,
    )

    val agentT3Stale = demo.copy(
        name = "agent-t3-stale", about = "The agent page with T3 disconnected and what it saw 40 minutes ago",
        t3 = Parts.staleT3,
        where = agentCell,
    )

    val threadLong = demo.copy(
        name = "thread-long", about = "The agent page with the approval's thread open beside it and a long transcript (inner screen)",
        t3 = Parts.longT3,
        where = Where(cell = listOf(0, 0), open = Opened.THREAD, target = "t-approve"),
    )

    /** The State dropdown, in order; the first is what a link without a state opens. */
    val all: List<Fixture> = listOf(demo, empty, loading, stale, failed, overfull, sensorsFailed, homeSettingsTabs, firstRun, agentWaiting, agentT3Stale, threadLong)

    fun named(name: String): Fixture? = all.firstOrNull { it.name == name }

    // ---- JSON ----

    /** Strict: a key no part has is an error, so a typo in a one-off says so instead of vanishing. */
    val json = Json { encodeDefaults = true; prettyPrint = true }

    fun toJson(f: Fixture): String = json.encodeToString(Fixture.serializer(), f)

    /**
     * A one-off state from JSON. With `base`, it starts from that named state and the other keys
     * replace that state's, objects key by key (a map's keys are added to the base's) and anything
     * else whole: `{"base":"demo","t3":{"status":"NOT_PAIRED"}}` is the demo day with T3 unpaired.
     * Without `base`, the object is the whole fixture and what it leaves out takes the defaults, so
     * a fixture's full JSON (`toJson`, the lab's Copy JSON) reads back as itself. Without a `name`,
     * it is called `one-off`.
     */
    fun fromJson(text: String): Result<Fixture> = runCatching {
        val given = try {
            json.parseToJsonElement(text).jsonObject
        } catch (e: SerializationException) {
            throw IllegalArgumentException("not JSON: ${e.message?.lineSequence()?.first()}")
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("not a JSON object")
        }
        val baseName = given["base"]?.let { it.jsonPrimitive.contentOrNull ?: throw IllegalArgumentException("base: must be a state's name") }
        val over = JsonObject(given - "base").let { if ("name" in it) it else it.with("name", JsonPrimitive("one-off")) }
        val whole = if (baseName == null) over else {
            val base = named(baseName) ?: throw IllegalArgumentException("no state \"$baseName\" to start from")
            merge(json.encodeToJsonElement(Fixture.serializer(), base).jsonObject, over)
        }
        try {
            json.decodeFromJsonElement(Fixture.serializer(), whole)
        } catch (e: SerializationException) {
            throw IllegalArgumentException(e.message?.lineSequence()?.first() ?: "doesn't fit a fixture")
        }
    }

    private fun merge(a: JsonObject, b: JsonObject): JsonObject = JsonObject(
        a + b.mapValues { (k, v) ->
            val old: JsonElement? = a[k]
            if (old is JsonObject && v is JsonObject) merge(old, v) else v
        },
    )
}
