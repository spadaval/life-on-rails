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

/** What a version is asked to draw: the state's fixture, on one screen. */
class LabScene(val fixture: Fixture, val screen: LabScreen)

/** A version of the launcher and app: a name in the dropdown and the composable tree it draws. */
class Version(val name: String, val about: String, val draw: @Composable (LabScene) -> Unit)

object Lab {
    /**
     * Every version, `current` first. A concept is one more line here. Until the screens move into
     * this module (lor-xdoi, lor-rgx4, lor-t8lq), `current` draws the kit sheet.
     */
    val versions: List<Version> = listOf(
        Version("current", "The app as it is: for now the kit sheet of its look") { KitSheet(it) },
    )

    val states: List<Fixture> get() = Fixtures.all

    fun version(name: String): Version? = versions.firstOrNull { it.name == name }
    fun state(name: String): Fixture? = states.firstOrNull { it.name == name }
}

/**
 * One screen of the lab, as the URL hash names it: `#version/state/screen/wallpaper`, for example
 * `#current/home-full/inner/pale`. The screen and the wallpaper may be left off for the cover
 * screen on the dark wallpaper (`#current/home-full`); the wallpaper alone for the dark one.
 */
data class LabLink(
    val version: String,
    val state: String,
    val screen: LabScreen = LabScreen.Cover,
    val wallpaper: Wallpaper = Wallpaper.Dark,
) {
    /** The hash for this link, without `#`, leaving off what is the default at the end. */
    val hash: String
        get() = buildList {
            add(version); add(state)
            if (screen != LabScreen.Cover || wallpaper != Wallpaper.Dark) add(screen.id)
            if (wallpaper != Wallpaper.Dark) add(wallpaper.id)
        }.joinToString("/")

    companion object {
        /** The link the page opens on with no hash: the first version and the first state. */
        fun first() = LabLink(Lab.versions.first().name, Lab.states.first().name)
    }
}

/** A parsed hash: the link to draw, and what was wrong with the hash if anything was (the page shows it). */
data class Parsed(val link: LabLink, val problem: String?)

/**
 * Reads a hash (with or without `#`). An empty hash is [LabLink.first]. A name that is not
 * registered is kept out of the link, which falls back to the first or the default, and named in
 * [Parsed.problem], so a mistyped link says so instead of quietly drawing something else.
 */
fun parseHash(raw: String, versions: List<String> = Lab.versions.map { it.name }, states: List<String> = Lab.states.map { it.name }): Parsed {
    val parts = raw.removePrefix("#").split("/").map { decode(it) }.filter { it.isNotEmpty() }
    val problems = mutableListOf<String>()
    fun <T> pick(i: Int, what: String, known: List<T>, id: (T) -> String, default: T): T {
        val name = parts.getOrNull(i) ?: return default
        return known.firstOrNull { id(it) == name } ?: default.also { problems += "No $what \"$name\"" }
    }
    val version = pick(0, "version", versions, { it }, versions.first())
    val state = pick(1, "state", states, { it }, states.first())
    val screen = pick(2, "screen", LabScreen.entries, { it.id }, LabScreen.Cover)
    val wallpaper = pick(3, "wallpaper", Wallpaper.entries, { it.id }, Wallpaper.Dark)
    if (parts.size > 4) problems += "Too many parts: ${parts.drop(4).joinToString("/")}"
    return Parsed(LabLink(version, state, screen, wallpaper), problems.takeIf { it.isNotEmpty() }?.joinToString("; "))
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
