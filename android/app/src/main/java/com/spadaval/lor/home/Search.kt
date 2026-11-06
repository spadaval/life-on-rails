package com.spadaval.lor.home

import java.text.Normalizer
import java.util.Locale

/*
 * Search (launcher polish 5.1): the matcher, and ranking across kinds of result. No Android here,
 * so the JVM tests reach all of it.
 *
 * Read for D1, ideas only:
 * - Lawnchair (Apache-2.0, 15-dev, `search/algorithms/engine/provider/apps/AppMatcher.kt`): rules
 *   tried in order, the first that holds giving the rank, with initials ("gm" for Google Maps) and
 *   ordered word prefixes ("goo map") among them.
 * - Kvaesitso (GPL-3.0, `core/base/.../search/ResultScore.kt`, `app/ui/.../search/SearchVM.kt`):
 *   the kind of match decides the order before use does; typos only from a minimum length; labels
 *   prepared once; kinds drawn as blocks, never interleaved, and the calculator never Enter's
 *   target; a search checks for cancellation every 8 items.
 * - Launcher3 / Lawnchair (`StringMatcherUtility.java`): word breaks (a capital after a lower-case
 *   letter, a digit after a non-digit), so "tube" finds YouTube. Kvaesitso's `StringNormalizer`:
 *   lower case with accents stripped.
 * The typo distance is the textbook optimal string alignment (Damerau–Levenshtein without
 * repeated edits).
 */

/** A name prepared once for matching: normalised, a compact form, and where its words start. */
class Prepared(
    /** Lower case, accents stripped. */
    val norm: String,
    /** [norm] with everything but letters and digits removed. */
    val compact: String,
    /** Where each word starts in [norm]; the first is 0. */
    val starts: IntArray,
    /** Where each word starts in [compact]. */
    val compactStarts: IntArray,
    /** Each word's first character. */
    val initials: String,
    /** Each word, letters and digits only. */
    val words: List<String>,
)

/** A query prepared once per search. */
class Query(text: String) {
    val norm: String = Match.normalize(text.trim()).replace(SPACES, " ")
    val compact: String = norm.filter { it.isLetterOrDigit() }
    val words: List<String> = norm.split(' ').map { w -> w.filter { it.isLetterOrDigit() } }.filter { it.isNotEmpty() }

    private companion object {
        val SPACES = Regex("\\s+")
    }
}

object Match {
    /** The tiers, best first; see [tier]. */
    const val PREFIX = 0
    const val WORD = 1
    const val WORDS = 2
    const val INITIALS = 3
    const val CONTAINS = 4
    const val IN_ORDER = 5
    const val TYPO = 6

    fun normalize(s: String): String =
        Normalizer.normalize(s.lowercase(Locale.ROOT), Normalizer.Form.NFD).replace(MARKS, "")

    private val MARKS = Regex("\\p{Mn}+")

    fun prepare(name: String): Prepared {
        val norm = StringBuilder()
        val compact = StringBuilder()
        val starts = mutableListOf<Int>()
        val compactStarts = mutableListOf<Int>()
        for (i in name.indices) {
            if (i == 0 || isBreak(name, i)) {
                starts.add(norm.length)
                compactStarts.add(compact.length)
            }
            val n = normalize(name[i].toString())
            norm.append(n)
            n.filterTo(compact) { it.isLetterOrDigit() }
        }
        val normS = norm.toString()
        val words = starts.indices.map { k ->
            val end = if (k + 1 < starts.size) starts[k + 1] else normS.length
            normS.substring(starts[k], end).filter { it.isLetterOrDigit() }
        }
        return Prepared(
            normS, compact.toString(), starts.toIntArray(), compactStarts.toIntArray(),
            words.mapNotNull { it.firstOrNull() }.joinToString(""), words,
        )
    }

    /**
     * How [q] matches [p], best first, or null for no match:
     * 0 the name starts with it (or the compact forms do); 1 a later word starts with it;
     * 2 each of its words starts a word of the name, in order; 3 it starts the name's initials
     * (2+ characters, one word); 4 the name contains it (3+); 5 its characters are in the name in
     * order, the first at a word start (2+); 6 a typo (4+): one edit, two from 8 characters.
     */
    fun tier(q: Query, p: Prepared): Int? {
        val n = q.norm
        if (n.isEmpty()) return null
        if (p.norm.startsWith(n) || (q.compact.isNotEmpty() && p.compact.startsWith(q.compact))) return PREFIX
        for (k in 1 until p.starts.size) if (p.norm.startsWith(n, p.starts[k])) return WORD
        if (q.words.size > 1 && wordsInOrder(q.words, p.words)) return WORDS
        if (q.words.size == 1 && q.compact.length >= 2 && p.initials.startsWith(q.compact)) return INITIALS
        if (n.length >= 3 && p.norm.contains(n)) return CONTAINS
        if (q.compact.length >= 2 && inOrder(q.compact, p)) return IN_ORDER
        if (q.compact.length >= 4 && typo(q.compact, p)) return TYPO
        return null
    }

    /** The best tier over [names], or null. */
    fun best(q: Query, names: List<Prepared>): Int? = names.mapNotNull { tier(q, it) }.minOrNull()

    private fun wordsInOrder(qw: List<String>, words: List<String>): Boolean {
        var at = 0
        for (w in qw) {
            while (at < words.size && !words[at].startsWith(w)) at++
            if (at == words.size) return false
            at++
        }
        return true
    }

    private fun inOrder(c: String, p: Prepared): Boolean {
        for (s in p.compactStarts) {
            if (p.compact.getOrNull(s) != c[0]) continue
            var i = 1
            var j = s + 1
            while (i < c.length && j < p.compact.length) {
                if (p.compact[j] == c[i]) i++
                j++
            }
            if (i == c.length) return true
        }
        return false
    }

    private fun typo(c: String, p: Prepared): Boolean {
        val most = if (c.length >= 8) 2 else 1
        return p.compactStarts.distinct().any { s -> prefixDistance(c, p.compact, s) <= most }
    }

    /**
     * The optimal-string-alignment distance from [q] to the closest prefix of [t] (from [from])
     * whose length is within one of [q]'s.
     */
    internal fun prefixDistance(q: String, t: String, from: Int = 0): Int {
        val m = minOf(t.length - from, q.length + 1)
        if (m < q.length - 1) return Int.MAX_VALUE
        // d[i][j]: q's first i characters against t's first j (from [from]).
        val d = Array(q.length + 1) { IntArray(m + 1) }
        for (i in 0..q.length) d[i][0] = i
        for (j in 0..m) d[0][j] = j
        for (i in 1..q.length) for (j in 1..m) {
            val cost = if (q[i - 1] == t[from + j - 1]) 0 else 1
            var v = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost)
            if (i > 1 && j > 1 && q[i - 1] == t[from + j - 2] && q[i - 2] == t[from + j - 1]) v = minOf(v, d[i - 2][j - 2] + 1)
            d[i][j] = v
        }
        return (maxOf(0, q.length - 1)..m).minOf { d[q.length][it] }
    }

    /** A word break at [i]: after a space or symbol, a digit after a non-digit, a capital after a lower-case letter or digit. */
    private fun isBreak(s: String, i: Int): Boolean {
        val prev = s[i - 1]
        val cur = s[i]
        if (!cur.isLetterOrDigit()) return false
        return !prev.isLetterOrDigit() ||
            (cur.isDigit() && !prev.isDigit()) ||
            (cur.isUpperCase() && (prev.isLowerCase() || prev.isDigit()))
    }
}

/** The kinds of result, in the order their blocks are drawn when their best matches tie. */
enum class Kind(val title: String, val minLength: Int, val most: Int) {
    APPS("Apps", 1, Int.MAX_VALUE),
    SHORTCUTS("Shortcuts", 2, 5),
    SETTINGS("Settings", 2, 5),
    CONTACTS("Contacts", 2, 5),
}

/** One thing search can find: [value] is what the drawer opens (an app, a shortcut, …). */
class Found<out T>(
    val kind: Kind,
    val key: String,
    val names: List<Prepared>,
    val value: T,
    /** A contact's phone numbers' digits, matched by a query of 3 or more digits (5.6). */
    val digits: List<String> = emptyList(),
)

/** A quick answer (5.4): [value] for [input]. It opens nothing. */
data class Answer(val value: String, val input: String)

class Block<out T>(val kind: Kind, val entries: List<Found<T>>, val best: Int)

class Results<out T>(val query: String, val answer: Answer?, val blocks: List<Block<T>>) {
    /** What Go opens: the first result drawn that opens something (never the answer). */
    val go: Found<T>? get() = blocks.firstOrNull()?.entries?.firstOrNull()

    /** Nothing matched, so the drawer offers the web, the store and the agent (3.4). */
    val nothing: Boolean get() = blocks.isEmpty()

    companion object {
        fun <T> empty(query: String) = Results<T>(query, null, emptyList())
    }
}

object Search {
    /**
     * Searches [sources] for [query]. Typo matches show only when nothing matched better. Within a kind: by tier, then by [weight] (apps' launch
     * weights; higher first), then as the source lists them. Kinds are blocks, ordered by their
     * best tier, ties in [Kind]'s order; [answer] comes first. [check] is called every 8 items, so
     * a cancelled search stops early.
     */
    fun <T> run(
        query: String,
        sources: List<Found<T>>,
        weight: (Found<T>) -> Double = { 0.0 },
        answer: (String) -> Answer? = { null },
        check: () -> Unit = {},
    ): Results<T> {
        val q = Query(query)
        if (q.norm.isEmpty()) return Results.empty(query)
        val digits = q.norm.filter { it.isDigit() }.takeIf { it.length >= 3 && it.length == q.compact.length }
        val found = ArrayList<Triple<Int, Int, Found<T>>>()
        sources.forEachIndexed { i, e ->
            if (i % 8 == 0) check()
            if (q.norm.length < e.kind.minLength) return@forEachIndexed
            val tier = Match.best(q, e.names)
                ?: if (digits != null && e.digits.any { it.contains(digits) }) Match.CONTAINS else null
            if (tier != null) found.add(Triple(tier, i, e))
        }
        // A typo is a guess: shown only when nothing matched better, so "cafe" doesn't bring Camera.
        if (found.any { it.first < Match.TYPO }) found.removeAll { it.first == Match.TYPO }
        val blocks = found.groupBy { it.third.kind }.map { (kind, list) ->
            val sorted = list.sortedWith(compareBy<Triple<Int, Int, Found<T>>> { it.first }.thenByDescending { weight(it.third) }.thenBy { it.second })
            Block(kind, sorted.take(kind.most).map { it.third }, sorted.first().first)
        }.sortedWith(compareBy({ it.best }, { it.kind.ordinal }))
        return Results(query, answer(query), blocks)
    }
}

/*
 * Shortcuts and conversations (launcher polish 5.2). Kvaesitso (GPL-3.0,
 * `data/appshortcuts/.../AppShortcutRepository.kt`) keeps every shortcut in memory, queried again on
 * `onShortcutsChanged` and package changes; Lawnchair (Apache-2.0, `ShortcutSearchProvider.kt`)
 * shows an app's shortcuts when search narrows to that one app.
 */

/** A messaging app's chat: a shortcut in the conversation category, or one it cached. */
fun isConversation(categories: Set<String>?, cached: Boolean): Boolean =
    cached || categories?.contains(CONVERSATION) == true

/** `ShortcutInfo.SHORTCUT_CATEGORY_CONVERSATION`. */
const val CONVERSATION = "android.shortcut.conversation"

/** An app a shortcut can belong to: its package, activity class and profile. */
data class Owner(val pkg: String, val cls: String, val user: String)

/** The app [owners] lists for a shortcut of [pkg] in [user]: the [activity] it names, else the package's first. Null when the drawer doesn't list it (hidden, or not installed). */
fun <A> ownerOf(owners: List<Pair<Owner, A>>, pkg: String, activity: String?, user: String): A? {
    val mine = owners.filter { it.first.pkg == pkg && it.first.user == user }
    return (mine.firstOrNull { it.first.cls == activity } ?: mine.firstOrNull())?.second
}

/**
 * When the apps block holds one app ([sameApp] counts its copies in other profiles once), the
 * first's shortcuts ([itsShortcuts], its menu's) lead the shortcuts block, matched or not, then
 * the matched ones, without repeats, to the kind's limit.
 */
fun <T> Results<T>.withShortcutsOfOneApp(sameApp: (Found<T>) -> Any = { it.key }, itsShortcuts: (Found<T>) -> List<Found<T>>): Results<T> {
    val apps = blocks.firstOrNull { it.kind == Kind.APPS }?.takeIf { b -> b.entries.distinctBy(sameApp).size == 1 } ?: return this
    val extra = itsShortcuts(apps.entries[0])
    if (extra.isEmpty()) return this
    val old = blocks.firstOrNull { it.kind == Kind.SHORTCUTS }
    val entries = (extra + old?.entries.orEmpty()).distinctBy { it.key }.take(Kind.SHORTCUTS.most)
    val block = Block(Kind.SHORTCUTS, entries, minOf(old?.best ?: apps.best, apps.best))
    return Results(query, answer, (blocks.filter { it.kind != Kind.SHORTCUTS } + block).sortedWith(compareBy({ it.best }, { it.kind.ordinal })))
}
