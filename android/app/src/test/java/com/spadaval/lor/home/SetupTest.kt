package com.spadaval.lor.home

import com.spadaval.lor.t3.T3Connection.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Home's set-up card (launcher polish 3.5): which steps are left, and which one leads. */
class SetupTest {
    @Test
    fun pairedMeansASessionTheServerHasNotRefused() {
        assertFalse(Setup.paired(Status.NotPaired))
        assertFalse(Setup.paired(Status.Rejected("revoked")))
        assertTrue(Setup.paired(Status.Stopped))
        assertTrue(Setup.paired(Status.Connecting))
        assertTrue(Setup.paired(Status.Live))
        assertTrue(Setup.paired(Status.Retrying("dropped", 1000)))
    }

    @Test
    fun theStepsLeftAreInOrderAndNoneMeansDone() {
        assertEquals(listOf(SetupStep.HOME, SetupStep.T3, SetupStep.LOR), Setup.left(home = false, paired = false, lor = LorReach.Checking))
        assertEquals(listOf(SetupStep.T3), Setup.left(home = true, paired = false, lor = LorReach.Reached))
        assertEquals(listOf(SetupStep.LOR), Setup.left(home = true, paired = true, lor = LorReach.Failed("LoR didn't answer. Is Tailscale on?")))
        assertEquals(listOf(SetupStep.HOME), Setup.left(home = false, paired = true, lor = LorReach.Reached))
        assertEquals(emptyList<SetupStep>(), Setup.left(home = true, paired = true, lor = LorReach.Reached))
    }
}
