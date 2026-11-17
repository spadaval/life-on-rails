package com.spadaval.lor.lab

import androidx.compose.runtime.Composable

/*
 * The launcher lab's registry: what the Version and State dropdowns list, and the screens and
 * wallpapers the phone frame offers. The page (wasmJsMain) draws whichever one the URL hash names,
 * so a link opens one exact screen.
 */

/** A screen of the Fold 7 (SM-F966U1 at density 360, read with adb): the cover screen and the inner one, in dp. */
enum class LabScreen(val id: String, val label: String, val widthDp: Int, val heightDp: Int) {
    Cover("cover", "Folded (cover)", 480, 1120),
    Inner("inner", "Unfolded (inner)", 875, 971),
}

/** The wallpapers behind the screen, each a file in the page's wallpapers/ folder. */
enum class Wallpaper(val id: String, val label: String) {
    Dark("dark", "Dark"),
    Pale("pale", "Pale"),
    Mixed("mixed", "Mixed"),
}

/** What a version is asked to draw: the state's fixture, on one screen, upright or turned on its side. */
class LabScene(val fixture: Fixture, val screen: LabScreen, val landscape: Boolean = false, val wallpaper: Wallpaper = Wallpaper.Dark) {
    val widthDp: Int get() = if (landscape) screen.heightDp else screen.widthDp
    val heightDp: Int get() = if (landscape) screen.widthDp else screen.heightDp
}

/** A version of the launcher and app: a name in the dropdown and the composable tree it draws. */
class Version(val name: String, val about: String, val draw: @Composable (LabScene) -> Unit)

object Lab {
    /**
     * Every version, `current` first. A concept is one more line here. `current` draws the app's
     * tabs and Home settings when the state opens one (`where.open`), and home for the rest
     * (lor-xdoi); the agent page and the LoR panel are placeholders on it until lor-rgx4.
     */
    val versions: List<Version> = listOf(
        Version("current", "The app as it is: home from the module the phone builds from, and the app's tabs and Home settings when the state opens one") { if (!AppScreen(it)) LabHome(it) },
        Version("kit", "The kit sheet of the look: every part, for every state") { KitSheet(it) },
        Version("gestures", "Not a launcher: the surface scripts/lab/run.mjs is proven on (a pager, a list, a tile with a menu, two panes)") { GestureSurface(it) },
    )

    val states: List<Fixture> get() = Fixtures.all

    fun version(name: String): Version? = versions.firstOrNull { it.name == name }
    fun state(name: String): Fixture? = states.firstOrNull { it.name == name }

    /**
     * The fixture a link's state part stands for: a registered name, or a one-off's JSON
     * ([Fixtures.fromJson]). A one-off that doesn't read draws the default, and the problem says why.
     */
    fun fixture(state: String): Pair<Fixture, String?> =
        if (isOneOff(state)) Fixtures.fromJson(state).fold({ it to null }, { states.first() to "The one-off state: ${it.message}" })
        else (state(state) ?: states.first()) to null

    fun isOneOff(state: String) = state.startsWith("{")
}

/**
 * One screen of the lab, as the URL hash names it: `#version/state/screen/wallpaper/orientation`,
 * for example `#current/demo/inner/pale`. What is left off at the end is the default: the cover
 * screen, the dark wallpaper, upright (`#current/demo`). `landscape` as the fifth part turns the
 * phone on its side. The state is a name, or a one-off as a JSON object
 * (`#current/{"base":"demo","t3":{"status":"NOT_PAIRED"}}/inner`), which may hold `/`.
 */
data class LabLink(
    val version: String,
    val state: String,
    val screen: LabScreen = LabScreen.Cover,
    val wallpaper: Wallpaper = Wallpaper.Dark,
    val landscape: Boolean = false,
) {
    /** The hash for this link, without `#`, leaving off what is the default at the end. */
    val hash: String
        get() = buildList {
            add(version); add(if (Lab.isOneOff(state)) encodeOneOff(state) else state)
            if (screen != LabScreen.Cover || wallpaper != Wallpaper.Dark || landscape) add(screen.id)
            if (wallpaper != Wallpaper.Dark || landscape) add(wallpaper.id)
            if (landscape) add(LANDSCAPE)
        }.joinToString("/")

    companion object {
        /** The link the page opens on with no hash: the first version and the first state. */
        fun first() = LabLink(Lab.versions.first().name, Lab.states.first().name)

        const val LANDSCAPE = "landscape"
    }
}

/** A parsed hash: the link to draw, and what was wrong with the hash if anything was (the page shows it). */
data class Parsed(val link: LabLink, val problem: String?)

/**
 * Reads a hash (with or without `#`). An empty hash is [LabLink.first]. A name that is not
 * registered is kept out of the link, which falls back to the first or the default, and named in
 * [Parsed.problem], so a mistyped link says so instead of quietly drawing something else. A state
 * that starts with `{` is a one-off's JSON, kept whole to its closing brace; [Lab.fixture] reads it.
 */
fun parseHash(raw: String, versions: List<String> = Lab.versions.map { it.name }, states: List<String> = Lab.states.map { it.name }): Parsed {
    val parts = split(decode(raw.removePrefix("#"))).filter { it.isNotEmpty() }
    val problems = mutableListOf<String>()
    fun <T> pick(i: Int, what: String, known: List<T>, id: (T) -> String, default: T): T {
        val name = parts.getOrNull(i) ?: return default
        return known.firstOrNull { id(it) == name } ?: default.also { problems += "No $what \"$name\"" }
    }
    val version = pick(0, "version", versions, { it }, versions.first())
    val state = parts.getOrNull(1)?.takeIf { Lab.isOneOff(it) } ?: pick(1, "state", states, { it }, states.first())
    val screen = pick(2, "screen", LabScreen.entries, { it.id }, LabScreen.Cover)
    val wallpaper = pick(3, "wallpaper", Wallpaper.entries, { it.id }, Wallpaper.Dark)
    val landscape = pick(4, "orientation", listOf(false, true), { if (it) LabLink.LANDSCAPE else "portrait" }, false)
    if (parts.size > 5) problems += "Too many parts: ${parts.drop(5).joinToString("/")}"
    return Parsed(LabLink(version, state, screen, wallpaper, landscape), problems.takeIf { it.isNotEmpty() }?.joinToString("; "))
}

/** Splits a decoded hash on `/`, except inside a JSON object in the state's place. */
private fun split(s: String): List<String> {
    val parts = mutableListOf<String>()
    var i = 0
    while (i <= s.length) {
        val end = if (parts.size == 1 && s.startsWith("{", i)) jsonEnd(s, i) else s.indexOf('/', i).let { if (it < 0) s.length else it }
        parts += s.substring(i, end)
        i = end + 1
        if (end == s.length) break
        if (s.getOrNull(end) != '/') i = end // a JSON object followed by something other than `/`: the next part starts there
    }
    return parts
}

/** Where the JSON object starting at [from] ends (one past its closing brace), or the string's end when it doesn't close. */
private fun jsonEnd(s: String, from: Int): Int {
    var depth = 0
    var inString = false
    var i = from
    while (i < s.length) {
        val c = s[i]
        when {
            inString && c == '\\' -> i++
            c == '"' -> inString = !inString
            !inString && (c == '{' || c == '[') -> depth++
            !inString && (c == '}' || c == ']') -> if (--depth == 0) return i + 1
        }
        i++
    }
    return s.length
}

/** A one-off's JSON as the hash writes it: compact, with what a hash can't hold as is percent-encoded. */
private fun encodeOneOff(json: String): String = buildString {
    for (c in json) when (c) {
        '%' -> append("%25"); '#' -> append("%23"); ' ' -> append("%20"); '"' -> append("%22")
        else -> append(c)
    }
}

/** Undoes the browser's percent-encoding of a hash part (a space typed as %20). */
private fun decode(s: String): String {
    if ('%' !in s) return s
    val bytes = ArrayList<Byte>()
    var i = 0
    while (i < s.length) {
        val c = s[i]
        if (c == '%' && i + 2 <= s.lastIndex) {
            val b = s.substring(i + 1, i + 3).toIntOrNull(16)
            if (b != null) { bytes += b.toByte(); i += 3; continue }
        }
        c.toString().encodeToByteArray().forEach { bytes += it }
        i++
    }
    return bytes.toByteArray().decodeToString()
}
