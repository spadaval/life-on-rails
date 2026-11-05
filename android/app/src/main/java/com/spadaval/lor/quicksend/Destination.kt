package com.spadaval.lor.quicksend

import java.net.URLDecoder

/**
 * Where a quick-send prompt goes (T3 Launcher, Increment 4): the first of the project named in the
 * prompt, the project on screen, or the `assistant` project (the Inbox). A project picked by hand on
 * the sheet wins over all three. Pure.
 */
object Destination {
    const val INBOX = "assistant"

    enum class Why(val label: String) { PICKED("picked"), NAMED("named"), ON_SCREEN("on screen"), INBOX("inbox") }

    /** What the assistant was handed about the screen; the tile and home have none. */
    data class Screen(
        /** The page a browser shows (`AssistContent.webUri`), when there is one. */
        val uris: List<String> = emptyList(),
    )

    /** [projectId] is null only when the server has no `assistant` project and nothing else matched. */
    data class Choice(val projectId: String?, val why: Why, val text: String)

    /**
     * [projects] maps project id to title; [threads] maps thread id to its project id.
     * [picked] is the project chosen on the sheet, if any.
     */
    fun choose(
        prompt: String,
        projects: Map<String, String>,
        threads: Map<String, String>,
        screen: Screen?,
        picked: String? = null,
    ): Choice {
        val named = named(prompt, projects)
        val text = named?.second ?: prompt.trim()
        if (picked != null && picked in projects) return Choice(picked, Why.PICKED, text)
        if (named != null) return Choice(named.first, Why.NAMED, text)
        screen?.let { onScreen(it, projects, threads) }?.let { return Choice(it, Why.ON_SCREEN, text) }
        return Choice(inbox(projects), Why.INBOX, text)
    }

    fun inbox(projects: Map<String, String>): String? =
        projects.entries.firstOrNull { it.value.equals(INBOX, ignoreCase = true) }?.key

    /**
     * A prompt that starts with `@` and a project's title (ignoring case), followed by a space,
     * punctuation or the end, names that project; when titles overlap the longest wins. Returns the
     * project id and the prompt without the mention (and the punctuation and space after it).
     */
    fun named(prompt: String, projects: Map<String, String>): Pair<String, String>? {
        val s = prompt.trimStart()
        if (!s.startsWith("@")) return null
        val rest = s.substring(1)
        val hit = projects.entries
            .filter { (_, title) -> title.isNotBlank() && rest.regionMatches(0, title, 0, title.length, ignoreCase = true) && ends(rest, title.length) }
            .maxByOrNull { it.value.length } ?: return null
        val after = rest.substring(hit.value.length).trimStart { it.isWhitespace() || it in ":,;." }
        return hit.key to after.trim()
    }

    private fun ends(s: String, at: Int): Boolean = at == s.length || s[at].isWhitespace() || s[at] in ":,;.!?"

    /**
     * The project on screen: a known thread id as a path segment of one of [Screen.uris], in order,
     * gives that thread's project. A browser at a T3 web thread (`/<environment>/<thread>`) names one;
     * anything else says nothing.
     */
    fun onScreen(screen: Screen, projects: Map<String, String>, threads: Map<String, String>): String? {
        for (uri in screen.uris) {
            for (segment in segments(uri)) {
                threads[segment]?.takeIf { it in projects }?.let { return it }
            }
        }
        return null
    }

    /** A URI's host and path segments, percent-decoded; the scheme, query and fragment dropped. */
    fun segments(uri: String): List<String> {
        val noTail = uri.substringBefore('#').substringBefore('?')
        val body = if ("://" in noTail) noTail.substringAfter("://") else noTail.substringAfter(':', noTail)
        return body.split('/').filter { it.isNotEmpty() }.map {
            runCatching { URLDecoder.decode(it.replace("+", "%2B"), "UTF-8") }.getOrDefault(it)
        }
    }
}
