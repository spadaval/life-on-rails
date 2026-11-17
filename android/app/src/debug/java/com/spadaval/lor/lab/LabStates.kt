package com.spadaval.lor.lab

import android.app.Activity
import android.content.Intent

/**
 * The debug build's named states (lor-oech): `am start -n com.spadaval.lor/.home.HomeActivity
 * --es lab_state <name>` draws the lab's fixture of that name, or a one-off's JSON, with the
 * lab's `current` version (`--es lab_version <name>` for another). Release builds have a
 * [LabStates] that opens nothing, and no [LabStateActivity].
 */
object LabStates {
    /** The debug build has the entry. */
    const val ENABLED = true

    const val STATE = "lab_state"
    const val VERSION = "lab_version"

    /** The process [LabStateActivity] runs in, where `LorApp` starts no sync and no T3 connection. */
    const val PROCESS = ":lab"

    /** Whether [intent] is a `lab_state` launch, which the starting activity only hands over. */
    fun handsOff(intent: Intent?): Boolean = intent?.hasExtra(STATE) == true

    /**
     * Hands the state an intent names to [LabStateActivity], replacing any fixture already shown;
     * false when it names none. HomeActivity asks before it reads anything.
     */
    fun open(from: Activity?, intent: Intent?): Boolean {
        val state = intent?.getStringExtra(STATE) ?: return false
        from ?: return false
        from.startActivity(
            Intent(from, LabStateActivity::class.java)
                .putExtra(STATE, state)
                .putExtra(VERSION, intent.getStringExtra(VERSION))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        )
        return true
    }

    /** The version and fixture to draw, and what was wrong with the names if anything, as the lab's links say it. */
    fun resolve(state: String, version: String?): Resolved {
        val v = version?.let(Lab::version)
        val (fixture, problem) = Lab.fixture(state)
        val unknown = if (!Lab.isOneOff(state) && Lab.state(state) == null) "No state \"$state\"" else null
        return Resolved(
            v ?: Lab.versions.first(),
            fixture,
            listOfNotNull(version?.takeIf { v == null }?.let { "No version \"$it\"" }, unknown, problem).joinToString("; ").ifEmpty { null },
        )
    }

    class Resolved(val version: Version, val fixture: Fixture, val problem: String?)
}
