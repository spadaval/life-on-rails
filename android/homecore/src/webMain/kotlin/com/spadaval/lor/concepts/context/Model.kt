package com.spadaval.lor.concepts.context

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.foundation.*
import androidx.compose.ui.draw.*
import com.spadaval.lor.concepts.kit.Glyph
import com.spadaval.lor.concepts.kit.Icon
import com.spadaval.lor.concepts.kit.MenuItem
import com.spadaval.lor.lab.labAnchor
import com.spadaval.lor.lab.Load
import com.spadaval.lor.lab.T3Status
import com.spadaval.lor.lab.Wallpaper
import com.spadaval.lor.lab.LocalWallpaperKind
import com.spadaval.lor.ui.LocalClock
import com.spadaval.lor.agent.T3Card
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.BasicText
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi

import com.spadaval.lor.concepts.kit.ConceptStore
import com.spadaval.lor.lor.LorHome
import com.spadaval.lor.lor.LorPeople
import com.spadaval.lor.lor.LorTasks
import com.spadaval.lor.t3.T3Thread
import com.spadaval.lor.t3.ThreadState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/*
 * What a context is and what belongs to it, as pure reads of the store (and of the links he has
 * accepted here), so Undo, a timer change or a fold redraws it all. The dashboard's model
 * (prototype/dashboard/src/directions/context/lib/model.ts) on the phone's fixture.
 *
 * A context is the thing being worked on: a project or a goal (its tasks), a person (what names or
 * waits on them), a slot (the next fixed event and the gap before it), a timer label that belongs to
 * nothing yet, an unnamed timer, or Loose ends (tasks under nothing). The brief's risk is that some
 * things belong to no context; Loose ends, the unlinked label and the unnamed timer are where those
 * live, and the agent's suggested links move them out.
 *
 * The phone's replies don't carry everything the dashboard's snapshot does, so a few tables below
 * are made up from the demo set (by title), and say so: a project's goal, the pages, the week's
 * hours, estimates, the agent's suggested links.
 */
enum class Kind(val word: String) { PROJECT("Project"), GOAL("Goal"), PERSON("Person"), SLOT("Next fixed event"), LOOSE("Under nothing yet"), LABEL("Timer label"), UNNAMED("Timer") }

data class Ctx(val kind: Kind, val title: String, val event: LorHome.Event? = null) {
    val key: String get() = when (kind) { Kind.LOOSE -> "loose"; Kind.UNNAMED -> "unnamed"; else -> "${kind.name.lowercase()}:$title" }
}

val LOOSE = Ctx(Kind.LOOSE, "Loose ends")
val UNNAMED = Ctx(Kind.UNNAMED, "An unnamed timer")

/** Why the context on Now is the one shown. */
enum class Why(val words: String) { TIMER("The timer is running here"), ONE("The one thing, from this morning's brief"), RECENT("Where you left off"), CHOSEN("Chosen by you"), NONE("Nothing is chosen") }

// ---- Made up from the demo set, by title: what the phone's replies don't carry ----

/** A project's goal (the snapshot's parent links; `/device/tasks` gives only the nearest parent). */
val GOAL_OF = mapOf(
    "2026 taxes" to "Get the finances in order",
    "Lisbon trip in April" to "Hold a conversation in Portuguese",
    "Offline maps" to "Ship the Field Guide app",
    "Public beta" to "Ship the Field Guide app",
    "Repaint the kitchen" to "Keep the house running",
    "Training block two" to "Run a half marathon in March",
    "Walnut bookshelf" to "Build the cabin bookshelf",
)

val GOALS = GOAL_OF.values.toSet() + setOf("Be a good friend")

/** A goal's hours this week and its target (the snapshot's `week_hours`). No target, or no hours, is "no data yet". */
val WEEK = mapOf(
    "Ship the Field Guide app" to (18.7 to 15.0),
    "Hold a conversation in Portuguese" to (1.5 to 3.0),
    "Run a half marathon in March" to (2.8 to 5.0),
    "Build the cabin bookshelf" to (1.3 to 2.0),
)

/** Which context a timer label counts toward. A label not here belongs to nothing yet. */
val LABEL_CTX = mapOf(
    "Offline maps spec" to "Offline maps",
    "Field guide" to "Ship the Field Guide app",
    "Morning run" to "Training block two",
    "File the taxes" to "2026 taxes",
)

/** A context's page in Notes: the snapshot's pages, shortened. */
class Page(val slug: String, val intro: String, val sections: List<Pair<String, List<String>>>, val edited: String)

val PAGES = mapOf(
    "Offline maps" to Page(
        "projects/offline-maps", "Download a region's trails and tiles before a hike.",
        listOf("Open questions" to listOf("Which tile provider: MapTiler, Stadia or self-host.", "How big can a region be before the download is annoying? Benchmark pending."), "Decisions" to listOf("Regions, not whole states.", "Vector tiles, styled on the phone.")),
        "2 days ago",
    ),
    "Ship the Field Guide app" to Page(
        "goals/field-guide", "A field guide to the trees and birds of the Bay Area that works with no signal.",
        listOf("Now" to listOf("Offline maps: the spec is due tomorrow.", "Beta testers: twenty people by the end of the month."), "Done means" to listOf("Public beta in the store, a hundred weekly users, crash-free over 99%.")),
        "last week",
    ),
    "Hold a conversation in Portuguese" to Page(
        "goals/portuguese", "European Portuguese, for the Lisbon trip in April.",
        listOf("How" to listOf("Daily lesson, twenty minutes.", "Weekly conversation with Inês on Tuesdays.", "Unit 8 is the past tense; it is slow going.")),
        "4 days ago",
    ),
    "Lisbon trip in April" to Page(
        "topics/lisbon-trip", "Six nights in April, for the Portuguese goal and a rest.",
        listOf("Plan" to listOf("Fly Friday 17 April, back Thursday 23 April.", "Stay in Alfama: the tiled flat with the terrace.", "One day trip to Sintra; one evening of fado, not two."), "Flights" to listOf("Nonstop SFO to LIS on the Friday, about \$780. Book this week.")),
        "yesterday",
    ),
    "Sam Whitfield" to Page(
        "people/sam-whitfield", "Housemate for three years on Valencia Street.",
        listOf("Notes" to listOf("Birthday this week. Dinner for eight; likes the Burmese place on Clement.", "Gift idea: the Japanese whetstone set.")),
        "3 days ago",
    ),
)

/** People a context's page names, with why (the snapshot's page links); the phone's people reply lists only those past their cadence. */
val PAGE_PEOPLE = mapOf(
    "Offline maps" to listOf(Triple("Lena Brandt", "waiting: the icon sizes", true), Triple("Daniel Reyes", "on vector tile providers", false)),
    "Ship the Field Guide app" to listOf(Triple("Lena Brandt", "waiting: the icon sizes", true)),
    "Public beta" to listOf(Triple("Lena Brandt", "has the tester list ready", false)),
)

/** Minutes a task should take, as the agent would estimate it: "what fits in the gap" needs some. */
val ESTIMATE = mapOf(
    "Renew the parking permit" to 15, "Return the library books" to 20, "Pick up dry cleaning" to 20, "Buy a birthday card for Sam" to 15,
    "Fix the dripping bathroom tap" to 45, "Finish unit 8 of the course" to 40, "Write release notes for beta 0.4" to 60,
    "Move the emergency fund to the high-yield account" to 15, "Choose the kitchen paint colour" to 30, "Draft the offline maps spec" to 120,
    "Fix the crash on rotate in the map view" to 90, "Invite twenty beta testers" to 30, "Book flights to Lisbon" to 30, "Book a sports massage" to 10,
)

/** Where the agent would put tasks that sit under nothing. He accepts one with Link, which can be undone. */
val SUGGEST = mapOf(
    "Renew the parking permit" to "Keep the house running",
    "Pick up dry cleaning" to "Keep the house running",
    "Return the library books" to "Keep the house running",
    "Buy a birthday card for Sam" to "Be a good friend",
    "Back up the laptop" to "Ship the Field Guide app",
)

/** Where the agent would put a timer label that counts toward nothing. */
val SUGGEST_LABEL = mapOf("Inbox zero" to "Keep the house running")

// ---- Words ----

private val STOP = setOf("the", "and", "for", "with", "from", "into", "that", "this", "your", "before", "first", "draft", "pick", "find")

fun words(s: String): Set<String> = s.lowercase().split(Regex("[^\\p{L}0-9]+")).filter { it.length >= 4 && it !in STOP }.toSet()

/** Whether [text] names [name]: the full name, or the first name as a word. */
fun mentions(text: String, name: String): Boolean {
    val first = name.substringBefore(' ')
    val t = text.lowercase()
    return listOf(name, first).any { w -> Regex("(^|[^\\p{L}])${Regex.escape(w.lowercase())}('s)?($|[^\\p{L}])").containsMatchIn(t) }
}

// ---- The model ----

/** A row of a context's agent work: a thread the agent works in, or a task given to it that has no thread yet. */
class AgentWork(val title: String, val thread: T3Thread?, val task: LorTasks.Task?)

/** A person a context waits on or names, and why. */
class PersonLink(val name: String, val why: String, val waiting: Boolean, val person: LorPeople.Person?)

/** Something waiting outside the context shown: a flag or a task due, and the context it lives in. */
class Elsewhere(val key: String, val title: String, val note: String?, val flag: Boolean, val task: LorTasks.Task?, val home: Ctx)

/**
 * The contexts of a store, with the links accepted on this phone ([links]: task id to context title;
 * [labelLinks]: timer label to context title).
 */
class Model(val store: ConceptStore, val links: Map<String, String>, val labelLinks: Map<String, String>) {
    fun parentOf(t: LorTasks.Task): String? = links[t.id] ?: t.parent

    fun ctxNamed(title: String): Ctx = when {
        title in GOALS -> Ctx(Kind.GOAL, title)
        else -> Ctx(Kind.PROJECT, title)
    }

    /** The context a task lives in. */
    fun homeOf(t: LorTasks.Task): Ctx = parentOf(t)?.let(::ctxNamed) ?: LOOSE

    fun taskById(id: String): LorTasks.Task? = store.tasks.firstOrNull { it.id == id }

    fun homeOfId(id: String): Ctx = taskById(id)?.let(::homeOf) ?: store.home?.needsYou?.firstOrNull { it.id == id }?.let { n -> store.tasks.firstOrNull { it.title == n.title }?.let(::homeOf) } ?: LOOSE

    /** The context a timer label counts toward: a context's own title, a linked one, a task's home; else the label alone. */
    fun ctxOfLabel(label: String?): Ctx {
        // A timer with no label, named here: its time goes to the context he picked ("" keys it).
        label ?: return labelLinks[""]?.let(::ctxNamed) ?: UNNAMED
        (labelLinks[label] ?: LABEL_CTX[label])?.let { return ctxNamed(it) }
        if (label in GOALS || store.tasks.any { parentOf(it) == label }) return ctxNamed(label)
        store.tasks.firstOrNull { it.title == label }?.let { return homeOf(it) }
        store.people?.let { p -> (p.overdue + p.birthdays).firstOrNull { it.name == label } }?.let { return Ctx(Kind.PERSON, it.name) }
        return Ctx(Kind.LABEL, label)
    }

    /** The task the agent's one thing names: the open task sharing most words with it. */
    fun oneThing(): LorTasks.Task? {
        val one = store.home?.one?.text ?: return null
        val w = words(one)
        return store.tasks.filter { store.done[it.id] != true }.maxByOrNull { (words(it.title) intersect w).size }?.takeIf { (words(it.title) intersect w).size >= 2 }
    }

    /** The context Now opens on, and why: the running timer's, else the one thing's, else where he left off. */
    fun current(): Pair<Ctx, Why> {
        store.timer?.let { return ctxOfLabel(it.label) to Why.TIMER }
        oneThing()?.let { return homeOf(it) to Why.ONE }
        store.fixture.timer.recent.firstOrNull()?.let { return ctxOfLabel(it.title) to Why.RECENT }
        return LOOSE to Why.NONE
    }

    /** The open tasks a context holds, flagged first, then by due date. */
    fun tasksOf(c: Ctx): List<LorTasks.Task> {
        val all = store.tasks
        val mine = when (c.kind) {
            Kind.PROJECT -> all.filter { parentOf(it) == c.title }
            Kind.GOAL -> all.filter { parentOf(it) == c.title || GOAL_OF[parentOf(it)] == c.title }
            Kind.PERSON -> all.filter { it.waitingOn == c.title || mentions(it.title, c.title) }
            Kind.SLOT -> all.filter { t -> peopleIn(c.title).any { mentions(t.title, it) } }
            Kind.LOOSE -> all.filter { parentOf(it) == null }
            Kind.LABEL, Kind.UNNAMED -> emptyList()
        }
        val flagged = flaggedTitles()
        return mine.sortedWith(compareBy<LorTasks.Task> { if (it.title in flagged || it.flagged) 0 else 1 }.thenBy { it.due?.toString() ?: "9999" })
    }

    private fun flaggedTitles() = store.home?.needsYou.orEmpty().map { it.title }.toSet()

    /** People the fixture knows by name, for an event's title. */
    private fun peopleIn(text: String): List<String> =
        (store.fixture.search.contacts.map { it.name } + store.people?.let { it.overdue + it.birthdays }.orEmpty().map { it.name }).distinct().filter { mentions(text, it) }

    /** The open tasks that are his, not the agent's: what the surface lists and leads with. */
    fun mineOf(c: Ctx): List<LorTasks.Task> = tasksOf(c).filter { !it.agent }

    /** What the context gave the agent: threads that match it, then its tasks given to the agent that have no thread yet. */
    fun agentOf(c: Ctx): List<AgentWork> {
        val tasks = tasksOf(c)
        if (c.kind == Kind.LABEL || c.kind == Kind.UNNAMED) return emptyList()
        val vocab = words(c.title) + tasks.flatMap { words(it.title) }
        val threads = store.threads.filter { t -> tasks.any { it.title == t.title } || (c.kind != Kind.LOOSE && (words(t.title) intersect vocab).size >= 2) }
        val queued = tasks.filter { it.agent && threads.none { th -> th.title == it.title } }
        return threads.sortedBy { if (it.state == ThreadState.RUNNING) 0 else 1 }.map { AgentWork(it.title, it, tasks.firstOrNull { x -> x.title == it.title }) } +
            queued.map { AgentWork(it.title, null, it) }
    }

    /** The context a thread belongs to, if any: the first project or goal whose work it is. */
    fun ctxOfThread(t: T3Thread): Ctx? = allProjects().sortedBy { if (it.kind == Kind.PROJECT) 0 else 1 }.firstOrNull { c -> agentOf(c).any { it.thread?.id == t.id } }

    /** Who the context waits on, then who its tasks and its page name. */
    fun peopleOf(c: Ctx): List<PersonLink> {
        val out = linkedMapOf<String, PersonLink>()
        val known = store.people?.let { it.overdue + it.birthdays }.orEmpty()
        fun add(name: String, why: String, waiting: Boolean) { if (name != c.title && name !in out) out[name] = PersonLink(name, why, waiting, known.firstOrNull { it.name == name }) }
        val tasks = tasksOf(c)
        tasks.forEach { t -> t.waitingOn?.let { add(it, "waiting: ${t.title}", true) } }
        PAGE_PEOPLE[c.title].orEmpty().forEach { (n, why, w) -> add(n, why, w) }
        tasks.forEach { t -> (known.map { it.name } + store.fixture.search.contacts.map { it.name }).distinct().filter { mentions(t.title, it) }.forEach { add(it, t.title, false) } }
        if (c.kind == Kind.SLOT) peopleIn(c.title).forEach { add(it, "at this event", false) }
        return out.values.toList()
    }

    fun pageOf(c: Ctx): Page? = PAGES[c.title]

    /** The goal whose week a context shows: itself, or its project's goal. */
    fun goalOf(c: Ctx): String? = when (c.kind) { Kind.GOAL -> c.title; Kind.PROJECT -> GOAL_OF[c.title]; else -> null }

    /** Every project and goal with an open task, goals first. */
    fun allProjects(): List<Ctx> {
        val parents = store.tasks.mapNotNull(::parentOf).distinct()
        val goals = (parents.filter { it in GOALS } + parents.mapNotNull { GOAL_OF[it] }).distinct().sorted()
        return goals.flatMap { g -> listOf(Ctx(Kind.GOAL, g)) + parents.filter { GOAL_OF[it] == g }.sorted().map { Ctx(Kind.PROJECT, it) } } +
            parents.filter { it !in GOALS && GOAL_OF[it] == null }.sorted().map { Ctx(Kind.PROJECT, it) }
    }

    fun peopleContexts(): List<Ctx> = (store.tasks.mapNotNull { it.waitingOn } + store.people?.let { it.overdue + it.birthdays }.orEmpty().map { it.name }).distinct().map { Ctx(Kind.PERSON, it) }

    /** The next fixed event after [now], with the time free before it. */
    fun next(now: Instant): Pair<LorHome.Event, Duration>? = store.home?.day?.events?.filter { it.start > now }?.minByOrNull { it.start }?.let { it to (it.start - now) }

    fun slots(now: Instant): List<Ctx> = store.home?.day?.events.orEmpty().filter { it.end > now }.map { Ctx(Kind.SLOT, it.title, it) }

    /** Up to three open tasks that fit [gap]: this context's first, then the soonest due. */
    fun fits(here: Ctx, gap: Duration): List<Pair<LorTasks.Task, Int>> {
        val mine = tasksOf(here).map { it.id }.toSet()
        return store.tasks.filter { store.done[it.id] != true && !it.agent && it.waitingOn == null && (ESTIMATE[it.title] ?: Int.MAX_VALUE).minutes <= gap }
            .sortedWith(compareBy<LorTasks.Task> { if (it.id in mine) 0 else 1 }.thenBy { it.due?.toString() ?: "9999" })
            .take(3).map { it to ESTIMATE.getValue(it.title) }
    }

    /** What waits on him outside [here]: LoR's flags first, then tasks due today or late, each with its own context. */
    fun elsewhere(here: Ctx): List<Elsewhere> {
        val mine = tasksOf(here).map { it.title }.toSet()
        val flags = store.home?.needsYou.orEmpty().filter { it.title !in mine }.map { n ->
            val t = store.tasks.firstOrNull { it.title == n.title }
            Elsewhere("flag:${n.id}", n.title, n.note, true, t, t?.let(::homeOf) ?: LOOSE)
        }
        val flagged = store.home?.needsYou.orEmpty().map { it.title }.toSet()
        val due = store.tasks.filter { t -> t.due != null && t.due!! <= store.today && t.title !in flagged && t.title !in mine }
            .sortedBy { it.due }.map { Elsewhere("task:${it.id}", it.title, null, false, it, homeOf(it)) }
        return flags + due
    }

    /** The contexts for the bar at the top of Now: the current one, then the recent and those with something flagged, then Loose ends. */
    fun recent(current: Ctx): List<Ctx> {
        val fromTimer = store.fixture.timer.recent.map { ctxOfLabel(it.title) }.filter { it.kind == Kind.PROJECT || it.kind == Kind.GOAL }
        val fromFlags = store.home?.needsYou.orEmpty().mapNotNull { n -> store.tasks.firstOrNull { it.title == n.title }?.let(::homeOf) }
        return (listOf(current) + fromTimer + fromFlags).distinctBy { it.key }.filter { it.kind != Kind.LOOSE }.take(4) + LOOSE
    }

    fun byKey(key: String, now: Instant): Ctx? = when {
        key == "loose" -> LOOSE
        key == "unnamed" -> UNNAMED
        key.startsWith("slot:") -> slots(now).firstOrNull { it.key == key } ?: Ctx(Kind.SLOT, key.removePrefix("slot:"))
        else -> {
            val (k, t) = key.split(':', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
            Kind.entries.firstOrNull { it.name.lowercase() == k }?.let { Ctx(it, t) }
        }
    }
}
