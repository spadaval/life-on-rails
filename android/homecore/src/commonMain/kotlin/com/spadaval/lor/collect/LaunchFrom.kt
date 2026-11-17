package com.spadaval.lor.collect

/** Where on the launcher an app was opened from: the `from` of a `kind: "launch"` observation. */
enum class LaunchFrom(val wire: String) {
    HOME("home"),
    FOLDER("folder"),
    /** The drawer with nothing typed. */
    DRAWER("drawer"),
    /** The drawer with something typed, including the keyboard's Go opening the first result. */
    SEARCH("search"),
    /** A card's own action on the agent page: Open on a failed or LoR card, a tap on a running turn. */
    AGENT_CARD("agent_card"),
    /** A card's icon on the agent page ("Open in T3", "Open in LoR"). */
    AGENT_ICON("agent_icon"),
}
