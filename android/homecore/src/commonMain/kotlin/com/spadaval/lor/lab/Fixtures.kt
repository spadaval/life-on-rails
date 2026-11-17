package com.spadaval.lor.lab

/**
 * A named state: the data a screen draws from, with nothing read from LoR, T3 or the phone.
 * lor-oech gives a fixture its data (the layout, the apps, `/device/home`'s payload, T3's threads,
 * where the screen is) and fills the list; for now a fixture is its name and what it stands for.
 */
class Fixture(val name: String, val about: String)

object Fixtures {
    val homeFull = Fixture("home-full", "A full home: every page, the dock and the panels filled")
    val firstRun = Fixture("first-run", "The first start: no layout yet, nothing paired")

    /** The State dropdown, in order; the first is what a link without a state opens. */
    val all: List<Fixture> = listOf(homeFull, firstRun)
}
