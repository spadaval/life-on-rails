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

    /** The State dropdown, in order; the first is what a link without a state opens. */
    val all: List<Fixture> = listOf(demo, empty, loading, stale, failed, overfull)

    fun named(name: String): Fixture? = all.firstOrNull { it.name == name }

    // ---- JSON ----

    /** Strict: a key no part has is an error, so a typo in a one-off says so instead of vanishing. */
    val json = Json { encodeDefaults = true; prettyPrint = true }

    fun toJson(f: Fixture): String = json.encodeToString(Fixture.serializer(), f)

    /**
     * A one-off state from JSON: an object whose `base` names the state it starts from (the default
     * when absent), and whose other keys replace that state's, objects key by key and anything else
     * whole. `{"base":"demo","t3":{"status":"NOT_PAIRED"}}` is the demo day with T3 unpaired. Without
     * a `name`, it is called `one-off`. A full fixture's JSON (`toJson`) reads back as itself.
     */
    fun fromJson(text: String): Result<Fixture> = runCatching {
        val given = try {
            json.parseToJsonElement(text).jsonObject
        } catch (e: SerializationException) {
            throw IllegalArgumentException("not JSON: ${e.message?.lineSequence()?.first()}")
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("not a JSON object")
        }
        val baseName = given["base"]?.jsonPrimitive?.contentOrNull ?: all.first().name
        val base = named(baseName) ?: throw IllegalArgumentException("no state \"$baseName\" to start from")
        val over = JsonObject(given - "base").let { if ("name" in it) it else it.with("name", JsonPrimitive("one-off")) }
        try {
            json.decodeFromJsonElement(Fixture.serializer(), merge(json.encodeToJsonElement(Fixture.serializer(), base).jsonObject, over))
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
