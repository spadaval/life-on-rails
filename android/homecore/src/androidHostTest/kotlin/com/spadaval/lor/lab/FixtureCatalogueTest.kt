package com.spadaval.lor.lab

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/** STATES.md lists the registry's states, in its order, each with the line its fixture's `about` gives. On the JVM, which can read the file. */
class FixtureCatalogueTest {
    @Test
    fun statesMdListsEveryState() {
        // Gradle runs host tests in the module's directory.
        val md = File("STATES.md").readText()
        val section = md.substringAfter("\n## States\n").substringBefore("\n## ")
        val listed = Regex("^- `([^`]+)`: (.+)$", RegexOption.MULTILINE).findAll(section).map { it.groupValues[1] to it.groupValues[2] }.toList()
        assertEquals(Fixtures.all.map { it.name to "${it.about}." }, listed)
    }
}
