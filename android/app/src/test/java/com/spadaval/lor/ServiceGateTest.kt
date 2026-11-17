package com.spadaval.lor

import org.junit.Assert.assertEquals
import org.junit.Test

/** The debug build's cold `lab_state` launch starts no sync and no T3 connection (lor-oech); everything else starts them once. */
class ServiceGateTest {
    private var starts = 0
    private val gate = ServiceGate { starts++ }

    @Test
    fun aRealActivityStartsThemAtOnce() {
        gate.activity(handsOff = false)
        gate.timeout()
        gate.activity(handsOff = false)
        assertEquals(1, starts)
    }

    @Test
    fun aFixtureHandOffHoldsThemPastTheTimeout() {
        gate.activity(handsOff = true)
        gate.timeout()
        assertEquals(0, starts)
    }

    @Test
    fun aRealActivityAfterAHandOffStartsThem() {
        gate.activity(handsOff = true)
        gate.timeout()
        gate.activity(handsOff = false)
        assertEquals(1, starts)
    }

    @Test
    fun noActivityStartsThemAtTheTimeout() {
        gate.timeout()
        gate.activity(handsOff = true)
        assertEquals(1, starts)
    }
}
