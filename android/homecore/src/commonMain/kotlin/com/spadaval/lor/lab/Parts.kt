package com.spadaval.lor.lab

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/*
 * The parts fixtures are made of. The demo parts are the demo set's day (seeds/demo/, DemoDay) at
 * 15:00 on 2026-11-17, with the phone's side invented to match it: the same people, the same work.
 * The other parts are each one condition of one source, so a state names the parts it changes.
 */
object Parts {
    private val json = Json

    /** Parses JSON written in a part; a typo fails the first test that builds the fixtures. */
    fun obj(text: String): JsonObject = json.parseToJsonElement(text).jsonObject

    /** [o] with [key] set to [value]. */
    fun JsonObject.with(key: String, value: JsonElement): JsonObject = JsonObject(this + (key to value))

    // ---- The clock ----

    /** The demo day's moment: 15:00 in New York, `now` and `timezone` of DemoDay.home. */
    val demoClock = Clock("2026-11-17T20:00:00.000Z", "America/New_York")

    // ---- Apps ----

    private fun app(label: String, pkg: String, cls: String = "$pkg.Main", installed: String = "2025-06-01T00:00:00Z") = App(label, pkg, cls, installed = installed)

    /** A phone's apps: the system's own, the demo life's, and two installed this week (quick dial's New). */
    val demoApps = listOf(
        app("Phone", "com.google.android.dialer"),
        app("Messages", "com.google.android.apps.messaging"),
        app("Chrome", "com.android.chrome"),
        app("Camera", "com.google.android.GoogleCamera"),
        app("Photos", "com.google.android.apps.photos"),
        app("Gmail", "com.google.android.gm"),
        app("Calendar", "com.google.android.calendar"),
        app("Maps", "com.google.android.apps.maps"),
        app("Clock", "com.google.android.deskclock"),
        app("Settings", "com.android.settings"),
        app("Play Store", "com.android.vending"),
        app("WhatsApp", "com.whatsapp"),
        app("Slack", "com.Slack"),
        app("GitHub", "com.github.android"),
        app("Linear", "app.linear"),
        app("Figma", "com.figma.mirror"),
        app("Spotify", "com.spotify.music"),
        app("YouTube", "com.google.android.youtube"),
        app("Strava", "com.strava"),
        app("Duolingo", "com.duolingo"),
        app("Kindle", "com.amazon.kindle"),
        app("Uber", "com.ubercab"),
        app("Airbnb", "com.airbnb.android"),
        app("Revolut", "com.revolut.revolut"),
        app("Notion", "notion.id"),
        app("T3 Code", "com.t3tools.t3code"),
        app("LoR", "com.spadaval.lor", "com.spadaval.lor.MainActivity"),
        app("Outlook", "com.microsoft.office.outlook").copy(work = true),
        app("Komoot", "de.komoot.android", installed = "2026-11-15T18:00:00Z"),
        app("TAP Air Portugal", "com.flytap.android", installed = "2026-11-16T21:00:00Z"),
    )

    private fun ref(label: String) = demoApps.first { it.label == label }.ref

    /** 200 apps: the demo's, then made-up ones with long names, for the drawer's and search's overfull states. */
    val manyApps = demoApps + List(200 - demoApps.size) { i ->
        val n = i + 1
        app(if (n % 7 == 0) "A very long application name that never ends $n" else "App ${('A' + n % 26)}${n}", "org.example.app$n")
    }

    // ---- The layout ----

    private fun strings(labels: List<String>) = labels.joinToString(", ") { "\"${ref(it)}\"" }

    /**
     * The demo's `layout.json`: the agent page left of home, home with the clock and two rows of
     * apps and a folder, a second page, the LoR panel, and the drawer under home, with four drawer tabs.
     */
    val demoLayout: JsonObject = obj(
        """
        {
          "version": 2,
          "active": "default",
          "hidden": ["${ref("Play Store")}"],
          "drawer": { "tabs": [
            { "name": "Quick dial", "icon": "bolt", "kind": "quick" },
            { "name": "Work", "icon": "brief", "apps": [${strings(listOf("Slack", "GitHub", "Linear", "Figma", "Outlook"))}] },
            { "name": "Travel", "icon": "compass", "apps": [${strings(listOf("Maps", "Uber", "Airbnb", "TAP Air Portugal", "Komoot"))}] },
            { "name": "All", "icon": "all", "kind": "all" }] },
          "layouts": [
            { "name": "default",
              "start": { "narrow": [1, 0] },
              "panels": [
                { "at": [0, 0], "type": "t3" },
                { "at": [1, 0], "type": "stack", "blocks": [
                    { "type": "clock" },
                    { "type": "today" },
                    { "type": "apps", "items": [
                        ${strings(listOf("Phone", "Messages", "WhatsApp", "Chrome", "Camera", "Gmail", "Calendar", "Maps"))},
                        { "folder": "Work", "apps": [${strings(listOf("Slack", "GitHub", "Linear", "Figma"))}], "pinned": true },
                        ${strings(listOf("Spotify", "Strava", "T3 Code"))}] }] },
                { "at": [2, 0], "type": "stack", "blocks": [
                    { "type": "needs-you" },
                    { "type": "apps", "items": [${strings(listOf("Photos", "YouTube", "Duolingo", "Kindle", "Revolut", "Notion", "Clock", "Settings"))}] }] },
                { "at": [3, 0], "type": "lor" },
                { "at": [1, 1], "type": "drawer" }] }
          ]
        }
        """,
    )

    /** `hidden` naming the demo apps [labels], then a package that isn't installed, which Home settings lists by its name. */
    fun hidden(vararg labels: String): JsonArray = JsonArray(labels.map { JsonPrimitive(ref(it)) } + JsonPrimitive("com.example.oldreader"))

    /** A file home can't read (`version` 3), so home shows its banner over the last good layout. */
    val brokenLayout: JsonObject = demoLayout.with("version", JsonPrimitive(3))

    /** Every stack as full as the rules let it be: five pages of 24 items, two of them folders of six, each app on home once. */
    val fullLayout: JsonObject = run {
        val refs = manyApps.drop(30).map { "\"${it.ref}\"" }
        fun page(i: Int) = refs.drop(i * 22).take(22).joinToString(", ")
        fun folder(name: String, k: Int) = """{ "folder": "$name", "apps": [${refs.drop(110 + k * 6).take(6).joinToString(", ")}] }"""
        fun items(i: Int) = "${page(i)}, ${folder(if (i == 0) "Everything I open on a Tuesday afternoon" else "Folder ${i}a", 2 * i)}, ${folder("Folder ${i}b", 2 * i + 1)}"
        obj(
            """
            { "version": 2, "active": "default", "layouts": [ { "name": "default", "start": { "narrow": [1, 0] }, "panels": [
              { "at": [0, 0], "type": "t3" },
              { "at": [1, 0], "type": "stack", "blocks": [ { "type": "clock" }, { "type": "today" }, { "type": "needs-you" }, { "type": "running" },
                { "type": "apps", "items": [ ${items(0)} ] } ] },
              ${(1..4).joinToString(",\n") { """{ "at": [${it + 1}, 0], "type": "stack", "blocks": [ { "type": "apps", "items": [ ${items(it)} ] } ] }""" }},
              { "at": [6, 0], "type": "lor" },
              { "at": [1, 1], "type": "drawer" } ] } ] }
            """,
        )
    }

    // ---- LoR ----

    fun ok(reply: JsonObject, at: String = demoClock.now) = Read(Load.OK, reply, readAt = at)

    val demoLor = Lor(ok(obj(DemoDay.home)), ok(obj(DemoDay.tasks)), ok(obj(DemoDay.people)))
    val loadingLor = Lor(Read(Load.LOADING), Read(Load.LOADING), Read(Load.LOADING))

    /** The demo's replies, last read two hours ago, every read since timed out. */
    val staleLor = Lor(
        Read(Load.STALE, obj(DemoDay.home), "timeout", "2026-11-17T18:00:00.000Z"),
        Read(Load.STALE, obj(DemoDay.tasks), "timeout", "2026-11-17T18:00:00.000Z"),
        Read(Load.STALE, obj(DemoDay.people), "timeout", "2026-11-17T18:00:00.000Z"),
    )
    val failedLor = Read(Load.FAILED, error = "Can't reach 10.0.2.2:3857 (connection refused)").let { Lor(it, it, it) }

    /** The demo's `/device/home` with [n] flagged tasks in Needs you, long notes included. */
    fun homeWithFlags(n: Int): JsonObject {
        val home = obj(DemoDay.home)
        val first = home.getValue("needs_you").jsonArray
        val more = List(maxOf(0, n - first.size)) { i ->
            buildJsonObject {
                put("id", "00000000-0000-7000-9000-${(i + 1).toString().padStart(12, '0')}")
                put("title", "Flag ${i + 1}: " + if (i % 3 == 0) "a task whose title runs on long enough to wrap over two lines and then some" else "Reply to the landlord")
                put("note", if (i % 2 == 0) "The agent's note on why this needs you, which is also long enough to wrap." else "Short note.")
                put("flagged_at", "2026-11-17T19:${(10 + i % 50).toString().padStart(2, '0')}:00.000Z")
                put("google", i % 4 == 0)
            }
        }
        return home.with("needs_you", JsonArray(first.take(n) + more))
    }

    // ---- T3 ----

    private const val AT = "2026-11-17T19:40:00Z"

    val demoThreads = listOf(
        Thread("t-approve", "Run the migration on the test database", state = ThreadState.WAITING_APPROVAL, updatedAt = "2026-11-17T19:52:00Z", requests = listOf(Request("r-approve", "command"))),
        Thread("t-tiles", "Benchmark tile cache sizes", project = "field-guide", state = ThreadState.RUNNING, updatedAt = "2026-11-17T19:58:00Z", runStartedAt = "2026-11-17T19:31:00Z"),
        Thread("t-spec", "Offline maps spec, first draft", project = "field-guide", updatedAt = "2026-11-17T18:20:00Z"),
        Thread("t-brief", "Morning brief", project = "assistant", updatedAt = "2026-11-17T12:05:00Z"),
    )

    val demoMessages = mapOf(
        "t-approve" to listOf(
            Message(true, "Add the demo's flagged_at column and run the migration on the test database.", "2026-11-17T19:41:00Z"),
            Message(false, "The migration is written: one column, nullable, no backfill. It needs to run `pnpm db:migrate` against life_on_rails_test. Approve the command?", "2026-11-17T19:52:00Z"),
        ),
    )

    val demoT3 = T3(
        T3Status.LIVE, seenAt = demoClock.now, threads = demoThreads, messages = demoMessages,
        server = "http://10.0.2.2:16021",
        projects = mapOf("p-lor" to "life-on-rails", "p-field" to "field-guide", "p-assistant" to "assistant"),
    )
    val unpairedT3 = T3(T3Status.NOT_PAIRED)
    val connectingT3 = T3(T3Status.CONNECTING)

    /** The demo's threads as last seen 40 minutes ago, the connection retrying since. */
    val staleT3 = demoT3.copy(status = T3Status.RETRYING, error = "connection reset", seenAt = "2026-11-17T19:20:00Z")
    val rejectedT3 = T3(T3Status.REJECTED, error = "The pairing was revoked on the server")

    /** A question with three options, asked by the field-guide thread. */
    private val tileQuestion = Question(
        "q1", "Tiles", "Which vector tile provider should the offline maps use?",
        listOf(Question.Option("MapTiler", "Free tier covers the beta"), Question.Option("Stadia", "Cheaper at scale"), Question.Option("Self-host", "Most work, no bill")),
    )

    /**
     * What waits on Sumeet on the agent page: the demo's approval, a question with options and a
     * turn that failed, beside the running turn and the idle threads.
     */
    val waitingT3 = demoT3.copy(
        threads = listOf(
            demoThreads[0],
            Thread("t-question", "Pick the offline tile provider", project = "field-guide", state = ThreadState.WAITING_ANSWER, updatedAt = "2026-11-17T19:50:00Z", requests = listOf(Request("r-question", "user_input"))),
            Thread("t-nightly", "Nightly: file the receipts", project = "assistant", state = ThreadState.FAILED, updatedAt = "2026-11-17T19:45:00Z", lastError = "The provider returned 529 Overloaded three times"),
        ) + demoThreads.drop(1),
        questions = mapOf("r-question" to listOf(tileQuestion)),
    )

    /** The approval's thread with a long back-and-forth: forty messages, some long, the last one still being written. */
    val longT3 = demoT3.copy(
        messages = demoMessages + ("t-approve" to List(40) { i ->
            val user = i % 2 == 0
            Message(
                fromUser = user,
                text = if (user) "Step ${i / 2 + 1}: what does the migration touch next?"
                else "Step ${i / 2 + 1}: the next table is `event_${i / 2}`. " + "It adds one nullable column and an index, with no backfill, so it runs in well under a second on the test database. ".repeat(1 + i % 3),
                // Every two minutes from 18:20, the last at 19:38, before the demo's 20:00.
                at = (Instant.parse("2026-11-17T18:20:00Z") + (i * 2).minutes).toString(),
                streaming = i == 39,
            )
        }),
    )

    /** [n] cards: approvals, questions with options, and failed turns in turn, with long titles among them. */
    fun cards(n: Int): T3 {
        val threads = List(n) { i ->
            val id = "t-card-${i + 1}"
            when (i % 3) {
                0 -> Thread(id, "Approve ${i + 1}: write the long migration that touches every table", state = ThreadState.WAITING_APPROVAL, updatedAt = AT, requests = listOf(Request("r-$id")))
                1 -> Thread(id, "Question ${i + 1}: which tile provider", state = ThreadState.WAITING_ANSWER, updatedAt = AT, requests = listOf(Request("r-$id", "user_input")))
                else -> Thread(id, "Failed ${i + 1}: the nightly run", state = ThreadState.FAILED, updatedAt = AT, lastError = "The provider returned 529 Overloaded three times")
            }
        }
        val questions = threads.filter { it.state == ThreadState.WAITING_ANSWER }.associate { t -> "r-${t.id}" to listOf(tileQuestion) }
        return demoT3.copy(threads = threads + demoThreads.drop(1), questions = questions)
    }

    // ---- The timer ----

    /** The demo's timer, idle: the intents worked on lately first, then every one LoR has. */
    val demoTimer = Timer(
        recent = listOf(TimerIntent("i-spec", "Offline maps spec"), TimerIntent("i-run", "Morning run", "habit"), TimerIntent("i-inbox", "Inbox zero")),
        intents = listOf(
            TimerIntent("i-spec", "Offline maps spec"), TimerIntent("i-run", "Morning run", "habit"), TimerIntent("i-inbox", "Inbox zero"),
            TimerIntent("i-guide", "Field guide", "project"), TimerIntent("i-spanish", "Spanish", "goal"), TimerIntent("i-taxes", "File the taxes"),
        ),
        login = "demo",
    )

    // ---- The phone ----

    val demoPhone = Phone(
        sync = SyncStatus("2026-11-17T19:52:00Z", mapOf("apps" to 41, "calls" to 2, "texts" to 9, "health" to 3, "launches" to 17), uploaded = 72),
        dots = mapOf("com.whatsapp" to 3, "com.google.android.gm" to 12, "com.Slack" to 1),
        permissions = mapOf("home" to true, "usage" to true, "notifications" to true, "contacts" to true),
    )

    /** The last sync failed twice and two sensors lost their permission. */
    val failedPhone = demoPhone.copy(
        sensors = Phone.SENSORS.map { SensorRow(it, allowed = it != "calls" && it != "location", on = it != "whatsapp") }.map {
            when (it.key) {
                "calls" -> it.copy(status = "Needs permission to read the call log.", action = "Allow")
                "location" -> it.copy(status = "Needs location permission.", action = "Allow")
                else -> it
            }
        },
        sync = SyncStatus("2026-11-17T19:52:00Z", mapOf("apps" to 41), uploaded = 0, pending = 312, errors = listOf("POST /api/v1/ingest: 502 Bad Gateway", "Health Connect: permission revoked")),
        permissions = mapOf("home" to true, "usage" to false, "notifications" to true, "contacts" to false),
    )

    // ---- Settings ----

    val demoSettings = Settings(serverUrl = "http://10.0.2.2:3857", setupDone = true, iconPacks = listOf("Arcticons", "Lawnicons"))

    // ---- Search and quick dial ----

    val demoSearch = SearchSources(
        contacts = listOf("Maya Okafor", "Daniel Reyes", "Priya Raman", "Lena Fischer", "Sam Whitfield", "Inês Carvalho", "Marcus Bell", "Grandpa Joe")
            .mapIndexed { i, n -> Contact(n, listOf("+1 917 555 01${(10 + i)}")) },
        shortcuts = listOf(
            Shortcut("com.whatsapp", "Maya Okafor", conversation = true),
            Shortcut("com.whatsapp", "Daniel Reyes", conversation = true),
            Shortcut("com.google.android.apps.maps", "Navigate home"),
            Shortcut("com.spotify.music", "Running mix"),
        ),
        settingsPages = listOf(SettingsPage("Wi-Fi", "Network & internet"), SettingsPage("Bluetooth", "Connected devices"), SettingsPage("Battery", "Usage and saver"), SettingsPage("Display", "Brightness, dark theme")),
        launches = mapOf(ref("WhatsApp") to 41, ref("Chrome") to 30, ref("Slack") to 22, ref("Maps") to 12, ref("Spotify") to 9),
        searches = listOf("maya", "dentist", "lisbon"),
        quickDial = listOf("WhatsApp", "Slack", "Chrome", "Gmail", "Maps", "Spotify", "Strava", "Calendar").map(::ref),
    )
}
