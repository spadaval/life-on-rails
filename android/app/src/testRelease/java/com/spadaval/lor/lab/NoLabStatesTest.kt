package com.spadaval.lor.lab

import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

/** The release build has no fixture entry (lor-oech): its LabStates opens nothing, and there is no activity to open. */
class NoLabStatesTest {
    @Test
    fun releaseOpensNoState() {
        // An intent can't be built on the JVM; a null one is the same call with nothing to read.
        assertFalse(LabStates.open(from = null, intent = null))
    }

    @Test
    fun releaseHasNoFixtureActivity() {
        assertThrows(ClassNotFoundException::class.java) { Class.forName("com.spadaval.lor.lab.LabStateActivity") }
    }

    @Test
    fun releaseHasNoResolve() {
        assertFalse(LabStates::class.java.methods.any { it.name == "resolve" })
    }
}
