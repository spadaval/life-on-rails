package com.spadaval.lor.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle

/*
 * A broken layout file (launcher polish 3.1): home keeps drawing the last good layout, and a
 * one-line banner says so. A tap shows the parser's error, which names the place, and offers to
 * put the file right: the last good layout, or the default when there is none.
 */


/** The banner's line for [state]. */
internal fun brokenLine(state: LayoutState.Broken): String = when {
    state.lastGood == null -> "layout.json has an error. Tap to put it right."
    state.left.isEmpty() -> "layout.json has an error. Showing the last good layout."
    else -> "layout.json has an error. Showing the last good layout without ${state.left.size} old dock ${if (state.left.size == 1) "app" else "apps"}."
}

/**
 * Why a last good file shown without [left] (old dock apps home has no room for, lor-bdus) can't
 * be put back as it is, and what to do.
 */
internal fun leftOutLine(left: List<AppRef>): String =
    "Home has no room for ${left.joinToString { "\"$it\"" }} from the old dock, so the last good layout is shown without " +
        "${if (left.size == 1) "it" else "them"} and nothing is written. Make room on home in layout.json, or take " +
        "${if (left.size == 1) "it" else "them"} out of its \"dock\"."

/** What [BrokenBanner] says for [state]; [appsLoaded] says whether the default layout can be made, for a file with no last good layout. */
internal fun brokenUi(state: LayoutState.Broken, appsLoaded: Boolean) = BrokenUi(
    line = brokenLine(state),
    message = state.message,
    leftOut = state.left.takeIf { it.isNotEmpty() }?.let(::leftOutLine),
    action = when {
        // Shown without apps that didn't fit, it isn't written over anything.
        state.left.isNotEmpty() -> null
        state.lastGood != null -> "Use the last good layout"
        appsLoaded -> "Start from the default layout"
        else -> null
    },
)

/** The banner for [state] (the shared module draws it), whose dialog offers to put the file right. */
@Composable
internal fun BrokenBanner(state: LayoutState.Broken, appsLoaded: Boolean, style: TextStyle, modifier: Modifier = Modifier, restore: () -> Unit) =
    BrokenBanner(brokenUi(state, appsLoaded), style, modifier, restore)
