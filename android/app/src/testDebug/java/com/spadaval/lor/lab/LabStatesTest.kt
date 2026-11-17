package com.spadaval.lor.lab

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The debug build's fixture entry reads names as the lab's links do. */
class LabStatesTest {
    @Test
    fun aNameOpensItsFixtureInCurrent() {
        val r = LabStates.resolve("overfull", null)
        assertEquals("current", r.version.name)
        assertEquals("overfull", r.fixture.name)
        assertNull(r.problem)
    }

    @Test
    fun anUnknownNameIsNamedAndDrawsTheDefault() {
        val r = LabStates.resolve("nope", "concept-x")
        assertEquals("demo", r.fixture.name)
        assertEquals("No version \"concept-x\"; No state \"nope\"", r.problem)
    }

    @Test
    fun aOneOffIsRead() {
        val r = LabStates.resolve("""{"base":"demo","t3":{"status":"NOT_PAIRED"}}""", null)
        assertEquals("one-off", r.fixture.name)
        assertEquals(T3Status.NOT_PAIRED, r.fixture.t3.status)
        assertNull(r.problem)
    }

    @Test
    fun theActivityIsInTheDebugBuild() {
        Class.forName("com.spadaval.lor.lab.LabStateActivity")
    }
}
